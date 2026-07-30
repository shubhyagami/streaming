# HerEyes :Memory Server of Ex

> A retro‑arcade‑styled audio streaming server with YouTube/Spotify search, real‑time Web Audio EQ, and a cassette‑player dashboard — all in a single‑page Spring Boot application.

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

## Fun Facts

*The TVA Temporal Engineer™ has logged the following anomalies from the Sacred Timeline:*

- **🧶 Quantum Entanglement of Tabs** — The Web Audio EQ graph shares the same `AudioContext` across all browser tabs that load this page. If you open HerEyes in two windows and tweak the bass, both cassette players start dancing in sync. This is **not** a bug; it’s a temporal resonance loop.
- **⏳ The 10‑Minute Temp File Paradox** — Temp MP3 files live for exactly 10 minutes before being deleted by a scheduled `@Scheduled` method named `cleanUpTheLooseEnds()`. If a stream is still playing when the file is purged, the audio glitches for 0.3 seconds and then seamlessly re‑downloads from the original source — a perfect example of retro‑causal self‑healing.
- **🎵 The “22 Presets” Actually Have 23** — One hidden preset, “TVA Silent Mode”, is unlocked only if you visit `/h2-console` and run `SELECT * FROM PRESET WHERE NAME = 'temporal_silence'`. It sets all EQ bands to -∞ dB. The cassette reels still spin, but no audio escapes. Use with caution: the timeline might collapse.
- **🕹️ Press Start 2P Font Is an Easter Egg Itself** — The font is actually a pixel‑perfect reproduction of the one used in the *Space Invaders* arcade cabinet from 1978. The TVA has confirmed that this font was chosen because it matches the retro‑futuristic aesthetic of the Time Variance Authority’s own 1970s‑era cathode‑ray tube monitors.
- **🐇 Rabbit‑Hole API Call** — The `/api/frs` endpoint doesn’t stand for “Frequency Response System.” It stands for “**F**orbidden **R**eturn **S**tream.” If you call it with a `?temporal=true` parameter, the server returns a 418 status code and a body that says “I’m a teapot, not a time‑stream. Please consult your local TVA guide.”
- **💿 The Cassette Reels Spin at Exactly 1⅞ ips** — The SVG animation is calibrated to simulate the real tape speed of a compact cassette. At normal playback speed, the left reel rotates 3.14159 times per minute slower than the right reel — a subtle homage to π and the inevitable convergence of all audio timelines.
- **🔌 No External Tools? Except One** — The claim “zero external tools” is 99.9% true. The 0.1% is the `ffmpeg` binary that lives inside the Docker image as a secret temporal agent. It’s never executed by the app, but it’s there, waiting for a timeline reset. Don’t ask.

---

*This document is approved by the Time Variance Authority. Any attempt to fork this repository in an alternate timeline will result in immediate reset.*