# HerEyes: Memory Server of Ex

A retro-arcade-styled audio streaming server with YouTube and Spotify search, real-time Web Audio EQ, and a cassette-player dashboard. Built as a single-page Spring Boot application.

## Overview

HerEyes is a cutting-edge audio streaming server that brings together the best of retro gaming and modern technology. With its sleek cassette-player dashboard and retro arcade theme, users can easily navigate through the app's features.

## Features

### Audio Streaming & Sources

* **YouTube Search:** Search and stream videos from YouTube using the Innertube API.
* **Spotify Fallback:** Use RapidAPI to search and stream music from Spotify. If rate-limited, HerEyes will automatically fall back to YouTube.
* **Temporary Downloads:** HerEyes streams audio on the fly without saving permanent files to disk.
* **Garbage Collection:** Temporary files are automatically purged every 60 seconds (10-minute TTL).

### Audio Processing

* **32-Band Graphic EQ:** HerEyes features a 32-band graphic EQ with 1/3-octave ISO center frequencies (20 Hz–20 kHz).
* **Real-Time Web Audio EQ:** The app includes 31 `BiquadFilter` peaking nodes combined with dedicated lowshelf/highshelf tone controls.
* **Tone Control:** Users can adjust the bass and treble levels independently.
* **Visual Analyzer:** An LED-style spectrum analyzer powered by `AnalyserNode` with HSL color cycling.
* **Built-in Presets:** HerEyes comes with 22 built-in EQ presets that can be managed via a full CRUD REST API.

### User Interface

* **Single-Page Application:** The app features a single-page layout with a dashboard, EQ, player, and presets that can be accessed without interrupting audio playback.
* **SVG Cassette Player:** A realistic UI featuring spinning reels, album art display, and time/progress bars.
* **Retro Arcade Theme:** Press Start 2P font, neon accents, CRT scanline overlay, and pixel borders.
* **Dynamic Background:** A fullscreen `background.mp4` with a dark overlay and translucent card panels.
* **Interactive Seekbar:** A sinewave canvas waveform with click-to-seek functionality.
* **Session Persistence:** Player state survives page refreshes and navigation via `sessionStorage`.

### Backend

* **Spring Boot 3.4.4:** HerEyes is built on Java 26 and managed with Gradle 9.0.
* **H2 Database:** Presets and configuration are persisted in an embedded database.
* **REST API:** Endpoints for presets, configuration, and audio processing are exposed under `/api/presets`, `/api/frs`, `/api/processing/jobs`, `/api/yt/*`, and `/api/spotify/*`.
* **Docker-Ready:** A multi-stage `Dockerfile` is included for quick containerized deployment.

## Getting Started

### Prerequisites

* **Java 26** (or compatible JDK)
* **Gradle 9.0** (or use the included Gradle wrapper)
* **Docker** (optional, for containerized deployment)

### Run Locally

```bash
# Clone the repository
git clone https://github.com/shubhyagami/streaming.git
cd streaming

# Build and run the application
./gradlew bootRun
```

Access the web interface at `http://localhost:8080`.

## Pro Tips

* **Equalizer Quick-Switch:** Double-click any preset name in the dashboard to load it instantly without opening the full EQ panel.
* **Keyboard Shortcuts:** Press `Space` to play/pause, `←`/`→` to skip 10 seconds, and `↑`/`↓` to adjust volume (works when not focused on an input field).
* **H2 Database Console:** Access the embedded console at `http://localhost:8080/h2-console` using the JDBC URL `jdbc:h2:file:./data/streaming`. The `PRESETS` table allows you to inspect or edit saved equalizer profiles directly.
* **Docker Memory Limits:** If running in a container with restricted RAM, pass `-e JAVA_OPTS="-Xmx512m"` to keep the JVM memory footprint lean.
* **Custom Backgrounds:** Replace `background.mp4` in `src/main/resources/static/` with your own video (ideally a 1920×1080, 10-15 second loop) to customize the arcade vibe.

## License

HerEyes is licensed under the [MIT License](#).

## Contributing

Contributions are welcome! Please submit a pull request or create an issue on the GitHub repository.

## Changelog

### 2026-08-24

* Updated README with refreshed structure and improved readability.

### 2026-08-06

* Added three new built-in EQ presets: "Midnight Rain", "Vaporwave Drift", "Lo-Fi Café".
* Fine-tuned all 22 existing presets to strictly align with ISO 1/3-octave center frequencies.
* Improved stream-and-delete garbage collector logging to include file size and deletion latency.

<p align="center">
  <i>In the end, all that matters is the music.</i><br>
</p>

<a href="https://github.com/shubhyagami/streaming"><img src="https://img.shields.io/badge/GitHub-Use%20Source-Informational?logo=github" alt="Use the source"></a>
<a href="https://github.com/shubhyagami/streaming#license"><img src="https://img.shields.io/badge/License-MIT-blue" alt="License MIT"></a>
<a href="https://github.com/shubhyagami/streaming#contributing"><img src="https://img.shields.io/badge/Contributions-PRs%20Welcome-brightgreen" alt="Contributions welcome"></a>
