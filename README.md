# HerEyes :Memory Server of Ex

> A retro‑arcade‑styled audio streaming server with YouTube/Spotify search, real‑time Web Audio EQ, and a cassette‑player dashboard — all in a single‑page Spring Boot application.

---

<p align="center">
  <img src="https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk&logoColor=white" alt="Java 26">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.4.4">
  <img src="https://img.shields.io/badge/Gradle-9.0-02303A?logo=gradle&logoColor=white" alt="Gradle 9.0">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License MIT">
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs welcome">
  <img src="https://img.shields.io/badge/TVA-Temporal_Engineer-FF6F00" alt="TVA Temporal Engineer">
  <img src="https://img.shields.io/github/repo-size/shubhyagami/streaming" alt="Repo size">
</p>

---

## Features

### Audio Streaming
- **YouTube search** — via Innertube API (no API key needed)
- **MP3 download** — via RapidAPI, streamed on the fly
- **Spotify fallback** — RapidAPI search; auto‑falls back to YouTube when rate‑limited
- **Stream‑and‑delete** — temp files auto‑clean every 60s (10 min TTL)
- **Zero external tools** — no yt‑dlp, no ffmpeg

### Audio Processing
- **32‑band Graphic EQ** — 1/3‑octave ISO centers (20 Hz–20 kHz) like Poweramp
- **Web Audio API real‑time EQ** — 31 BiquadFilter peaking nodes + lowshelf/highshelf tone
- **Tone Control** — Bass (−12/+12 dB @ 250 Hz) & Treble (−12/+12 dB @ 4 kHz)
- **LED Spectrum Analyzer** — AnalyserNode‑powered, HSL random color cycling
- **22 built‑in EQ presets** seeded on boot, full CRUD REST API
- **Limiter, Stereo FX, Tempo (WSOLA), Reverb (Schroeder)** — server‑side DSP engine

### UI / UX
- **Single‑page application** — all sections (dashboard, EQ, player, presets) in one HTML; audio never stops
- **Realistic SVG cassette player** — spinning reels, album art, time/progress bar
- **Retro arcade theme** — Press Start 2P font, neon accents, CRT scanline overlay, pixel borders
- **Fullscreen MP4 background** — `background.mp4` with dark overlay, translucent card panels
- **Sinewave seekbar canvas** — animated waveform with click‑to‑seek
- **Session persistence** — player state survives page navigation via `sessionStorage`
- **Source toggle** — YouTube / Spotify radio buttons

### Backend
- **Spring Boot 3.4.4** — Java 26, Gradle 9.0
- **H2 Database** — presets & config persisted; embedded console at `/h2-console`
- **REST API** — `/api/presets`, `/api/frs`, `/api/processing/jobs`, `/api/yt/*`, `/api/spotify/*`
- **Docker‑ready** — multi‑stage `Dockerfile` (`gradle:8-jdk21` → `eclipse-temurin:...`)

---

## 🕰️ TVA Temporal Engineer’s Log

**Date:** 2026-08-06  
**Operator:** shubhyagami  
**Case File:** `streaming-nexus-042`

*The timeline is stable. HerEyes continues to deliver crystal‑clear audio across all branches of the Sacred Timeline. No Variants detected. The cassette player’s reels spin in perfect sync with the Web Audio EQ. Today’s maintenance included recalibrating the 32‑band graphic equalizer to match ISO standards, seeding 3 new presets (“Midnight Rain”, “Vaporwave Drift”, “Lo‑Fi Café”), and verifying the stream‑and‑delete garbage collector — all temp files purged within 60s as expected. The background MP4 loop now includes a hidden easter egg: a neon‑pink “TVA” glitch at frame 2048. Proceed as normal.*

---

## 💡 Pro Tips

- **Equalizer quick‑switch:** Double‑click any preset name in the dashboard to load it instantly without opening the EQ panel.
- **Keyboard shortcuts:** Press `Space` to play/pause, `←`/`→` to skip 10s, `↑`/`↓` to adjust volume (when not in an input field).
- **H2 database:** Access the embedded console at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:file:./data/streaming`. The `PRESETS` table lets you inspect or edit all saved equalizer profiles directly.
- **Docker memory:** If running in a container with limited RAM, add `-e JAVA_OPTS="-Xmx512m"` to keep the JVM lean.
- **Background video:** Replace `background.mp4` in `src/main/resources/static/` with your own MP4 (ideally 1920×1080, ~10‑15s loop) to customize the arcade vibe.

---

## 📅 Changelog — 2026‑08‑06

### Added
- 3 new built‑in EQ presets: “Midnight Rain”, “Vaporwave Drift”, “Lo‑Fi Café”
- Hidden TVA easter egg in background video (frame 2048)
- Pro Tips section in README

### Changed
- Fine‑tuned all 22 existing presets to align with ISO 1/3‑octave center frequencies
- Improved stream‑and‑delete GC logging (now includes file size and deletion latency)

### Fixed
- Rare race condition when switching between YouTube and Spotify sources mid‑stream
- Sinewave seekbar not updating after a source toggle in Safari

---

## 🚀 Weekly Highlight

**“Rainy Night” preset is now the most requested EQ setting across the TVA branches!**  
Featuring a gentle low‑end boost (+4 dB @ 63 Hz), scooped mids (−2 dB @ 1 kHz), and airy highs (+3 dB @ 16 kHz). Perfect for jazz vocals, lo‑fi beats, or late‑night coding sessions. Try it out — the cassette reels glow a soft blue when loaded.

---

## Quick Start

### Prerequisites
- **Java 26** (or compatible JDK)
- **Gradle 9.0** (or use the Gradle wrapper)
- **Docker** (optional, for containerized deployment)

### Run Locally
```bash
# Clone the repository
git clone https://github.com/shubhyagami/streaming.git
cd streaming

# Build and run
./gradlew bootRun
```

### Access the Application
- **Web UI**: `http://localhost:8080`

---

<p align="center">
  <i>“In the end, all that matters is the music — and the timeline.”</i><br>
  <sub>— Temporal Engineer shubhyagami, TVA Archives</sub>
</p>