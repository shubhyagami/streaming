# HerEyes – Retro‑Arcade Audio Streaming Server  

A lightweight Spring Boot application that streams music from YouTube and Spotify, renders a cassette‑player style UI, and lets you tweak the sound in real time with a 32‑band graphic equalizer.  

---

## Features  

| Feature | Details |
|---------|---------|
| **Multi‑source playback** | Search YouTube or Spotify; falls back to YouTube when Spotify’s API rate limit is hit. |
| **Real‑time equalizer** | 25 ready‑made presets plus a 32‑band graphical EQ with live spectrum display. |
| **Cassette‑player UI** | HTML5 front‑end with spinning reel, album art, and intuitive controls. |
| **Session persistence** | Player state is stored in local storage so it survives page refreshes and navigation changes. |

---

## Tech Stack  

* Java 26 + Spring Boot 3  
* Gradle 9 (wrapper included)  
* Docker (containerized deployment)  
* Front‑end: vanilla JS, Web Audio API, CSS animations  

---

## Installation  

### 1. Clone & Build  

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
```  

### 2. Run Locally  

```bash
./gradlew bootRun
```

Open your browser at `http://localhost:8080`.  

### 3. Docker (optional)  

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage  

1. **Search** – Type a query into the search bar.  
2. **Play** – Click the resulting track; the UI shows the album art and starts playback.  
3. **Equalize** – Choose a preset or adjust the sliders.  
4. **Persist** – Refresh the page and your current track + EQ settings will be restored automatically.  

---

## Badges  

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)  
[![Build Status](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)](https://github.com/shubhyagami/streaming/actions)  
[![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)](https://hub.docker.com/r/hereyes/streaming)  
[![Contributions](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](https://github.com/shubhyagami/streaming/issues)

---

## Contributing  

Pull requests are welcome! If you’d like to add a feature or fix a bug, open an issue first so we can discuss it.  

### Checklist  
- [ ] Tests pass (`./gradlew test`)  
- [ ] Code follows style guidelines  
- [ ] Update docs if necessary  

---

## License  

HerEyes is available under the MIT License – see the [LICENSE](LICENSE) file for details.  

---

## Changelog  

**2026‑08‑29** – Cleaned up README, reorganized sections, removed stale content.  
**2026‑08‑06** – Added three new EQ presets (*Midnight Rain*, *Vaporwave Drift*, *Lo‑Fi Café*), optimized existing presets, and improved GC logging.  

---
