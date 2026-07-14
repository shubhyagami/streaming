package com.poweramp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient fastClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    private final YouTubeService ytService;

    @Value("${poweramp.temp-dir:#{systemProperties['java.io.tmpdir']}/poweramp-stream}")
    private String tempDir;

    @Value("${poweramp.rapidapi.spotify-host:spotify81.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    public SpotifyService(YouTubeService ytService) {
        this.ytService = ytService;
    }

    public record SpotifyResult(String id, String title, String artist, String audioUrl, String size, String videoId) {}

    public List<SpotifyResult> search(String query) throws IOException, InterruptedException {
        // Try real Spotify/SoundCloud API first (fast timeout)
        try {
            List<SpotifyResult> results = searchRapidApi(query);
            if (!results.isEmpty()) return results;
        } catch (Exception e) {
            log.warn("Spotify RapidAPI search failed, falling back to YouTube: {}", e.getMessage());
        }

        // Fallback: search YouTube and wrap results as Spotify
        List<YouTubeService.SearchResult> ytResults = ytService.search(query, 10);
        List<SpotifyResult> fallback = new ArrayList<>();
        for (YouTubeService.SearchResult yt : ytResults) {
            fallback.add(new SpotifyResult(
                yt.videoId(),
                yt.title(),
                yt.channel() + " (via YouTube)",
                null,
                formatDuration(yt.duration()),
                yt.videoId()
            ));
        }
        return fallback;
    }

    private List<SpotifyResult> searchRapidApi(String query) throws Exception {
        String apiUrl = "https://" + rapidApiHost + "/download_track_sc?q="
            + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&onlyLinks=true";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiHost)
            .header("x-rapidapi-key", rapidApiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = fastClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) return List.of();

        String body = response.body();
        if (body == null || body.isBlank() || "{}".equals(body.trim())) return List.of();

        List<SpotifyResult> results = new ArrayList<>();

        // Try array format
        try {
            List<Map<String, Object>> items = mapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : items) {
                String url = (String) item.get("url");
                String size = (String) item.get("size");
                if (url == null || url.isBlank()) continue;
                results.add(new SpotifyResult(
                    UUID.randomUUID().toString().substring(0, 8),
                    query, "Spotify", url, size != null ? size : "", ""));
            }
        } catch (Exception e) {
            // Try object format
            try {
                Map<String, Object> obj = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                String url = (String) obj.get("url");
                if (url != null && !url.isBlank()) {
                    String size = (String) obj.get("size");
                    results.add(new SpotifyResult(
                        UUID.randomUUID().toString().substring(0, 8),
                        query, "Spotify", url, size != null ? size : "", ""));
                }
            } catch (Exception e2) {
                log.warn("Cannot parse Spotify API response: {}", body.length() > 100 ? body.substring(0, 100) : body);
            }
        }
        return results;
    }

    public Path downloadAudio(String videoId, String title) throws IOException, InterruptedException {
        // Try RapidAPI download first
        try {
            Path p = downloadViaRapidApi(videoId, title);
            if (p != null) return p;
        } catch (Exception e) {
            log.warn("Spotify RapidAPI download failed, falling back to YouTube: {}", e.getMessage());
        }

        // Fallback to YouTube download
        return ytService.downloadAudio(videoId);
    }

    private Path downloadViaRapidApi(String videoId, String title) throws Exception {
        String apiUrl = "https://" + rapidApiHost + "/download_track?q="
            + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&onlyLinks=true";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiHost)
            .header("x-rapidapi-key", rapidApiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = fastClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;

        String body = response.body();
        if (body == null || body.isBlank() || "{}".equals(body.trim())) return null;

        // Parse the download link from response
        String downloadUrl = null;
        try {
            List<Map<String, Object>> items = mapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
            if (!items.isEmpty()) downloadUrl = (String) items.get(0).get("url");
        } catch (Exception e) {
            try {
                Map<String, Object> obj = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                downloadUrl = (String) obj.get("url");
            } catch (Exception e2) {
                return null;
            }
        }

        if (downloadUrl == null || downloadUrl.isBlank()) return null;

        // Download the audio file
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);
        Path outputPath = dir.resolve(videoId + ".mp3");

        HttpRequest dlRequest = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .timeout(Duration.ofMinutes(3))
            .GET()
            .build();

        HttpResponse<InputStream> dlResponse = fastClient.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (dlResponse.statusCode() != 200) return null;

        try (InputStream in = dlResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Spotify download saved: {} ({} bytes)", outputPath.getFileName(), Files.size(outputPath));
        return outputPath;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "";
        long m = seconds / 60, s = seconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
