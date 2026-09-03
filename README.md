# HerEyes – Retro‑Arcade Audio Streaming Server

A lightweight Spring Boot application that streams audio from **YouTube** and **Spotify**, displays a cassette‑player style UI, and lets you tweak the sound in real time with a 32‑band graphic equalizer.

---

## Overview

* Play tracks from YouTube or Spotify.
* Automatic fallback to YouTube when a Spotify request fails.
* Cassette‑player UI with spinning reel, album art, and an on‑screen equalizer.
* Player state (current track, position, EQ settings) is persisted in local storage for a seamless reload.

---

## Features

| Feature | Description |
|---------|-------------|
| **Multi‑source playback** | Search and play from YouTube or Spotify; fallback on API limits. |
| **Real‑time equalizer** | 25 presets + 32‑band graphic EQ with live spectrum display. |
| **Cassette‑player UI** | Classic look built with vanilla JS, Web Audio API, and CSS animations. |
| **Session persistence** | State stored in local storage; restores after page reload. |

---

## Tech Stack

* **Java 26** + **Spring Boot 3**
* **Gradle 9** (wrapper provided)
* **Docker** for containerised deployment
* Front‑end: vanilla JavaScript, Web Audio API, CSS animations

---

## Getting Started

### Prerequisites

* Java 26 (or higher)
* Gradle 9 (or use the wrapper)
* Docker (optional)

### Install

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open `http://localhost:8080` in your browser.

### Docker

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; the UI shows album art and starts playback.  
3. **Equalize** – select a preset or adjust the sliders.  
4. **Persist** – refresh the page; the current track and EQ settings load automatically.

---

## Contributing

Pull requests are welcome.  
1. Open an issue to discuss a change.  
2. Fork the repository and create a feature branch.  
3. Add tests; run `./gradlew test` to verify.  
4. Follow existing style guidelines.  
5. Update documentation if needed.

---

## License

HerEyes is licensed under the [MIT License](LICENSE).

---

## Changelog

- **2026‑08‑29** – Initial release, added Docker support.  
- **2026‑08‑06** – Added three new EQ presets, optimized GC logs.

---

## Badges

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)  
[![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)](https://github.com/shubhyagami/streaming/actions)  
[![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)](https://hub.docker.com/r/hereyes/streaming)  
[![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)](https://github.com/shubhyagami/streaming/issues)
