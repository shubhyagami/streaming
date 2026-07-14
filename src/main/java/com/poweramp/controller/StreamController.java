package com.poweramp.controller;

import com.poweramp.service.SpotifyService;
import com.poweramp.service.TempFileManager;
import com.poweramp.service.YouTubeService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class StreamController {

    private final YouTubeService ytService;
    private final SpotifyService spotifyService;
    private final TempFileManager tempFileManager;
    private final Map<String, CompletableFuture<Path>> downloads = new ConcurrentHashMap<>();

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

    @PostMapping("/api/yt/stream")
    public ResponseEntity<Map<String, String>> startStream(
            @RequestParam String videoId,
            @RequestParam(defaultValue = "Unknown") String title) {

        CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> {
            try {
                return ytService.downloadAudio(videoId);
            } catch (Exception e) {
                throw new RuntimeException("Download failed: " + e.getMessage(), e);
            }
        });

        downloads.put(videoId, future);

        try {
            Path path = future.get(180, java.util.concurrent.TimeUnit.SECONDS);
            String token = tempFileManager.register(path, title);
            String contentType = getContentType(path);
            downloads.remove(videoId);

            return ResponseEntity.ok(Map.of(
                "token", token,
                "streamUrl", "/api/yt/stream/" + token,
                "title", title,
                "contentType", contentType
            ));
        } catch (java.util.concurrent.TimeoutException e) {
            downloads.remove(videoId);
            future.cancel(true);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Download timed out. The server may be under heavy load."));
        } catch (Exception e) {
            downloads.remove(videoId);
            String msg = e.getMessage();
            if (msg != null && msg.contains("bot")) {
                msg = "YouTube is blocking this request. Please try again later.";
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg != null ? msg : "Unknown download error"));
        }
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

        CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> {
            try {
                return useRapidApi
                    ? spotifyService.downloadAudio(audioUrl, id)
                    : ytService.downloadAudio(videoId);
            } catch (Exception e) {
                throw new RuntimeException("Download failed: " + e.getMessage(), e);
            }
        });

        String key = useRapidApi ? id : videoId;
        downloads.put(key, future);

        try {
            Path path = future.get(180, java.util.concurrent.TimeUnit.SECONDS);
            String token = tempFileManager.register(path, title);
            String contentType = getContentType(path);
            downloads.remove(key);

            return ResponseEntity.ok(Map.of(
                "token", token,
                "streamUrl", "/api/yt/stream/" + token,
                "title", title,
                "contentType", contentType
            ));
        } catch (java.util.concurrent.TimeoutException e) {
            downloads.remove(key);
            future.cancel(true);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Download timed out. The server may be under heavy load."));
        } catch (Exception e) {
            downloads.remove(key);
            String msg = e.getMessage();
            if (msg != null && msg.contains("bot")) {
                msg = "YouTube is blocking this request. Please try again later.";
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg != null ? msg : "Unknown download error"));
        }
    }

    // ===== Shared streaming (works for both YouTube and Spotify tokens) =====

    @GetMapping("/api/yt/stream/{token}")
    public ResponseEntity<Resource> streamFile(@PathVariable String token) {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = entry.path();
        if (!java.nio.file.Files.exists(path)) {
            tempFileManager.delete(token);
            return ResponseEntity.notFound().build();
        }

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
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String token) {
        TempFileManager.TempEntry entry = tempFileManager.get(token);
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "NOT_FOUND"));
        }
        return ResponseEntity.ok(Map.of("status", "READY", "title", entry.title()));
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
