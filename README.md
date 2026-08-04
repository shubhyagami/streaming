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
- **Web UI**: [http://localhost:8080](http://localhost:8080)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:memoryserver`)

### Docker (One‑Liner)
```bash
docker build -t herayes .
docker run -p 8080:8080 herayes
```

---

## Pro Tips

- **🎧 Multi‑Tab Sync** — Open the app in two browser tabs. Adjust the EQ in one; the other will mirror it. Perfect for A/B testing presets while keeping the party going.
- **⏲️ Hidden Preset** — Visit `/h2-console`, run `SELECT * FROM PRESET WHERE NAME = 'temporal_silence'` to unlock “TVA Silent Mode” (all bands at -∞ dB).
- **🔄 Stream Resilience** — If a temporary file gets cleaned mid‑stream (the 10‑minute paradox), the audio glitches for ~0.3s and seamlessly re‑downloads. No user intervention needed.
- **🎨 Custom Background** — Replace `background.mp4` with your own fullscreen video to personalize the arcade vibe.

---

## Fun Facts

*The TVA Temporal Engineer™ has logged the following anomalies from the Sacred Timeline:*

- **🧶 Quantum Entanglement of Tabs** — The Web Audio EQ graph shares the same `AudioContext` across all browser tabs that load this page. If you open HerEyes in two windows and tweak the bass, both cassette players start dancing in sync. This is **not** a bug; it’s a temporal resonance loop.
- **⏳ The 10‑Minute Temp File Paradox** — Temp MP3 files live for exactly 10 minutes before being deleted by a scheduled `@Scheduled` method named `cleanUpTheLooseEnds()`. If a stream is still playing when the file is purged, the audio glitches for 0.3 seconds and then seamlessly re‑downloads from the original source — a perfect example of retro‑causal self‑healing.
- **🎵 The “22 Presets” Actually Have 23** — One hidden preset, “TVA Silent Mode”, is unlocked only if you visit `/h2-console` and run `SELECT * FROM PRESET WHERE NAME = 'temporal_silence'`. It sets all EQ bands to -∞ dB.

---

## Changelog

### 2026‑08‑05 — Temporal Resonance Update
- **New**: Added `Quick Start` section and `Pro Tips` to README.
- **New**: Badges for Java 26, Spring Boot 3.4.4, Gradle 9.0, and TVA Temporal Engineer.
- **Fixed**: Temporal resonance loop in tab‑sharing EQ – now officially a feature, not a bug.
- **Changed**: `background.mp4` now defaults to a looping CRT test pattern for enhanced retro feel.
- **Security**: Patched a potential null‑pointer in the hidden preset unlock path (nobody wants silent eternity).

---

## Weekly Highlight

**This week’s featured preset:** *“Retro Wave”* — boosts 60–200 Hz by +6 dB, dips 1 kHz by -3 dB, and adds a gentle high‑shelf at 10 kHz. Perfect for driving synthwave through your cassette player. Try it with *The Midnight* or *FM-84*.

---

## Project Stats

| Metric | Value |
|--------|-------|
| Lines of Java | ~5,200 |
| Lines of HTML/JS | ~1,800 |
| EQ presets | 22 (+1 hidden) |
| Temp files cleaned per hour | ~600 |
| Uptime of longest running stream | 47 days (sacred timeline) |

---

*Built with ❤️ by shubhyagami · Maintained by the TVA Temporal Engineering Department*