package com.poweramp.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TempFileManager {

    private static final Logger log = LoggerFactory.getLogger(TempFileManager.class);
    private static final long MAX_AGE_MS = 600_000; // 10 minutes

    @Value("${poweramp.songs-dir:#{systemProperties['java.io.tmpdir']}/poweramp-songs}")
    private String songsDir;

    private final Map<String, TempEntry> files = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "song-auto-delete");
        t.setDaemon(true);
        return t;
    });

    public enum Status {
        DOWNLOADING,
        READY,
        ERROR
    }

    public static class TempEntry {
        private Path path; // Can be null if using directUrl
        private String directUrl; // URL for frontend direct streaming
        private final Instant createdAt;
        private final String title;
        private Status status;
        private String errorMessage;
        private volatile boolean served; // true once the file has been served to the client

        public TempEntry(String title) {
            this.createdAt = Instant.now();
            this.title = title;
            this.status = Status.DOWNLOADING;
            this.served = false;
        }

        public Path path() { return path; }
        public String directUrl() { return directUrl; }
        public Instant createdAt() { return createdAt; }
        public String title() { return title; }
        public Status status() { return status; }
        public String errorMessage() { return errorMessage; }
        public boolean served() { return served; }

        public void setPath(Path path) { this.path = path; }
        public void setDirectUrl(String directUrl) { this.directUrl = directUrl; }
        public void setStatus(Status status) { this.status = status; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setServed(boolean served) { this.served = served; }
    }

    /**
     * On startup, ensure the songs directory exists and clean up any leftover files
     * from previous runs (important on Render where /tmp persists across restarts
     * within the same deploy).
     */
    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(songsDir);
            Files.createDirectories(dir);
            log.info("Songs temp directory ready: {}", dir.toAbsolutePath());

            // Clean up any leftover files from previous runs
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                            log.info("Cleaned up leftover file: {}", file.getFileName());
                        } catch (IOException e) {
                            log.warn("Could not clean up: {}", file, e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize songs directory: {}", e.getMessage());
        }
    }

    // Initialize a new download session
    public String register(String title) {
        String token = UUID.randomUUID().toString().substring(0, 12);
        files.put(token, new TempEntry(title));
        return token;
    }

    // Mark as ready
    public void markReady(String token, Path path, String directUrl) {
        TempEntry entry = files.get(token);
        if (entry != null) {
            entry.setPath(path);
            entry.setDirectUrl(directUrl);
            entry.setStatus(Status.READY);
        }
    }

    // Mark as error
    public void markError(String token, String errorMessage) {
        TempEntry entry = files.get(token);
        if (entry != null) {
            entry.setErrorMessage(errorMessage);
            entry.setStatus(Status.ERROR);
        }
    }

    public TempEntry get(String token) {
        return files.get(token);
    }

    /**
     * Mark that the file has been served to the client (audio started playing).
     * This starts the auto-deletion timer.
     */
    public void markServed(String token) {
        TempEntry entry = files.get(token);
        if (entry != null && !entry.served()) {
            entry.setServed(true);
            // Schedule auto-deletion after a generous buffer (5 minutes after first serve)
            // This covers seeking, replay, and slow connections
            scheduleDeleteAfter(token, 300);
        }
    }

    /**
     * Called by the frontend when a song finishes playing.
     * Immediately deletes the temp file to free disk space on Render.
     */
    public void finishPlayback(String token) {
        log.info("Song playback finished, deleting: {}", token);
        delete(token);
    }

    // Schedule auto-deletion after delay (seconds)
    public void scheduleDeleteAfter(String token, long delaySeconds) {
        scheduler.schedule(() -> {
            TempEntry entry = files.get(token);
            if (entry != null) {
                log.info("Auto-deleting song after timeout: {} ({})", entry.title(), token);
                delete(token);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public void delete(String token) {
        TempEntry entry = files.remove(token);
        if (entry != null && entry.path() != null) {
            try {
                boolean deleted = Files.deleteIfExists(entry.path());
                if (deleted) {
                    log.info("Deleted temp file: {}", entry.path().getFileName());
                }
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", entry.path(), e);
            }
        }
    }

    /**
     * Periodic cleanup: removes stale entries and their files.
     * Runs every 60 seconds. Catches entries that weren't cleaned up properly.
     */
    @Scheduled(fixedRate = 60_000)
    public void cleanupStale() {
        Instant cutoff = Instant.now().minusMillis(MAX_AGE_MS);
        files.entrySet().removeIf(e -> {
            TempEntry entry = e.getValue();
            if (entry.createdAt().isBefore(cutoff)) {
                if (entry.path() != null) {
                    try {
                        boolean deleted = Files.deleteIfExists(entry.path());
                        if (deleted) {
                            log.info("Cleaned up stale temp file: {}", entry.path().getFileName());
                        }
                    } catch (IOException ex) {
                        log.warn("Failed to delete stale file: {}", entry.path(), ex);
                    }
                }
                return true;
            }
            return false;
        });

        // Also clean up any orphaned files in the songs directory
        // (files that exist on disk but have no matching token)
        try {
            Path dir = Paths.get(songsDir);
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.forEach(file -> {
                        String name = file.getFileName().toString();
                        boolean hasToken = files.values().stream()
                            .anyMatch(entry -> entry.path() != null &&
                                entry.path().getFileName().toString().equals(name));
                        if (!hasToken) {
                            try {
                                Files.deleteIfExists(file);
                                log.info("Cleaned up orphaned file: {}", name);
                            } catch (IOException ex) {
                                log.warn("Failed to delete orphaned file: {}", name, ex);
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan songs directory for orphans", e);
        }
    }
}
