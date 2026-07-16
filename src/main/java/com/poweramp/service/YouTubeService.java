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
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    private static final String YT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    @Value("${poweramp.songs-dir:#{systemProperties['java.io.tmpdir']}/poweramp-songs}")
    private String songsDir;

    @Value("${poweramp.rapidapi.host:youtube138.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.download-host:youtube-mp36.p.rapidapi.com}")
    private String rapidApiDownloadHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    public record SearchResult(String videoId, String title, String channel, long duration, String thumbnail) {}

    public record VideoDetails(
        String videoId, String title, String channel, String channelId, String description,
        long views, long likes, long comments, long duration,
        String publishedDate, String publishedDateTime, boolean isLive,
        String thumbnail
    ) {}

    @SuppressWarnings("unchecked")
    public VideoDetails getVideoDetails(String videoId) throws IOException, InterruptedException {
        String apiUrl = "https://" + rapidApiHost + "/video/details/?id=" + videoId + "&hl=en&gl=US";
        log.info("Fetching video details from RapidAPI: {}", apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiHost)
            .header("x-rapidapi-key", rapidApiKey)
            .header("Accept", "application/json")
            .timeout(java.time.Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Video details API returned status " + response.statusCode());
        }

        Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        String title = (String) json.get("title");
        String desc = (String) json.get("description");
        Number len = (Number) json.get("lengthSeconds");
        boolean isLive = Boolean.TRUE.equals(json.get("isLiveNow"));

        String publishedDate = (String) json.get("publishedDate");
        String publishedDateTime = (String) json.get("publishedDateTime");

        String channel = "";
        String channelId = "";
        Object authorObj = json.get("author");
        if (authorObj instanceof Map) {
            Map<String, Object> author = (Map<String, Object>) authorObj;
            channel = (String) author.get("title");
            channelId = (String) author.get("channelId");
        }

        long views = 0, likes = 0, comments = 0;
        Object statsObj = json.get("stats");
        if (statsObj instanceof Map) {
            Map<String, Object> stats = (Map<String, Object>) statsObj;
            if (stats.get("views") instanceof Number) views = ((Number) stats.get("views")).longValue();
            if (stats.get("likes") instanceof Number) likes = ((Number) stats.get("likes")).longValue();
            if (stats.get("comments") instanceof Number) comments = ((Number) stats.get("comments")).longValue();
        }

        String thumbnail = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

        return new VideoDetails(videoId, title != null ? title : "", channel, channelId,
            desc != null ? desc : "", views, likes, comments,
            len != null ? len.longValue() : 0,
            publishedDate, publishedDateTime, isLive, thumbnail);
    }

    // ===== Search =====

    public List<SearchResult> search(String query, int limit) throws IOException, InterruptedException {
        List<String> errors = new ArrayList<>();

        // Method 1: Scrape YouTube search page HTML (most reliable, no API key needed)
        try {
            List<SearchResult> results = searchByScrapingYouTube(query, limit);
            if (results != null && !results.isEmpty()) return results;
        } catch (Exception e) {
            errors.add("Scrape: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            log.warn("YouTube scrape search failed: {}", e.getMessage());
        }

        // Method 2: RapidAPI (youtube138) fallback
        try {
            List<SearchResult> results = searchWithRapidApi(query, limit);
            if (results != null && !results.isEmpty()) return results;
        } catch (Exception e) {
            errors.add("RapidAPI: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            log.warn("RapidAPI search failed: {}", e.getMessage());
        }

        throw new IOException("Search failed. " + String.join(" | ", errors));
    }

    // Extract JSON object from HTML by counting braces (more robust than regex)
    private String extractJsonFromHtml(String html, String marker) {
        int idx = html.indexOf(marker);
        if (idx < 0) return null;
        int start = html.indexOf('{', idx);
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return html.substring(start, i + 1);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchByScrapingYouTube(String query, int limit) throws IOException, InterruptedException {
        String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        String url = "https://www.youtube.com/results?search_query=" + encoded + "&hl=en&gl=US";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", YT_USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.5")
            .GET()
            .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        String body = res.body();

        List<SearchResult> results = new ArrayList<>();

        // Extract ytInitialData JSON using brace counting
        String json = extractJsonFromHtml(body, "ytInitialData");
        if (json != null) {
            json = json.replace("\\x26", "&").replace("\\u0026", "&").replace("\\/", "/");
            try {
                Map<String, Object> root = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> contents = (Map<String, Object>) root.get("contents");
                if (contents != null) {
                    Map<String, Object> twoCol = (Map<String, Object>) contents.get("twoColumnSearchResultsRenderer");
                    if (twoCol != null) {
                        Map<String, Object> primary = (Map<String, Object>) twoCol.get("primaryContents");
                        if (primary != null) {
                            Map<String, Object> sectionList = (Map<String, Object>) primary.get("sectionListRenderer");
                            if (sectionList != null) {
                                List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionList.get("contents");
                                if (sections != null) {
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
                                            if (title == null || title.isEmpty()) title = extractSimpleText((Map<String, Object>) video.get("title"));
                                            String channel = extractText(video, "longBylineText");
                                            if (channel == null || channel.isEmpty()) channel = extractText(video, "ownerText");

                                            long duration = 0;
                                            try {
                                                Map<String, Object> lengthText = (Map<String, Object>) video.get("lengthText");
                                                if (lengthText != null) {
                                                    String durStr = extractSimpleText(lengthText);
                                                    if (durStr != null) duration = parseDuration(durStr);
                                                }
                                            } catch (Exception ignored) {}

                                            // Skip shorts (< 60 seconds)
                                            if (duration > 0 && duration < 60) continue;

                                            String thumb = extractThumbnail(video);
                                            if (thumb == null || thumb.isBlank()) thumb = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

                                            results.add(new SearchResult(videoId, title != null ? title : "",
                                                channel != null ? channel : "", duration, thumb));
                                            if (results.size() >= limit) break;
                                        }
                                        if (results.size() >= limit) break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse scraped YouTube JSON", e);
            }
        }

        // If JSON parsing failed, return empty — the next method in the pipeline will handle it

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchWithRapidApi(String query, int limit) throws IOException, InterruptedException {
        List<SearchResult> results = new ArrayList<>();

        String apiUrl = "https://" + rapidApiHost + "/search/?q="
            + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
            + "&hl=en&gl=US";
        log.info("RapidAPI search: {}", apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiHost)
            .header("x-rapidapi-key", rapidApiKey)
            .header("Accept", "application/json")
            .header("User-Agent", YT_USER_AGENT)
            .timeout(java.time.Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) return results;

        Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> contents = (List<Map<String, Object>>) json.get("contents");
        if (contents == null || contents.isEmpty()) return results;

        for (Map<String, Object> item : contents) {
            // Skip non-video items (shorts, playlists, channels, etc.)
            String type = (String) item.get("type");
            if (!"video".equals(type)) continue;

            Map<String, Object> video = (Map<String, Object>) item.get("video");
            if (video == null) continue;

            String videoId = (String) video.get("videoId");
            if (videoId == null) continue;

            // Skip shorts (typically < 60 seconds)
            Object lenObj = video.get("lengthSeconds");
            long duration = 0;
            if (lenObj instanceof Number) duration = ((Number) lenObj).longValue();
            if (duration > 0 && duration < 60) continue;

            String title = (String) video.get("title");
            String channel = "";
            Object authorObj = video.get("author");
            if (authorObj instanceof Map) {
                channel = (String) ((Map<String, Object>) authorObj).get("title");
            }

            String thumbnail = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

            results.add(new SearchResult(videoId, title != null ? title : "",
                channel != null ? channel : "", duration, thumbnail));

            if (results.size() >= limit) break;
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



    // ===================================================================
    // Download Pipeline — only RapidAPI mp36 (with retry)
    // ===================================================================

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 2000;

    public Path downloadAudio(String videoId) throws IOException, InterruptedException {
        IOException lastError = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Path result = downloadWithRapidApi(videoId);
                log.info("✓ Downloaded via RapidAPI (attempt {}): {}", attempt + 1, videoId);
                return result;
            } catch (IOException e) {
                lastError = e;
                String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
                log.warn("RapidAPI download attempt {} failed for {}: {}", attempt + 1, videoId, msg);
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        String msg = lastError != null && lastError.getMessage() != null
            ? lastError.getMessage() : "unknown error";
        log.error("RapidAPI download failed after {} attempts for {}: {}", MAX_RETRIES + 1, videoId, msg);
        throw new IOException("Download failed after retries. " + msg);
    }

    private Path downloadWithRapidApi(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(songsDir);
        Files.createDirectories(dir);

        String apiUrl = "https://" + rapidApiDownloadHost + "/dl?id=" + videoId;
        log.info("RapidAPI download request: {}", apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("x-rapidapi-host", rapidApiDownloadHost)
            .header("x-rapidapi-key", rapidApiKey)
            .header("Accept", "application/json")
            .timeout(java.time.Duration.ofSeconds(30))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new IOException("API rate limit reached. Please wait a moment and try again.");
        }
        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        String status = (String) json.get("status");
        if (!"ok".equals(status)) {
            String msg = (String) json.get("msg");
            throw new IOException("API error: " + (msg != null ? msg : status));
        }

        String downloadUrl = (String) json.get("link");
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("No download link received from API");
        }

        Path outputPath = dir.resolve(videoId + ".mp3");
        log.info("Downloading MP3: {} ({} bytes)", json.get("title"), json.get("filesize"));

        HttpRequest downloadRequest = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(java.time.Duration.ofMinutes(3))
            .GET()
            .build();

        HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest,
            HttpResponse.BodyHandlers.ofInputStream());

        if (downloadResponse.statusCode() != 200) {
            throw new IOException("MP3 download returned status " + downloadResponse.statusCode());
        }

        try (InputStream in = downloadResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long fileSize = Files.size(outputPath);
        if (fileSize < 10000) {
            Files.deleteIfExists(outputPath);
            throw new IOException("Downloaded file too small (" + fileSize + " bytes) — may be corrupted");
        }

        log.info("Saved: {} ({} bytes)", outputPath.getFileName(), fileSize);
        return outputPath;
    }

}
