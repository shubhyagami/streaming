# HerEyes – Retro‑Arcade Audio Streaming Server

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**.  
The front‑end mimics a cassette player, built with vanilla JavaScript, the Web Audio API, and CSS animations. It offers a 32‑band graphic equalizer that can be tuned in real time. Player state (track, position, EQ) is persisted to `localStorage`, so you resume exactly where you left off.

---

## Quick links

|   |   |
|---|---|
| **Repository** | <https://github.com/shubhyagami/streaming> |
| **Docker image** | <https://hub.docker.com/r/hereyes/streaming> |
| **Live demo** | (if deployed) |
| **API docs** | (link if available) |

---

## Features

- **Multi‑source playback** – search and play from YouTube or Spotify, with an automatic YouTube fallback if a Spotify request fails or the quota is exceeded.  
- **Real‑time equalizer** – 25 presets plus a 32‑band graphic EQ that shows the live spectrum.  
- **Retro UI** – spinning cassette reel, album art, and animated equalizer.  
- **Session persistence** – restores the current track, position, and EQ settings after a page reload.  

---

## Technology stack

| Component | Technology |
|-----------|-------------|
| **Backend** | Java 26, Spring Boot 3, Gradle 9 |
| **Container** | Docker |
| **Front‑end** | Vanilla JavaScript, Web Audio API, CSS |
| **CI** | GitHub Actions |
| **Version control** | Git |

---

## Getting started

### Prerequisites

- Java 26 (or newer)  
- Gradle 9 (or the Gradle wrapper)  
- Docker (optional, for containerised deployment)

### Clone and run locally

```text
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open <http://localhost:8080> to start searching for tracks.

### Docker

```text
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; album art appears and playback starts.  
3. **Adjust EQ** – choose a preset or move the sliders.  
4. **Persist** – refresh the page; the last track and EQ settings reload automatically.

---

## Development

### Running tests

```text
./gradlew test
```

### Linting & code quality

```text
./gradlew spotlessApply
./gradlew checkstyleMain
```

### Adding a new source provider

1. Create a new implementation of `TrackSource` (e.g., for Apple Music).  
2. Wire it into `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repo and create a feature branch.  
2. Add tests that exercise your changes.  
3. Verify locally with `./gradlew test`.  
4. Open a pull request against `main`.  
5. Keep commits focused and follow the commit style guidelines.  

See `CONTRIBUTING.md` for more details.

---

## License

HerEyes is released under the MIT License. See the [LICENSE](LICENSE) file.

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| **1.0.0** | 2026‑08‑29 | Initial release, Docker support |
| 0.9.0 | 2026‑08‑06 | Added three new EQ presets, optimized GC logs |
| 0.8.0 | 2026‑07‑22 | Refactored source selection logic |

---

## Badges

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)  
[![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)](https://github.com/shubhyagami/streaming/actions)  
[![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)](https://hub.docker.com/r/hereyes/streaming)  
[![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)](https://github.com/shubhyagami/streaming/issues)  
[![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)](https://github.com/shubhyagami/streaming)
