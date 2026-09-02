# HerEyes – Retro‑Arcade Audio Streaming Server  

A lightweight Spring Boot application that streams music from YouTube and Spotify, renders a cassette‑player style UI, and lets you tweak the sound in real time with a 32‑band graphic equalizer.  

---  

## Overview  

* Play audio from both **YouTube** and **Spotify**.  
* If a Spotify request fails, the server automatically falls back to a YouTube search.  
* The front‑end mimics a classic cassette player: spinning reel, album art, and an on‑screen equalizer.  
* The player state (current track, position, and EQ settings) is saved in local storage, so reloading the page restores playback seamlessly.  

---  

## Features  

- **Multi‑source playback** – search and play from YouTube or Spotify; automatic fallback on API limits.  
- **Real‑time equalizer** – 25 presets + a 32‑band graphic EQ with live spectrum display.  
- **Cassette‑player UI** – HTML5 + vanilla JavaScript with CSS animations.  
- **Session persistence** – local storage keeps track and EQ state across page reloads.  

---  

## Tech Stack  

- **Java 26** + **Spring Boot 3**  
- **Gradle 9** (wrapper included)  
- **Docker** for containerised deployment  
- Front‑end: vanilla JS, Web Audio API, CSS animations  

---  

## Quick Start  

Clone the repository, build, and run:  

```
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open `http://localhost:8080` in your browser.  

---  

## Docker (optional)  

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```  

---  

## Usage  

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; the UI shows album art and starts playback.  
3. **Equalize** – select a preset or adjust the sliders.  
4. **Persist** – refresh the page and the current track + EQ settings will load automatically.  

---  

## Badges  

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)  
[![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)](https://github.com/shubhyagami/streaming/actions)  
[![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)](https://hub.docker.com/r/hereyes/streaming)  
[![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)](https://github.com/shubhyagami/streaming/issues)  

---  

## Contributing  

Pull requests are welcome! Please follow these guidelines:  

1. Open an issue first to discuss the change.  
2. Fork the repository and checkout a feature branch.  
3. Write tests – run `./gradlew test` to verify.  
4. Ensure code follows the existing style guidelines.  
5. Update documentation if necessary.  

---  

## License  

HerEyes is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.  

---  

## Changelog  

**2026‑08‑29** – Initial release, cleaned up README, added Docker support.  
**2026‑08‑06** – Added three new EQ presets, optimized GC logs.  
