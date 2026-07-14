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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(15))
        .build();

    private static final String INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    private static final String INNERTUBE_URL = "https://www.youtube.com/youtubei/v1/search?key=" + INNERTUBE_KEY;

    @Value("${poweramp.temp-dir:#{systemProperties['java.io.tmpdir']}/poweramp-stream}")
    private String tempDir;

    @Value("${poweramp.rapidapi.host:youtube-mp36.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    public record SearchResult(String videoId, String title, String channel, long duration, String thumbnail) {}

    public List<SearchResult> search(String query, int limit) throws IOException, InterruptedException {
        String bodyJson = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20240201.08.00\",\"hl\":\"en\",\"gl\":\"US\"}},\"query\":\"" + escapeJson(query) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(INNERTUBE_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Innertube API returned status " + response.statusCode());
        }

        Map<String, Object> root = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        List<SearchResult> results = new ArrayList<>();

        try {
            Map<String, Object> contents = (Map<String, Object>) root.get("contents");
            if (contents == null) return results;
            Map<String, Object> twoCol = (Map<String, Object>) contents.get("twoColumnSearchResultsRenderer");
            if (twoCol == null) return results;
            Map<String, Object> primary = (Map<String, Object>) twoCol.get("primaryContents");
            if (primary == null) return results;
            Map<String, Object> sectionList = (Map<String, Object>) primary.get("sectionListRenderer");
            if (sectionList == null) return results;
            List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionList.get("contents");
            if (sections == null) return results;

            for (Map<String, Object> section : sections) {
                Map<String, Object> itemSection = (Map<String, Object>) section.get("itemSectionRenderer");
                if (itemSection == null) continue;
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemSection.get("contents");
                if (items == null) continue;

                for (Map<String, Object> item : items) {
                    Map<String, Object> video = (Map<String, Object>) item.get("videoRenderer");
                    if (video == null) continue;

                    String videoId = (String) video.get("videoId");
                    if (videoId == null) continue;

                    String title = extractText(video, "title");
                    String channel = extractText(video, "longBylineText");
                    if (channel == null || channel.isEmpty()) {
                        channel = extractText(video, "ownerText");
                    }

                    long duration = 0;
                    try {
                        Map<String, Object> lengthText = (Map<String, Object>) video.get("lengthText");
                        if (lengthText != null) {
                            String durStr = extractSimpleText(lengthText);
                            duration = parseDuration(durStr);
                        }
                    } catch (Exception e) {
                        // ignore duration parse errors
                    }

                    String thumbnail = extractThumbnail(video);

                    results.add(new SearchResult(videoId, title != null ? title : "",
                        channel != null ? channel : "", duration, thumbnail != null ? thumbnail : ""));

                    if (results.size() >= limit) break;
                }
                if (results.size() >= limit) break;
            }
        } catch (Exception e) {
            log.warn("Failed to parse search results", e);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> video, String key) {
        try {
            Map<String, Object> obj = (Map<String, Object>) video.get(key);
            if (obj == null) return null;
            List<Map<String, Object>> runs = (List<Map<String, Object>>) obj.get("runs");
            if (runs == null || runs.isEmpty()) {
                String simpleText = (String) obj.get("simpleText");
                return simpleText != null ? simpleText : null;
            }
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> run : runs) {
                String text = (String) run.get("text");
                if (text != null) sb.append(text);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractSimpleText(Map<String, Object> obj) {
        try {
            String simpleText = (String) obj.get("simpleText");
            if (simpleText != null) return simpleText;
            List<Map<String, Object>> runs = (List<Map<String, Object>>) obj.get("runs");
            if (runs != null && !runs.isEmpty()) {
                return (String) runs.get(0).get("text");
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractThumbnail(Map<String, Object> video) {
        try {
            Map<String, Object> thumbnail = (Map<String, Object>) video.get("thumbnail");
            if (thumbnail == null) return null;
            List<Map<String, Object>> thumbnails = (List<Map<String, Object>>) thumbnail.get("thumbnails");
            if (thumbnails == null || thumbnails.isEmpty()) return null;
            Map<String, Object> last = thumbnails.get(thumbnails.size() - 1);
            return (String) last.get("url");
        } catch (Exception e) {
            return null;
        }
    }

    private long parseDuration(String durStr) {
        if (durStr == null || durStr.isBlank()) return 0;
        try {
            String[] parts = durStr.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60L + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600L + Integer.parseInt(parts[1]) * 60L + Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return 0;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public Path downloadAudio(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        String apiUrl = "https://" + rapidApiHost + "/dl?id=" + videoId;
        log.info("Requesting download from RapidAPI: {}", apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiHost)
            .header("x-rapidapi-key", rapidApiKey)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("RapidAPI returned status " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        String status = (String) json.get("status");
        if (!"ok".equals(status)) {
            String msg = (String) json.get("msg");
            throw new IOException("RapidAPI error: " + (msg != null ? msg : status));
        }

        String downloadUrl = (String) json.get("link");
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("No download link in RapidAPI response");
        }

        String title = (String) json.get("title");
        log.info("Downloading MP3: {} ({} bytes)", title, json.get("filesize"));

        Path outputPath = dir.resolve(videoId + ".mp3");

        HttpRequest downloadRequest = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .timeout(java.time.Duration.ofMinutes(3))
            .GET()
            .build();

        HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest,
            HttpResponse.BodyHandlers.ofInputStream());

        if (downloadResponse.statusCode() != 200) {
            throw new IOException("Download returned status " + downloadResponse.statusCode());
        }

        try (InputStream in = downloadResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Saved: {} ({} bytes)", outputPath.getFileName(), Files.size(outputPath));
        return outputPath;
    }

}
