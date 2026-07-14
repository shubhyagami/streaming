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
    private static final long MAX_AGE_MS = 600_000;

    private final Map<String, TempEntry> files = new ConcurrentHashMap<>();

    public record TempEntry(Path path, Instant createdAt, String title) {}

    public String register(Path path, String title) {
        String token = UUID.randomUUID().toString().substring(0, 12);
        files.put(token, new TempEntry(path, Instant.now(), title));
        return token;
    }

    public TempEntry get(String token) {
        return files.get(token);
    }

    public void delete(String token) {
        TempEntry entry = files.remove(token);
        if (entry != null) {
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
                try {
                    Files.deleteIfExists(e.getValue().path());
                    log.info("Cleaned up stale temp file: {}", e.getValue().path());
                } catch (IOException ex) {
                    log.warn("Failed to delete stale file: {}", e.getValue().path(), ex);
                }
                return true;
            }
            return false;
        });
    }
}
