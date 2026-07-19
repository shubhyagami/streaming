package com.poweramp.controller;

import com.poweramp.service.SpotifyService;
import com.poweramp.service.TempFileManager;
import com.poweramp.service.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class StreamController {

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);

    private final YouTubeService ytService;
    private final SpotifyService spotifyService;
    private final TempFileManager tempFileManager;

    public StreamController(YouTubeService ytService, SpotifyService spotifyService, TempFileManager tempFileManager) {
        this.ytService = ytService;
        this.spotifyService = spotifyService;
        this.tempFileManager = tempFileManager;
    }

    // ===== YouTube =====

    @GetMapping("/api/yt/search")
    public List<YouTubeService.SearchResult> search(@RequestParam String q) throws Exception {
        return ytService.search(q, 10);
    }

    @GetMapping("/api/yt/details")
    public YouTubeService.VideoDetails getVideoDetails(@RequestParam String videoId) throws Exception {
        return ytService.getVideoDetails(videoId);
    }

    @PostMapping("/api/yt/stream")
    public ResponseEntity<Map<String, String>> startStream(
            @RequestParam String videoId,
            @RequestParam(defaultValue = "Unknown") String title) {

        // 1. Register a download session immediately and return
        String token = tempFileManager.register(title);

        // 2. Download audio in background
        CompletableFuture.supplyAsync(() -> {
            try { return ytService.downloadAudio(videoId); } catch (Exception e) { throw new RuntimeException(e.getMessage(), e); }
        }).whenComplete((path, ex) -> {
            if (ex != null) {
                // yt-dlp failed — don't mark error (mp36 might still work for streaming)
                // Just log it. The entry stays in DOWNLOADING state with directUrl.
                log.warn("yt-dlp download failed for {} token={}: {}", videoId, token, extractRootCause(ex));
            } else {
                tempFileManager.markReady(token, path, null);
            }
        });

        // 3. Return immediately — client polls for status
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "token", token,
            "status", "DOWNLOADING"
        ));
    }

    // ===== Spotify (search only — downloads use YouTube MP3 API) =====

    @GetMapping("/api/spotify/search")
    public List<SpotifyService.SpotifyResult> searchSpotify(@RequestParam String q) throws Exception {
        return spotifyService.search(q);
    }

    @PostMapping("/api/spotify/stream")
    public ResponseEntity<Map<String, String>> startSpotifyStream(
            @RequestParam String videoId,
            @RequestParam String title) {

        // 1. Register a download session
        String token = tempFileManager.register(title);

        // 2. Download via YouTube MP3 API (same as YouTube stream)
        CompletableFuture.supplyAsync(() -> {
            try {
                return ytService.downloadAudio(videoId);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).whenComplete((path, ex) -> {
            if (ex != null) {
                String msg = extractRootCause(ex);
                log.error("Spotify download failed for videoId={}: {}", videoId, msg);
                tempFileManager.markError(token, msg);
            } else {
                tempFileManager.markReady(token, path, null);
            }
        });

        // 3. Return 202 Accepted immediately
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "token", token,
            "status", "DOWNLOADING"
        ));
    }

    // ===== Shared streaming (works for both YouTube and Spotify tokens) =====

    @GetMapping("/api/yt/stream/{token}")
    public ResponseEntity<?> streamFile(@PathVariable String token) {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }

        if (entry.path() == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = entry.path();
        if (!Files.exists(path)) {
            tempFileManager.delete(token);
            return ResponseEntity.notFound().build();
        }

        // Mark as served — starts the auto-deletion timer
        tempFileManager.markServed(token);

        String contentType = getContentType(path);
        Resource resource = new FileSystemResource(path.toFile());

        try {
            long fileSize = Files.size(path);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
        }
    }

    /**
     * Called by the frontend when a song finishes playing.
     * Immediately deletes the temp file to free disk space.
     */
    @PostMapping("/api/yt/finished/{token}")
    public ResponseEntity<Void> songFinished(@PathVariable String token) {
        tempFileManager.finishPlayback(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/yt/stop/{token}")
    public ResponseEntity<Void> stopStream(@PathVariable String token) {
        tempFileManager.delete(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/yt/stream/{token}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String token) {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "NOT_FOUND"));
        }
        
        if (entry.status() == TempFileManager.Status.DOWNLOADING) {
            return ResponseEntity.ok(Map.of(
                "status", "DOWNLOADING",
                "title", entry.title()
            ));
        } else if (entry.status() == TempFileManager.Status.ERROR) {
            return ResponseEntity.ok(Map.of(
                "status", "ERROR", 
                "error", entry.errorMessage() != null ? entry.errorMessage() : "Unknown error",
                "title", entry.title()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "status", "READY", 
                "title", entry.title(),
                "streamUrl", "/api/yt/stream/" + token
            ));
        }
    }

    // ===== Helpers =====

    /**
     * Unwrap ExecutionException → RuntimeException → original IOException
     * to get the actual meaningful error message instead of Java wrapper noise.
     */
    private String extractRootCause(Throwable e) {
        Throwable cause = e;

        // Unwrap ExecutionException (from CompletableFuture.get())
        if (cause instanceof ExecutionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        // Unwrap RuntimeException (from our supplyAsync wrapper)
        if (cause instanceof RuntimeException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        // Unwrap CompletionException (from CompletableFuture)
        if (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Download failed. Please try a different song.";
        }

        // Clean up common Java exception prefixes that leak into error messages
        msg = msg.replaceFirst("^java\\.lang\\.RuntimeException:\\s*", "");
        msg = msg.replaceFirst("^java\\.io\\.IOException:\\s*", "");
        msg = msg.replaceFirst("^Download failed:\\s*", "");

        return msg;
    }

    private String getContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".m4a") || name.endsWith(".mp4")) return "audio/mp4";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".webm")) return "audio/webm";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".ogg") || name.endsWith(".opus")) return "audio/ogg";
        return "application/octet-stream";
    }
}
