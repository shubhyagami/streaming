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
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com")
            .header("Cookie", "CONSENT=YES+cb; SOCS=CAISHAhCEhcKAzEwOBIENTcyNBghMACoBQ")
            .header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-platform", "\"Windows\"")
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
        // Try RapidAPI first
        try {
            return downloadWithRapidApi(videoId);
        } catch (Exception e) {
            log.warn("RapidAPI download failed, falling back to Innertube player API: {}", e.getMessage());
            return downloadWithPlayerApi(videoId);
        }
    }

    private Path downloadWithRapidApi(String videoId) throws IOException, InterruptedException {
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

    @SuppressWarnings("unchecked")
    private Path downloadWithPlayerApi(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        // Try multiple Innertube clients in order of permissiveness
        String[][] clients = {
            {"WEB_EMBEDDED_PLAYER", "1.0", ""},
            {"ANDROID_MUSIC", "6.27.51", "30"},
            {"ANDROID", "19.09.37", "30"},
            {"WEB", "2.20240201.08.00", ""}
        };

        IOException lastError = new IOException("All clients exhausted");
        for (String[] client : clients) {
            try {
                Map<String, Object> streamingData = fetchPlayerStreamingData(videoId, client[0], client[1], client[2]);
                if (streamingData == null) continue;
                Path result = downloadFromStreamingData(videoId, streamingData, dir);
                if (result != null) return result;
            } catch (IOException e) {
                lastError = e;
                log.warn("Player API client {} failed: {}", client[0], e.getMessage());
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
            body.append(",\"androidSdkVersion\":").append(sdkVersion);
        }
        body.append(",\"hl\":\"en\",\"gl\":\"US\"");
        body.append("}},\"videoId\":\"").append(escapeJson(videoId)).append("\"}");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://www.youtube.com/youtubei/v1/player?key=" + INNERTUBE_KEY))
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com")
            .header("Cookie", "CONSENT=YES+cb; SOCS=CAISHAhCEhcKAzEwOBIENTcyNBghMACoBQ; __Secure-ENID=17.SE=; YSC=DwKYllHNwC4; VISITOR_INFO1_LIVE=ST1TiqP3p9k")
            .header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "same-origin")
            .header("Sec-Fetch-Site", "same-origin")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Player API returned status " + response.statusCode() + " for client " + clientName);
        }

        Map<String, Object> root = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> playability = (Map<String, Object>) root.get("playabilityStatus");
        if (playability != null) {
            String pStatus = (String) playability.get("status");
            if (!"OK".equals(pStatus)) {
                String reason = (String) playability.get("reason");
                throw new IOException("Video not playable (" + clientName + "): " + (reason != null ? reason : pStatus));
            }
        }

        return (Map<String, Object>) root.get("streamingData");
    }

    @SuppressWarnings("unchecked")
    private Path downloadFromStreamingData(String videoId, Map<String, Object> streamingData, Path dir) throws IOException {
        List<Map<String, Object>> adaptive = (List<Map<String, Object>>) streamingData.get("adaptiveFormats");
        if (adaptive == null) adaptive = new ArrayList<>();
        List<Map<String, Object>> regular = (List<Map<String, Object>>) streamingData.get("formats");
        if (regular == null) regular = new ArrayList<>();

        Map<String, Object> bestFormat = null;

        // First pass: audio-only with direct URLs
        for (Map<String, Object> fmt : adaptive) {
            String mime = (String) fmt.get("mimeType");
            String url = (String) fmt.get("url");
            if (mime != null && mime.startsWith("audio") && url != null && !url.isBlank()) {
                if (bestFormat == null) bestFormat = fmt;
                else {
                    String bestMime = (String) bestFormat.get("mimeType");
                    int bestBitrate = bestFormat.containsKey("bitrate") ? ((Number) bestFormat.get("bitrate")).intValue() : 0;
                    int fmtBitrate = fmt.containsKey("bitrate") ? ((Number) fmt.get("bitrate")).intValue() : 0;
                    boolean bestIsOpus = bestMime != null && bestMime.contains("opus");
                    boolean fmtIsOpus = mime.contains("opus");
                    if (fmtIsOpus && !bestIsOpus) {
                        bestFormat = fmt;
                    } else if (fmtIsOpus == bestIsOpus && fmtBitrate > bestBitrate) {
                        bestFormat = fmt;
                    }
                }
            }
        }

        // Second pass: regular formats with direct URLs
        if (bestFormat == null) {
            for (Map<String, Object> fmt : regular) {
                String mime = (String) fmt.get("mimeType");
                String url = (String) fmt.get("url");
                if (mime != null && mime.startsWith("audio") && url != null && !url.isBlank()) {
                    bestFormat = fmt;
                    break;
                }
            }
        }

        // Third pass: signature-ciphered formats
        if (bestFormat == null) {
            for (Map<String, Object> fmt : adaptive) {
                String cipher = (String) fmt.get("signatureCipher");
                if (cipher != null) {
                    String decodedUrl = decipherUrl(cipher);
                    if (decodedUrl != null) {
                        String mime = (String) fmt.get("mimeType");
                        if (mime != null && mime.startsWith("audio")) {
                            bestFormat = fmt;
                            bestFormat.put("url", decodedUrl);
                            break;
                        }
                    }
                }
            }
        }

        if (bestFormat == null) {
            throw new IOException("No usable audio format found");
        }

        String audioUrl = (String) bestFormat.get("url");
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
        log.info("Downloading audio ({}) from format: {}", ext, bestFormat.getOrDefault("itag", "?"));

        HttpRequest dlRequest = HttpRequest.newBuilder()
            .uri(URI.create(audioUrl))
            .timeout(java.time.Duration.ofMinutes(3))
            .GET()
            .header("User-Agent", "Mozilla/5.0")
            .build();

        HttpResponse<InputStream> dlResponse;
        try {
            dlResponse = httpClient.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }

        if (dlResponse.statusCode() != 200 && dlResponse.statusCode() != 206) {
            throw new IOException("Audio download returned status " + dlResponse.statusCode());
        }

        try (InputStream in = dlResponse.body()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Saved: {} ({} bytes)", outputPath.getFileName(), Files.size(outputPath));
        return outputPath;
    }

    private String decipherUrl(String cipher) {
        try {
            String[] params = cipher.split("&");
            String urlParam = null;
            String spParam = null;
            String sigParam = null;
            for (String p : params) {
                if (p.startsWith("url=")) urlParam = p.substring(4);
                else if (p.startsWith("sp=")) spParam = p.substring(3);
                else if (p.startsWith("s=")) sigParam = p.substring(2);
            }
            if (urlParam != null) {
                String decodedUrl = java.net.URLDecoder.decode(urlParam, "UTF-8");
                if (sigParam != null) {
                    String sp = java.net.URLDecoder.decode(spParam != null ? spParam : "sig", "UTF-8");
                    decodedUrl += "&" + sp + "=" + java.net.URLDecoder.decode(sigParam, "UTF-8");
                }
                return decodedUrl;
            }
        } catch (Exception e) {
            log.warn("Failed to decipher URL: {}", e.getMessage());
        }
        return null;
    }

}
