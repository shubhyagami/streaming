# HerEyes: Memory Server of Ex

A retro‑arcade‑styled audio streaming server that blends nostalgic visual design with modern features—YouTube and Spotify search, real‑time Web Audio equalization, and a cassette‑player dashboard.

---

## Features

- **Multi‑Source Streaming** – Search YouTube and Spotify; falls back to YouTube when rate limits are hit.  
- **Real‑Time EQ** – 25 built‑in presets plus a full 32‑band graphic equalizer with dynamic spectrum analysis.  
- **Cassette‑Player Dashboard** – Interactive HTML5 UI with spinning reel animation, album art, and playback controls.  
- **Session Persistence** – Player state survives page refreshes and navigation changes.  

---

## Badges

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)  
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/shubhyagami/streaming/actions)  
[![Contributions Welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](https://github.com/shubhyagami/streaming/issues)

---

## Getting Started

### Prerequisites
- **Java 26**  
- **Gradle 9.0** (or use the provided wrapper)  
- **Docker** (optional, for container deployment)

### Run Locally
```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew bootRun
```
Open your browser at `http://localhost:8080`.

---

## Contributing
We welcome contributions! Feel free to open an issue or submit a pull request.

---

## License
HerEyes is released under the **MIT License**—see the [LICENSE](LICENSE) file for details.

---

## Changelog

### 2026‑08‑26
- Refined README structure and clarified documentation.

### 2026‑08‑06
- Added three new EQ presets: *Midnight Rain*, *Vaporwave Drift*, *Lo‑Fi Café*.  
- Optimized all 22 existing presets to match ISO 1/3‑octave center frequencies.  
- Improved garbage‑collector logging for stream‑and‑delete operations.  

---  

*In the end, all that matters is the music.*
