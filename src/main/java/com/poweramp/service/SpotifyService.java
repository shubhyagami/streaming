package com.poweramp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient fastClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    private final YouTubeService ytService;

    @Value("${poweramp.rapidapi.spotify-host:spotify81.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    public SpotifyService(YouTubeService ytService) {
        this.ytService = ytService;
    }

    public record SpotifyResult(String id, String title, String artist, String size, String videoId) {}

    public List<SpotifyResult> search(String query) throws IOException, InterruptedException {
        // Try Spotify RapidAPI search first (fast timeout)
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
                String title = (String) item.get("title");
                String size = (String) item.get("size");
                String videoId = (String) item.get("videoId");
                results.add(new SpotifyResult(
                    videoId != null ? videoId : String.valueOf(results.size()),
                    title != null ? title : query, "Spotify", size != null ? size : "", videoId != null ? videoId : ""));
            }
        } catch (Exception e) {
            // Try object format
            try {
                Map<String, Object> obj = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                String title = (String) obj.get("title");
                String size = (String) obj.get("size");
                String videoId = (String) obj.get("videoId");
                results.add(new SpotifyResult(
                    videoId != null ? videoId : "0",
                    title != null ? title : query, "Spotify", size != null ? size : "", videoId != null ? videoId : ""));
            } catch (Exception e2) {
                log.warn("Cannot parse Spotify API response: {}", body.length() > 100 ? body.substring(0, 100) : body);
            }
        }
        return results;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "";
        long m = seconds / 60, s = seconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
