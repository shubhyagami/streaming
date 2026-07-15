package com.poweramp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.TimeUnit;

@Service
public class YouTubeService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    private static final String INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    private static final String INNERTUBE_URL = "https://www.youtube.com/youtubei/v1/search?key=" + INNERTUBE_KEY;

    // Cookie to bypass YouTube bot detection on datacenter IPs
    private static final String YT_CONSENT_COOKIE = "CONSENT=YES+cb.20261010-03-p0.en+FX+";
    private static final String YT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    // Multiple Piped API instances for redundancy
    private static final String[] PIPED_INSTANCES = {
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.in.projectsegfau.lt",
        "https://api.piped.yt"
    };

    @Value("${poweramp.temp-dir:#{systemProperties['java.io.tmpdir']}/poweramp-stream}")
    private String tempDir;

    @Value("${poweramp.rapidapi.host:youtube138.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.download-host:youtube-mp36.p.rapidapi.com}")
    private String rapidApiDownloadHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    @Value("${poweramp.ytdlp.path:yt-dlp}")
    private String ytdlpPath;

    @Value("${poweramp.ytdlp.timeout:120}")
    private int ytdlpTimeout;

    public record SearchResult(String videoId, String title, String channel, long duration, String thumbnail) {}

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

        // Method 3: Piped API fallback
        try {
            List<SearchResult> results = searchWithPipedApi(query, limit);
            if (results != null && !results.isEmpty()) return results;
        } catch (Exception e) {
            errors.add("Piped: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            log.warn("Piped search failed: {}", e.getMessage());
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
        String url = "https://www.youtube.com/results?search_query=" + encoded;

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

                                            String thumb = (String) video.get("thumbnail");
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

        // If JSON parsing failed, try regex fallback
        if (results.isEmpty()) {
            java.util.regex.Pattern idPat = java.util.regex.Pattern.compile("\"videoId\":\"([^\"]+)\"");
            java.util.regex.Matcher idMat = idPat.matcher(body);
            List<String> ids = new ArrayList<>();
            while (idMat.find()) {
                String vid = idMat.group(1);
                if (!ids.contains(vid)) ids.add(vid);
                if (ids.size() >= limit) break;
            }
            java.util.regex.Pattern titlePat = java.util.regex.Pattern.compile("\"text\":\"([^\"]+?)\"");
            java.util.regex.Matcher titleMat = titlePat.matcher(body);
            List<String> titles = new ArrayList<>();
            while (titleMat.find()) {
                String t = titleMat.group(1);
                if (!titles.contains(t)) titles.add(t);
                if (titles.size() >= limit) break;
            }
            int maxResults = Math.min(limit, ids.size());
            for (int i = 0; i < maxResults; i++) {
                results.add(new SearchResult(ids.get(i),
                    i < titles.size() ? titles.get(i) : "Unknown", "",
                    0, "https://i.ytimg.com/vi/" + ids.get(i) + "/hqdefault.jpg"));
            }
        }

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
            Map<String, Object> video = (Map<String, Object>) item.get("video");
            if (video == null) continue;

            String videoId = (String) video.get("videoId");
            if (videoId == null) continue;

            String title = (String) video.get("title");
            String channel = "";
            Object authorObj = video.get("author");
            if (authorObj instanceof Map) {
                channel = (String) ((Map<String, Object>) authorObj).get("title");
            }

            long duration = 0;
            Object lenObj = video.get("lengthSeconds");
            if (lenObj instanceof Number) duration = ((Number) lenObj).longValue();

            String thumbnail = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

            results.add(new SearchResult(videoId, title != null ? title : "",
                channel != null ? channel : "", duration, thumbnail));

            if (results.size() >= limit) break;
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchWithPipedApi(String query, int limit) throws IOException, InterruptedException {
        List<SearchResult> results = new ArrayList<>();

        for (String instance : PIPED_INSTANCES) {
            try {
                String apiUrl = instance + "/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&filter=videos";
                log.info("Trying Piped search: {}", apiUrl);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", YT_USER_AGENT)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) continue;

                Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                if (json.containsKey("error")) continue;

                List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
                if (items == null) continue;

                for (Map<String, Object> item : items) {
                    String videoId = (String) item.get("url");
                    if (videoId == null) continue;
                    // Piped returns "/watch?v=VIDEO_ID" format
                    if (videoId.startsWith("/watch?v=")) videoId = videoId.substring(9);

                    String title = (String) item.get("title");
                    String channel = (String) item.get("uploader");
                    Number durObj = (Number) item.get("duration");
                    long duration = durObj != null ? durObj.longValue() : 0;
                    String thumbnail = (String) item.get("thumbnail");

                    results.add(new SearchResult(videoId, title != null ? title : "",
                        channel != null ? channel : "", duration, thumbnail != null ? thumbnail : ""));

                    if (results.size() >= limit) break;
                }

                if (!results.isEmpty()) return results;
            } catch (Exception e) {
                log.warn("Piped instance {} search failed: {}", instance, e.getMessage());
            }
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

    // Common headers for YouTube API requests (bypass bot detection)
    // NOTE: Java HttpClient does NOT auto-decompress gzip/brotli, so Accept-Encoding is omitted
    private HttpRequest.Builder ytApiRequest(String uri) {
        return HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("User-Agent", YT_USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com")
            .header("Cookie", YT_CONSENT_COOKIE)
            .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-platform", "\"Windows\"");
    }

    // Parse query string like "key1=value1&key2=value2" into a map
    private Map<String, String> parseQueryString(String qs) {
        Map<String, String> params = new java.util.HashMap<>();
        if (qs == null || qs.isBlank()) return params;
        String[] pairs = qs.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = java.net.URLDecoder.decode(pair.substring(0, eq), java.nio.charset.StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    // ===================================================================
    // Download Pipeline — tries each method in order until one succeeds:
    //   1. Piped API    (fast, proxied streams, pure HTTP, no dependencies)
    //   2. RapidAPI     (3rd party, bypasses YouTube bot detection entirely)
    //   3. YouTube      (scrape watch page HTML, extract direct audio URLs)
    //   4. yt-dlp       (subprocess, good fallback when others fail)
    //   5. Innertube    (last resort)
    // ===================================================================

    public Path downloadAudio(String videoId) throws IOException, InterruptedException {
        List<String> errors = new ArrayList<>();

        // 1. Piped API — lightweight, no dependencies, proxied through their servers
        try {
            Path result = downloadWithPipedApi(videoId);
            if (result != null) {
                log.info("✓ Downloaded via Piped API: {}", videoId);
                return result;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            errors.add("Piped: " + msg);
            log.warn("Piped API failed for {}: {}", videoId, msg);
        }

        // 2. RapidAPI — third-party service, bypasses YouTube's bot detection entirely
        try {
            Path result = downloadWithRapidApi(videoId);
            log.info("✓ Downloaded via RapidAPI: {}", videoId);
            return result;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            errors.add("RapidAPI: " + msg);
            log.warn("RapidAPI failed for {}: {}", videoId, msg);
        }

        // 3. Direct YouTube page scrape — extract streaming data from watch page HTML
        try {
            Path result = downloadFromYouTubeDirect(videoId);
            if (result != null) {
                log.info("✓ Downloaded via YouTube scrape: {}", videoId);
                return result;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            errors.add("YouTubeScrape: " + msg);
            log.warn("YouTube scrape failed for {}: {}", videoId, msg);
        }

        // 4. yt-dlp — subprocess, bypasses some restrictions with extractor args
        try {
            Path result = downloadWithYtDlp(videoId);
            if (result != null) {
                log.info("✓ Downloaded via yt-dlp: {}", videoId);
                return result;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            errors.add("yt-dlp: " + msg);
            log.warn("yt-dlp failed for {}: {}", videoId, msg);
        }

        // 5. Innertube Player API — last resort
        try {
            Path result = downloadWithPlayerApi(videoId);
            log.info("✓ Downloaded via Innertube: {}", videoId);
            return result;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            errors.add("Innertube: " + msg);
            log.warn("Innertube failed for {}: {}", videoId, msg);
        }

        String allErrors = String.join(" | ", errors);
        log.error("All download methods failed for {}: {}", videoId, allErrors);
        throw new IOException("Could not download audio. Tried 5 methods, all failed. Details: " + allErrors);
    }

    // ===== Method 1: Piped API =====
    // Piped is a privacy-friendly YouTube proxy. Its API returns direct audio stream URLs
    // that are proxied through Piped's servers, bypassing YouTube's IP-based bot detection.

    @SuppressWarnings("unchecked")
    private Path downloadWithPipedApi(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        IOException lastError = null;

        for (String instance : PIPED_INSTANCES) {
            try {
                String apiUrl = instance + "/streams/" + videoId;
                log.info("Trying Piped instance: {}", apiUrl);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.warn("Piped {} returned status {}", instance, response.statusCode());
                    continue;
                }

                Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

                // Check for error in response
                if (json.containsKey("error")) {
                    log.warn("Piped {} returned error: {}", instance, json.get("error"));
                    continue;
                }

                List<Map<String, Object>> audioStreams = (List<Map<String, Object>>) json.get("audioStreams");
                if (audioStreams == null || audioStreams.isEmpty()) {
                    log.warn("Piped {} returned no audio streams", instance);
                    continue;
                }

                // Find the best audio stream (highest bitrate, prefer m4a/mp3 over webm for compatibility)
                Map<String, Object> bestStream = null;
                int bestScore = -1;

                for (Map<String, Object> stream : audioStreams) {
                    String url = (String) stream.get("url");
                    if (url == null || url.isBlank()) continue;

                    String mimeType = (String) stream.get("mimeType");
                    int bitrate = stream.containsKey("bitrate") ? ((Number) stream.get("bitrate")).intValue() : 0;

                    // Score: prefer higher bitrate, slight bonus for mp4/m4a (better browser compatibility)
                    int score = bitrate;
                    if (mimeType != null && (mimeType.contains("mp4") || mimeType.contains("m4a"))) {
                        score += 10000; // prefer mp4a for web playback
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestStream = stream;
                    }
                }

                if (bestStream == null) {
                    log.warn("Piped {} — no stream with a valid URL", instance);
                    continue;
                }

                String streamUrl = (String) bestStream.get("url");
                String mimeType = (String) bestStream.get("mimeType");

                // Determine file extension
                String ext = "m4a";
                if (mimeType != null) {
                    if (mimeType.contains("webm") || mimeType.contains("opus")) ext = "webm";
                    else if (mimeType.contains("mpeg")) ext = "mp3";
                }

                Path outputPath = dir.resolve(videoId + "." + ext);
                log.info("Downloading from Piped ({}, {}kbps): {}", ext,
                    bestStream.getOrDefault("bitrate", "?"), instance);

                // Download the audio stream
                HttpRequest dlRequest = HttpRequest.newBuilder()
                    .uri(URI.create(streamUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .build();

                HttpResponse<InputStream> dlResponse = httpClient.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());

                if (dlResponse.statusCode() != 200 && dlResponse.statusCode() != 206) {
                    log.warn("Piped stream download returned status {} from {}", dlResponse.statusCode(), instance);
                    continue;
                }

                try (InputStream in = dlResponse.body()) {
                    Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }

                long fileSize = Files.size(outputPath);
                if (fileSize > 10000) { // at least 10KB = valid audio
                    log.info("Piped download saved: {} ({} bytes)", outputPath.getFileName(), fileSize);
                    return outputPath;
                } else {
                    log.warn("Piped download too small ({} bytes), likely invalid", fileSize);
                    Files.deleteIfExists(outputPath);
                }

            } catch (Exception e) {
                lastError = new IOException("Piped " + instance + ": " + e.getMessage(), e);
                log.warn("Piped instance {} failed: {}", instance, e.getMessage());
            }
        }

        throw lastError != null ? lastError : new IOException("All Piped instances failed");
    }

    private synchronized String resolveYtDlpPath() {
        String[] paths = {ytdlpPath, "/usr/local/bin/yt-dlp", "/usr/bin/yt-dlp", "/bin/yt-dlp"};
        for (String p : paths) {
            try {
                Process check = new ProcessBuilder(p, "--version").start();
                if (check.waitFor(5, TimeUnit.SECONDS) && check.exitValue() == 0) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(check.getInputStream()))) {
                        String version = r.readLine();
                        log.info("Found yt-dlp at {} (version {})", p, version);
                    }
                    return p;
                }
            } catch (Exception e) {
                // Ignore and try next
            }
        }
        return null;
    }

    private Path downloadWithYtDlp(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        String resolvedPath = resolveYtDlpPath();
        if (resolvedPath == null) {
            throw new IOException("yt-dlp is not installed or not available in standard paths (/usr/local/bin, etc)");
        }
        ytdlpPath = resolvedPath; // Cache for next time

        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        // Try multiple strategies with consent bypass
        String[][] strategies = {
            // Strategy 1: default with consent bypass and cookie
            {
                ytdlpPath, "-x", "--audio-format", "mp3", "--audio-quality", "5",
                "--no-check-certificates", "--no-warnings", "--no-playlist",
                "--no-part", "--no-cache-dir",
                "--socket-timeout", "30",
                "--extractor-args", "youtube:consent=1",
                "--add-header", "Cookie: " + YT_CONSENT_COOKIE,
                "-o", dir.resolve(videoId + ".%(ext)s").toString(),
                videoUrl
            },
            // Strategy 2: force mweb client (mobile web, least restricted)
            {
                ytdlpPath, "-x", "--audio-format", "mp3", "--audio-quality", "5",
                "--no-check-certificates", "--no-warnings", "--no-playlist",
                "--no-part", "--no-cache-dir",
                "--socket-timeout", "30",
                "--extractor-args", "youtube:player_client=mweb;consent=1",
                "--add-header", "Cookie: " + YT_CONSENT_COOKIE,
                "-o", dir.resolve(videoId + ".%(ext)s").toString(),
                videoUrl
            },
            // Strategy 3: force ios client (currently the most reliable for bypassing bot checks)
            {
                ytdlpPath, "-x", "--audio-format", "mp3", "--audio-quality", "5",
                "--no-check-certificates", "--no-warnings", "--no-playlist",
                "--no-part", "--no-cache-dir",
                "--socket-timeout", "30",
                "--extractor-args", "youtube:player_client=ios;consent=1",
                "--add-header", "Cookie: " + YT_CONSENT_COOKIE,
                "-o", dir.resolve(videoId + ".%(ext)s").toString(),
                videoUrl
            },
            // Strategy 4: force tv client
            {
                ytdlpPath, "-x", "--audio-format", "mp3", "--audio-quality", "5",
                "--no-check-certificates", "--no-warnings", "--no-playlist",
                "--no-part", "--no-cache-dir",
                "--socket-timeout", "30",
                "--extractor-args", "youtube:player_client=tv;consent=1",
                "--add-header", "Cookie: " + YT_CONSENT_COOKIE,
                "-o", dir.resolve(videoId + ".%(ext)s").toString(),
                videoUrl
            },
            // Strategy 5: force android_music client (often bypasses bot checks)
            {
                ytdlpPath, "-x", "--audio-format", "mp3", "--audio-quality", "5",
                "--no-check-certificates", "--no-warnings", "--no-playlist",
                "--no-part", "--no-cache-dir",
                "--socket-timeout", "30",
                "--extractor-args", "youtube:player_client=android_music;consent=1",
                "--add-header", "Cookie: " + YT_CONSENT_COOKIE,
                "-o", dir.resolve(videoId + ".%(ext)s").toString(),
                videoUrl
            }
        };

        IOException lastError = null;

        for (int i = 0; i < strategies.length; i++) {
            log.info("yt-dlp strategy {} of {} for video {}", i + 1, strategies.length, videoId);
            try {
                Path result = executeYtDlp(strategies[i], videoId, dir);
                if (result != null) return result;
            } catch (IOException e) {
                lastError = e;
                log.warn("yt-dlp strategy {} failed: {}", i + 1, e.getMessage());
                cleanPartialFiles(dir, videoId);
            }
        }

        throw lastError != null ? lastError : new IOException("All yt-dlp strategies failed");
    }

    private Path executeYtDlp(String[] command, String videoId, Path dir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.environment().put("HOME", "/tmp");

        log.info("Running yt-dlp: {}", String.join(" ", command));
        Process process = pb.start();

        // Read output asynchronously to prevent pipe blocking
        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("yt-dlp: {}", line);
                }
            } catch (IOException e) {
                log.warn("Error reading yt-dlp output: {}", e.getMessage());
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(ytdlpTimeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            outputReader.join(3000);
            throw new IOException("yt-dlp timed out after " + ytdlpTimeout + "s");
        }

        outputReader.join(5000);
        int exitCode = process.exitValue();

        if (exitCode != 0) {
            String errorOutput = output.toString().trim();
            log.warn("yt-dlp exit {}: {}", exitCode, errorOutput);
            throw new IOException("yt-dlp error: " + truncate(errorOutput, 300));
        }

        // Find the output file
        return findOutputFile(dir, videoId);
    }

    private Path findOutputFile(Path dir, String videoId) throws IOException {
        String[] extensions = {".mp3", ".m4a", ".opus", ".webm", ".ogg", ".wav"};
        for (String ext : extensions) {
            Path p = dir.resolve(videoId + ext);
            if (Files.exists(p) && Files.size(p) > 10000) {
                log.info("yt-dlp output: {} ({} bytes)", p.getFileName(), Files.size(p));
                return p;
            }
        }
        throw new IOException("yt-dlp finished but no valid output file found");
    }

    private void cleanPartialFiles(Path dir, String videoId) {
        String[] extensions = {".mp3", ".m4a", ".opus", ".webm", ".ogg", ".wav", ".part", ".ytdl"};
        for (String ext : extensions) {
            try {
                Files.deleteIfExists(dir.resolve(videoId + ext));
            } catch (IOException ignored) {}
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ===== Method 3: RapidAPI =====

    private Path downloadWithRapidApi(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
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

        if (response.statusCode() != 200) {
            throw new IOException("RapidAPI status " + response.statusCode());
        }

        Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        String status = (String) json.get("status");
        if (!"ok".equals(status)) {
            String msg = (String) json.get("msg");
            throw new IOException("RapidAPI error: " + (msg != null ? msg : status));
        }

        String downloadUrl = (String) json.get("link");
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("No download link from RapidAPI");
        }

        Path outputPath = dir.resolve(videoId + ".mp3");
        log.info("Downloading from RapidAPI: {} ({} bytes)", json.get("title"), json.get("filesize"));

        HttpRequest downloadRequest = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(java.time.Duration.ofMinutes(3))
            .GET()
            .build();

        HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest,
            HttpResponse.BodyHandlers.ofInputStream());

        if (downloadResponse.statusCode() != 200) {
            throw new IOException("RapidAPI download status " + downloadResponse.statusCode());
        }

        try (InputStream in = downloadResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long fileSize = Files.size(outputPath);
        if (fileSize < 10000) {
            Files.deleteIfExists(outputPath);
            throw new IOException("RapidAPI download too small (" + fileSize + " bytes)");
        }

        log.info("RapidAPI saved: {} ({} bytes)", outputPath.getFileName(), fileSize);
        return outputPath;
    }

    // ===== Method 3: Direct YouTube page scrape =====
    // Scrapes youtube.com/watch?v=VIDEO_ID HTML, extracts ytInitialPlayerResponse JSON,
    // finds the best audio-only stream URL, and downloads it directly.

    @SuppressWarnings("unchecked")
    private Path downloadFromYouTubeDirect(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        String pageUrl = "https://www.youtube.com/watch?v=" + videoId;

        HttpRequest pageReq = HttpRequest.newBuilder()
            .uri(URI.create(pageUrl))
            .header("User-Agent", YT_USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.5")
            .GET()
            .build();

        HttpResponse<String> pageRes = httpClient.send(pageReq, HttpResponse.BodyHandlers.ofString());
        String body = pageRes.body();

        // Extract ytInitialPlayerResponse JSON via brace counting
        String json = extractJsonFromHtml(body, "ytInitialPlayerResponse");
        if (json == null) throw new IOException("No ytInitialPlayerResponse found");

        json = json.replace("\\x26", "&").replace("\\u0026", "&").replace("\\/", "/");
        Map<String, Object> root = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> streamingData = (Map<String, Object>) root.get("streamingData");
        if (streamingData == null) throw new IOException("No streamingData in player response");

        List<Map<String, Object>> adaptive = (List<Map<String, Object>>) streamingData.get("adaptiveFormats");
        if (adaptive == null) adaptive = new ArrayList<>();
        List<Map<String, Object>> regular = (List<Map<String, Object>>) streamingData.get("formats");
        if (regular == null) regular = new ArrayList<>();

        // Find best audio-only stream with a URL
        String bestUrl = null;
        int bestBitrate = -1;
        for (Map<String, Object> fmt : adaptive) {
            String mime = (String) fmt.get("mimeType");
            if (mime == null || !mime.startsWith("audio/")) continue;
            String url = (String) fmt.get("url");
            if (url == null || url.isBlank()) continue;
            int bitrate = fmt.containsKey("bitrate") ? ((Number) fmt.get("bitrate")).intValue() : 0;
            if (bitrate > bestBitrate) {
                bestBitrate = bitrate;
                bestUrl = url;
            }
        }

        // Fallback to first format with a URL
        if (bestUrl == null) {
            for (Map<String, Object> fmt : adaptive) {
                String url = (String) fmt.get("url");
                if (url != null && !url.isBlank()) { bestUrl = url; break; }
            }
        }
        if (bestUrl == null) {
            for (Map<String, Object> fmt : regular) {
                String url = (String) fmt.get("url");
                if (url != null && !url.isBlank()) { bestUrl = url; break; }
            }
        }

        if (bestUrl == null) throw new IOException("No audio URL found in scraped page");

        // Determine extension from content type
        String mimeType = "";
        for (Map<String, Object> fmt : adaptive) {
            String mime = (String) fmt.get("mimeType");
            String url = (String) fmt.get("url");
            if (url != null && url.equals(bestUrl) && mime != null) { mimeType = mime; break; }
        }
        String ext = "m4a";
        if (mimeType.contains("opus")) ext = "opus";
        else if (mimeType.contains("webm")) ext = "webm";
        else if (mimeType.contains("mp4")) ext = "m4a";

        Path outputPath = dir.resolve(videoId + "." + ext);

        HttpRequest audioReq = HttpRequest.newBuilder()
            .uri(URI.create(bestUrl))
            .header("User-Agent", YT_USER_AGENT)
            .header("Referer", "https://www.youtube.com/")
            .timeout(java.time.Duration.ofMinutes(3))
            .GET()
            .build();

        HttpResponse<InputStream> audioRes = httpClient.send(audioReq, HttpResponse.BodyHandlers.ofInputStream());
        if (audioRes.statusCode() != 200 && audioRes.statusCode() != 206) {
            throw new IOException("YouTube scrape download status " + audioRes.statusCode());
        }

        try (InputStream in = audioRes.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long size = Files.size(outputPath);
        if (size < 10000) {
            Files.deleteIfExists(outputPath);
            throw new IOException("YouTube scrape download too small (" + size + " bytes)");
        }

        log.info("YouTube scrape saved: {} ({} bytes)", outputPath.getFileName(), size);
        return outputPath;
    }

    // ===== Method 5: Innertube Player API (last resort) =====

    @SuppressWarnings("unchecked")
    private Path downloadWithPlayerApi(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        // Order matters: try the least restricted clients first
        // ANDROID_MUSIC and ANDROID often bypass bot detection on datacenter IPs
        String[][] clients = {
            {"ANDROID_MUSIC", "6.42.51", "34"},
            {"ANDROID", "19.43.38", "34"},
            {"WEB_EMBEDDED_PLAYER", "1.0", ""},
            {"IOS", "19.43.38", ""},
            {"WEB", "2.20250212.00.00", ""}
        };

        IOException lastError = new IOException("All Innertube clients exhausted");
        for (String[] client : clients) {
            try {
                Map<String, Object> streamingData = fetchPlayerStreamingData(videoId, client[0], client[1], client[2]);
                if (streamingData == null) continue;
                Path result = downloadFromStreamingData(videoId, streamingData, dir);
                if (result != null) return result;
            } catch (IOException e) {
                lastError = e;
                log.warn("Innertube client {} failed: {}", client[0], e.getMessage());
            }
        }
        throw lastError;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPlayerStreamingData(String videoId, String clientName, String clientVersion, String sdkVersion) throws IOException, InterruptedException {
        StringBuilder body = new StringBuilder();
        body.append("{\"context\":{\"client\":{");
        body.append("\"clientName\":\"").append(clientName).append("\",");
        body.append("\"clientVersion\":\"").append(clientVersion).append("\"");
        if (!sdkVersion.isEmpty()) {
            body.append(",\"androidSdkVersion\":\"").append(sdkVersion).append("\"");
        }
        body.append(",\"hl\":\"en\",\"gl\":\"US\"");
        body.append("}},\"videoId\":\"").append(escapeJson(videoId)).append("\"}");

        // Try with API key first, then without (some clients work better without it)
        IOException lastError = null;
        String[] keys = {INNERTUBE_KEY, ""};
        for (String key : keys) {
            try {
                String url = "https://www.youtube.com/youtubei/v1/player" + (key.isEmpty() ? "" : "?key=" + key);
                HttpRequest request = ytApiRequest(url)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IOException("Innertube status " + response.statusCode() + " for " + clientName + " key=" + (key.isEmpty() ? "none" : "default"));
                }

                Map<String, Object> root = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                Map<String, Object> playability = (Map<String, Object>) root.get("playabilityStatus");
                if (playability != null) {
                    String pStatus = (String) playability.get("status");
                    if (!"OK".equals(pStatus)) {
                        String reason = (String) playability.get("reason");
                        throw new IOException(clientName + ": " + (reason != null ? reason : pStatus));
                    }
                }

                Map<String, Object> streamingData = (Map<String, Object>) root.get("streamingData");
                if (streamingData != null) return streamingData;
                throw new IOException(clientName + ": no streamingData in response");
            } catch (IOException e) {
                lastError = e;
                log.warn("Innertube key={} for {} failed: {}", key.isEmpty() ? "none" : "default", clientName, e.getMessage());
            }
        }
        throw lastError != null ? lastError : new IOException("All Innertube key variants failed for " + clientName);
    }

    @SuppressWarnings("unchecked")
    private Path downloadFromStreamingData(String videoId, Map<String, Object> streamingData, Path dir) throws IOException {
        List<Map<String, Object>> adaptive = (List<Map<String, Object>>) streamingData.get("adaptiveFormats");
        if (adaptive == null) adaptive = new ArrayList<>();
        List<Map<String, Object>> regular = (List<Map<String, Object>>) streamingData.get("formats");
        if (regular == null) regular = new ArrayList<>();

        // Helper to extract URL from format, handling signatureCipher/cipher
        java.util.function.Function<Map<String, Object>, String> extractUrl = (fmt) -> {
            String url = (String) fmt.get("url");
            if (url != null && !url.isBlank()) return url;
            String cipher = (String) fmt.get("signatureCipher");
            if (cipher == null) cipher = (String) fmt.get("cipher");
            if (cipher != null && !cipher.isBlank()) {
                Map<String, String> params = parseQueryString(cipher);
                url = params.get("url");
                String s = params.get("s");
                String sp = params.get("sp");
                if (url != null && s != null && sp != null) {
                    url = url + "&" + sp + "=" + s;
                }
            }
            return url;
        };

        Map<String, Object> bestFormat = null;

        // First pass: audio-only with direct or signed URLs
        for (Map<String, Object> fmt : adaptive) {
            String mime = (String) fmt.get("mimeType");
            String url = extractUrl.apply(fmt);
            if (mime != null && mime.startsWith("audio") && url != null && !url.isBlank()) {
                if (bestFormat == null) bestFormat = fmt;
                else {
                    int bestBitrate = bestFormat.containsKey("bitrate") ? ((Number) bestFormat.get("bitrate")).intValue() : 0;
                    int fmtBitrate = fmt.containsKey("bitrate") ? ((Number) fmt.get("bitrate")).intValue() : 0;
                    if (fmtBitrate > bestBitrate) bestFormat = fmt;
                }
            }
        }

        // Second pass: regular formats (also try signatureCipher)
        if (bestFormat == null) {
            for (Map<String, Object> fmt : regular) {
                String url = extractUrl.apply(fmt);
                if (url != null && !url.isBlank()) {
                    bestFormat = fmt;
                    break;
                }
            }
        }

        if (bestFormat == null) {
            throw new IOException("No usable audio format in streaming data");
        }

        String audioUrl = extractUrl.apply(bestFormat);
        if (audioUrl == null || audioUrl.isBlank()) {
            throw new IOException("Audio URL is empty");
        }

        String mimeType = (String) bestFormat.get("mimeType");
        String ext = "m4a";
        if (mimeType != null) {
            if (mimeType.contains("opus")) ext = "opus";
            else if (mimeType.contains("webm")) ext = "webm";
            else if (mimeType.contains("mp4")) ext = "m4a";
        }

        Path outputPath = dir.resolve(videoId + "." + ext);

        HttpRequest dlRequest = HttpRequest.newBuilder()
            .uri(URI.create(audioUrl))
            .timeout(java.time.Duration.ofMinutes(3))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build();

        HttpResponse<InputStream> dlResponse;
        try {
            dlResponse = httpClient.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }

        if (dlResponse.statusCode() != 200 && dlResponse.statusCode() != 206) {
            throw new IOException("Innertube audio download status " + dlResponse.statusCode());
        }

        try (InputStream in = dlResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Innertube saved: {} ({} bytes)", outputPath.getFileName(), Files.size(outputPath));
        return outputPath;
    }
}
