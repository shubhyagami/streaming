package com.poweramp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TempFileManager {

    private static final Logger log = LoggerFactory.getLogger(TempFileManager.class);
    private static final long MAX_AGE_MS = 600_000; // 10 minutes

    private final Map<String, TempEntry> files = new ConcurrentHashMap<>();

    public enum Status {
        DOWNLOADING,
        READY,
        ERROR
    }

    public static class TempEntry {
        private Path path; // Can be null while DOWNLOADING
        private final Instant createdAt;
        private final String title;
        private Status status;
        private String errorMessage;

        public TempEntry(String title) {
            this.createdAt = Instant.now();
            this.title = title;
            this.status = Status.DOWNLOADING;
        }

        public Path path() { return path; }
        public Instant createdAt() { return createdAt; }
        public String title() { return title; }
        public Status status() { return status; }
        public String errorMessage() { return errorMessage; }

        public void setPath(Path path) { this.path = path; }
        public void setStatus(Status status) { this.status = status; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    // Initialize a new download session
    public String register(String title) {
        String token = UUID.randomUUID().toString().substring(0, 12);
        files.put(token, new TempEntry(title));
        return token;
    }

    // Mark as ready
    public void markReady(String token, Path path) {
        TempEntry entry = files.get(token);
        if (entry != null) {
            entry.setPath(path);
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

    public void delete(String token) {
        TempEntry entry = files.remove(token);
        if (entry != null && entry.path() != null) {
            try {
                Files.deleteIfExists(entry.path());
                log.debug("Deleted temp file: {}", entry.path());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", entry.path(), e);
            }
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupStale() {
        Instant cutoff = Instant.now().minusMillis(MAX_AGE_MS);
        files.entrySet().removeIf(e -> {
            if (e.getValue().createdAt().isBefore(cutoff)) {
                if (e.getValue().path() != null) {
                    try {
                        Files.deleteIfExists(e.getValue().path());
                        log.info("Cleaned up stale temp file: {}", e.getValue().path());
                    } catch (IOException ex) {
                        log.warn("Failed to delete stale file: {}", e.getValue().path(), ex);
                    }
                }
                return true;
            }
            return false;
        });
    }
}
