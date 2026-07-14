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
        .build();

    private static final String INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    private static final String INNERTUBE_URL = "https://www.youtube.com/youtubei/v1/search?key=" + INNERTUBE_KEY;

    @Value("${poweramp.temp-dir:#{systemProperties['java.io.tmpdir']}/poweramp-stream}")
    private String tempDir;

    @Value("${poweramp.rapidapi.host:youtube-mp36.p.rapidapi.com}")
    private String rapidApiHost;

    @Value("${poweramp.rapidapi.key:e9f2c625ebmsh6cd2de7109f2f5ep1f9991jsn4f3b636412b2}")
    private String rapidApiKey;

    @Value("${poweramp.ytdlp.path:yt-dlp}")
    private String ytdlpPath;

    @Value("${poweramp.ytdlp.timeout:120}")
    private int ytdlpTimeout;

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

    // ===== Download Pipeline =====
    // Order: yt-dlp (most reliable) → RapidAPI → Innertube Player API (last resort)

    public Path downloadAudio(String videoId) throws IOException, InterruptedException {
        // 1. Try yt-dlp first (most reliable for bypassing bot detection)
        try {
            Path result = downloadWithYtDlp(videoId);
            if (result != null) return result;
        } catch (Exception e) {
            log.warn("yt-dlp download failed: {}", e.getMessage());
        }

        // 2. Try RapidAPI
        try {
            return downloadWithRapidApi(videoId);
        } catch (Exception e) {
            log.warn("RapidAPI download failed: {}", e.getMessage());
        }

        // 3. Last resort: Innertube Player API (likely blocked on datacenter IPs)
        try {
            return downloadWithPlayerApi(videoId);
        } catch (Exception e) {
            log.warn("Innertube Player API download failed: {}", e.getMessage());
            throw new IOException("All download methods failed for video " + videoId
                + ". YouTube may be blocking this server's IP address.", e);
        }
    }

    // ===== yt-dlp Download =====

    private Path downloadWithYtDlp(String videoId) throws IOException, InterruptedException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);

        Path outputPath = dir.resolve(videoId + ".%(ext)s");
        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        // Try multiple yt-dlp strategies in order
        String[][] strategies = {
            // Strategy 1: mweb client (mobile web - least restricted)
            {
                ytdlpPath,
                "--no-check-certificates",
                "--no-warnings",
                "--prefer-free-formats",
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--extractor-args", "youtube:player_client=mweb",
                "--no-playlist",
                "--no-part",
                "-o", dir.resolve(videoId + ".mp3").toString(),
                videoUrl
            },
            // Strategy 2: ios client
            {
                ytdlpPath,
                "--no-check-certificates",
                "--no-warnings",
                "--prefer-free-formats",
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--extractor-args", "youtube:player_client=ios",
                "--no-playlist",
                "--no-part",
                "-o", dir.resolve(videoId + ".mp3").toString(),
                videoUrl
            },
            // Strategy 3: default client with --force-generic-extractor off
            {
                ytdlpPath,
                "--no-check-certificates",
                "--no-warnings",
                "--prefer-free-formats",
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--no-playlist",
                "--no-part",
                "-o", dir.resolve(videoId + ".mp3").toString(),
                videoUrl
            }
        };

        for (int i = 0; i < strategies.length; i++) {
            log.info("yt-dlp strategy {} for video {}", i + 1, videoId);
            try {
                Path result = executeYtDlp(strategies[i], videoId, dir);
                if (result != null) return result;
            } catch (Exception e) {
                log.warn("yt-dlp strategy {} failed: {}", i + 1, e.getMessage());
                // Clean up any partial files before trying next strategy
                cleanPartialFiles(dir, videoId);
            }
        }

        throw new IOException("All yt-dlp strategies failed for video " + videoId);
    }

    private Path executeYtDlp(String[] command, String videoId, Path dir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.environment().put("HOME", dir.toString());

        log.info("Running: {}", String.join(" ", command));
        Process process = pb.start();

        // Read output in a separate thread to prevent blocking
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
        outputReader.start();

        boolean finished = process.waitFor(ytdlpTimeout, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            outputReader.join(5000);
            throw new IOException("yt-dlp timed out after " + ytdlpTimeout + " seconds");
        }

        outputReader.join(5000);
        int exitCode = process.exitValue();

        if (exitCode != 0) {
            String errorOutput = output.toString();
            log.warn("yt-dlp exited with code {}: {}", exitCode, errorOutput);
            throw new IOException("yt-dlp failed (exit " + exitCode + "): " + truncate(errorOutput, 200));
        }

        // Find the output file (yt-dlp may produce .mp3, .m4a, .opus, .webm)
        Path mp3 = dir.resolve(videoId + ".mp3");
        if (Files.exists(mp3) && Files.size(mp3) > 1000) {
            log.info("yt-dlp saved: {} ({} bytes)", mp3.getFileName(), Files.size(mp3));
            return mp3;
        }

        // Check for other extensions
        String[] extensions = {".m4a", ".opus", ".webm", ".ogg", ".wav"};
        for (String ext : extensions) {
            Path alt = dir.resolve(videoId + ext);
            if (Files.exists(alt) && Files.size(alt) > 1000) {
                log.info("yt-dlp saved: {} ({} bytes)", alt.getFileName(), Files.size(alt));
                return alt;
            }
        }

        throw new IOException("yt-dlp completed but no output file found");
    }

    private void cleanPartialFiles(Path dir, String videoId) {
        String[] extensions = {".mp3", ".m4a", ".opus", ".webm", ".ogg", ".wav", ".part", ".ytdl"};
        for (String ext : extensions) {
            try {
                Path f = dir.resolve(videoId + ext);
                if (Files.exists(f)) Files.deleteIfExists(f);
            } catch (IOException ignored) {}
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ===== RapidAPI Download =====

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

    // ===== Innertube Player API (last resort) =====

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
