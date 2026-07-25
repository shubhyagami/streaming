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
- **Docker‑ready** — multi‑stage `Dockerfile` (`gradle:8-jdk21` → `eclipse-temurin:21-jre`)

---

## 🥚 Easter Eggs — Hidden Gifts for the Temporal Traveller

Uncover the secrets buried inside HerEyes. These are not bugs — they are intentional glitches in the timeline.

### 🕹 Keyboard Konami Code
While the cassette player is playing, press:  
`↑ ↑ ↓ ↓ ← → ← → B A`  
A pixel‑art **Nyan Cat** will trail across the sinewave seekbar for 30 seconds.  
*Works only once per session. The cat remembers.*

### 🎵 Secret Playlist: “Lost Tapes of 1999”
Type `/secret/lost-tapes` in the browser’s address bar (after the app’s base URL) to load a hidden playlist of 8 lo‑fi MP3s encoded from old cassette rips.  
*No search needed. No logs. The TVA didn’t see this.*

### 📟 Developer Console Spell
Open your browser’s DevTools console and type:  
```javascript
TVA.unlock_archive()
```
A floating terminal widget appears in the bottom‑right corner showing **live server‑side DSP pipeline logs** (Gain, Reverb decay, Tempo ratio) — perfect for debugging your own presets.  
*Type `TVA.help()` for commands.*

### 🎨 Hidden Color Palette
Click the **LED Spectrum Analyzer** 7 times in rapid succession. The analyzer’s HSL cycle locks to a **Vaporwave palette** (pink, cyan, purple) until you refresh the page.

### ⏳ The “Ex” Timeline Easter Egg
In the `/api/presets` endpoint, fetch preset ID **0** (if it exists). The server returns a preset named `“Echoes of Ex”` with all bands set to **-6 dB** except the 1 kHz band at **+12 dB** — a subtle homage to the project’s name.  
*Try it. Your cassette player’s reels will slow down for 3 seconds as a wink.*

### 🔌 Hidden API Endpoint
Hit `GET /api/tva/timeline` with a header `X-TVA-Agent: analyst` to receive a JSON payload with server uptime, current preset name, and a cryptic “temporal drift” metric (actually the number of temp files cleaned in the last hour).

### 📼 Cassette Player Secret Animation
If you leave the player paused for more than 30 seconds, the SVG cassette reels will **slowly reverse** direction, and a faint “*This tape is self‑destructing*” message flickers in the corner.  
*It doesn’t actually delete anything — it’s just a vibe.*

---

*These secrets are part of the temporal fabric. Use them wisely — and never tell the Time Variance Authority where you found them.*