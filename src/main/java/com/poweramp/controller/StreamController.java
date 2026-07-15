package com.poweramp.controller;

import com.poweramp.service.SpotifyService;
import com.poweramp.service.TempFileManager;
import com.poweramp.service.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

        // 2. Fetch direct stream URL in background (media downloader API)
        CompletableFuture.supplyAsync(() -> ytService.getFastStreamUrl(videoId)).thenAccept(url -> {
            if (url != null) {
                tempFileManager.setDirectUrl(token, url);
                log.info("Direct stream URL ready for {} token={}", videoId, token);
            }
        });

        // 3. Start file download in background for persistence
        CompletableFuture.supplyAsync(() -> {
            try { return ytService.downloadAudio(videoId); } catch (Exception e) { throw new RuntimeException(e.getMessage(), e); }
        }).whenComplete((path, ex) -> {
            if (ex != null) {
                tempFileManager.markError(token, extractRootCause(ex));
            } else {
                tempFileManager.markReady(token, path);
            }
        });

        // 4. Return immediately — client polls for status
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "token", token,
            "status", "DOWNLOADING"
        ));
    }

    // ===== Spotify =====

    @GetMapping("/api/spotify/search")
    public List<SpotifyService.SpotifyResult> searchSpotify(@RequestParam String q) throws Exception {
        return spotifyService.search(q);
    }

    @PostMapping("/api/spotify/stream")
    public ResponseEntity<Map<String, String>> startSpotifyStream(
            @RequestParam(defaultValue = "") String audioUrl,
            @RequestParam(defaultValue = "") String videoId,
            @RequestParam String title) {

        boolean useRapidApi = audioUrl != null && !audioUrl.isBlank();
        String id = useRapidApi ? java.util.UUID.randomUUID().toString().substring(0, 8) : videoId;

        // 1. Immediately register a download session
        String token = tempFileManager.register(title);

        // 2. Start download in the background
        CompletableFuture.supplyAsync(() -> {
            try {
                return useRapidApi
                    ? spotifyService.downloadAudio(audioUrl, id)
                    : ytService.downloadAudio(videoId);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).whenComplete((path, ex) -> {
            if (ex != null) {
                String msg = extractRootCause(ex);
                log.error("Spotify download failed for id={}: {}", id, msg);
                tempFileManager.markError(token, msg);
            } else {
                tempFileManager.markReady(token, path);
            }
        });

        // 3. Return 202 Accepted immediately
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "token", token,
            "status", "DOWNLOADING"
        ));
    }

    // ===== Direct stream proxy (immediate playback from YouTube CDN) =====

    @GetMapping("/api/yt/direct/{token}")
    public void directStream(@PathVariable String token, HttpServletResponse response) throws Exception {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        if (entry == null || entry.directUrl() == null) {
            response.sendError(404);
            return;
        }

        // Schedule auto-deletion
        tempFileManager.scheduleDeleteAfter(token, 0);

        // Proxy the YouTube CDN stream
        var httpClient = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();
        var proxyReq = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(entry.directUrl()))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build();
        var proxyRes = httpClient.send(proxyReq, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        response.setStatus(proxyRes.statusCode());
        proxyRes.headers().map().forEach((k, v) -> {
            if (!"transfer-encoding".equalsIgnoreCase(k) && !"content-encoding".equalsIgnoreCase(k)) {
                v.forEach(val -> response.setHeader(k, val));
            }
        });
        try (var out = response.getOutputStream(); var in = proxyRes.body()) {
            in.transferTo(out);
        }
    }

    // ===== Shared streaming (works for both YouTube and Spotify tokens) =====

    @GetMapping("/api/yt/stream/{token}")
    public ResponseEntity<Resource> streamFile(@PathVariable String token) {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        if (entry == null || entry.path() == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = entry.path();
        if (!java.nio.file.Files.exists(path)) {
            tempFileManager.delete(token);
            return ResponseEntity.notFound().build();
        }

        // Schedule auto-deletion after song duration (with buffer)
        tempFileManager.scheduleDeleteAfter(token, 0);

        String contentType = getContentType(path);
        Resource resource = new FileSystemResource(path.toFile());

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
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
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("status", "DOWNLOADING");
            body.put("title", entry.title());
            if (entry.directUrl() != null) {
                body.put("directUrl", entry.directUrl());
                body.put("streamUrl", "/api/yt/direct/" + token);
            }
            return ResponseEntity.ok(body);
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
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".webm")) return "audio/webm";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".ogg") || name.endsWith(".opus")) return "audio/ogg";
        return "application/octet-stream";
    }
}
