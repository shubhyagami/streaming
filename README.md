# HerEyes: Memory Server of Ex

A retro-arcade-styled audio streaming server with YouTube and Spotify search, real-time Web Audio EQ, and a cassette-player dashboard. Built as a single-page Spring Boot application.

<p align="center">
  <img src="https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk&logoColor=white" alt="Java 26">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.4.4">
  <img src="https://img.shields.io/badge/Gradle-9.0-02303A?logo=gradle&logoColor=white" alt="Gradle 9.0">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License MIT">
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs welcome">
  <img src="https://img.shields.io/github/repo-size/shubhyagami/streaming" alt="Repo size">
</p>

## Features

### Audio Streaming & Sources
- **YouTube Search:** Keyless video search and streaming powered by the Innertube API.
- **Spotify Fallback:** Searches via RapidAPI; automatically falls back to YouTube if rate-limited.
- **Temporary Downloads:** Streams audio on the fly without saving permanent files to disk.
- **Garbage Collection:** Temporary files are automatically purged every 60 seconds (10-minute TTL).
- **Zero External Tools:** No need to install `yt-dlp` or `ffmpeg` on the host machine.

### Audio Processing
- **32-Band Graphic EQ:** 1/3-octave ISO center frequencies (20 Hz–20 kHz) comparable to professional desktop players.
- **Real-Time Web Audio EQ:** 31 `BiquadFilter` peaking nodes combined with dedicated lowshelf/highshelf tone controls.
- **Tone Control:** Independent Bass (−12/+12 dB @ 250 Hz) and Treble (−12/+12 dB @ 4 kHz) adjustments.
- **Visual Analyzer:** LED-style spectrum analyzer powered by `AnalyserNode` with HSL color cycling.
- **Built-in Presets:** 22 built-in EQ presets seeded on boot, manageable via a full CRUD REST API.
- **Server-Side DSP:** Includes Limiter, Stereo FX, Tempo (WSOLA), and Reverb (Schroeder) processing capabilities.

### User Interface
- **Single-Page Application:** Navigate between the dashboard, EQ, player, and presets without interrupting audio playback.
- **SVG Cassette Player:** Realistic UI featuring spinning reels, album art display, and time/progress bars.
- **Retro Arcade Theme:** Press Start 2P font, neon accents, CRT scanline overlay, and pixel borders.
- **Dynamic Background:** Fullscreen `background.mp4` with a dark overlay and translucent card panels.
- **Interactive Seekbar:** Sinewave canvas waveform with click-to-seek functionality.
- **Session Persistence:** Player state survives page refreshes and navigation via `sessionStorage`.

### Backend
- **Spring Boot 3.4.4:** Built on Java 26 and managed with Gradle 9.0.
- **H2 Database:** Presets and configuration persisted in an embedded database.
- **REST API:** Exposes endpoints under `/api/presets`, `/api/frs`, `/api/processing/jobs`, `/api/yt/*`, and `/api/spotify/*`.
- **Docker-Ready:** Multi-stage `Dockerfile` included for quick containerized deployment.

## Getting Started

### Prerequisites
- **Java 26** (or compatible JDK)
- **Gradle 9.0** (or use the included Gradle wrapper)
- **Docker** (optional, for containerized deployment)

### Run Locally
```bash
# Clone the repository
git clone https://github.com/shubhyagami/streaming.git
cd streaming

# Build and run the application
./gradlew bootRun
```

Once running, access the web interface at `http://localhost:8080`.

## Pro Tips

- **Equalizer Quick-Switch:** Double-click any preset name in the dashboard to load it instantly without opening the full EQ panel.
- **Keyboard Shortcuts:** Press `Space` to play/pause, `←`/`→` to skip 10 seconds, and `↑`/`↓` to adjust volume (works when not focused on an input field).
- **H2 Database Console:** Access the embedded console at `http://localhost:8080/h2-console` using the JDBC URL `jdbc:h2:file:./data/streaming`. The `PRESETS` table allows you to inspect or edit saved equalizer profiles directly.
- **Docker Memory Limits:** If running in a container with restricted RAM, pass `-e JAVA_OPTS="-Xmx512m"` to keep the JVM memory footprint lean.
- **Custom Backgrounds:** Replace `background.mp4` in `src/main/resources/static/` with your own video (ideally a 1920×1080, 10-15 second loop) to customize the arcade vibe.

## Changelog

### 2026-08-21
#### Changed
- Refreshed README structure for clarity and improved readability.
- Updated environment and dependency documentation notes.

### 2026-08-06
#### Added
- 3 new built-in EQ presets: "Midnight Rain", "Vaporwave Drift", "Lo-Fi Café".
- Pro Tips section in the documentation.
#### Changed
- Fine-tuned all 22 existing presets to strictly align with ISO 1/3-octave center frequencies.
- Improved stream-and-delete garbage collector logging to include file size and deletion latency.
#### Fixed
- Rare race condition when switching between YouTube and Spotify sources mid-stream.
- Sinewave seekbar failing to update after a source toggle in Safari.

---

<p align="center">
  <i>In the end, all that matters is the music.</i><br>
</p>
