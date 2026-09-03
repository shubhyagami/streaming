# HerEyes – Retro‑Arcade Audio Streaming Server

A lightweight Spring Boot (Java 26) backend that streams audio from **YouTube** and **Spotify**.  
The front‑end is a cassette‑player‑style UI written in vanilla JavaScript, the Web Audio API and CSS animations, and offers a 32‑band graphic equalizer that can be tuned in real time.  
Player state (track, position, EQ) is persisted in the browser, so you pick up right where you left off.

---

## Quick links

| | |
|---|---|
| **Repository** | <https://github.com/shubhyagami/streaming> |
| **Docker image** | <https://hub.docker.com/r/hereyes/streaming> |
| **Live demo** | (optional, if you have deployed) |
| **API documentation** | (link if available) |

---

## Features

- **Multi‑source playback** – search and play from YouTube or Spotify; automatic fallback to YouTube if a Spotify request fails or an API quota is exceeded.
- **Real‑time equalizer** – 25 presets and a 32‑band graphic EQ that displays the live spectrum.
- **Retro UI** – spinning cassette reel, album art, and on‑screen equalizer, all with CSS animations.
- **Session persistence** – current track, position and EQ settings are stored in localStorage and restored on reload.

---

## Tech stack

| Component | Technology |
|-----------|-----------|
| **Backend** | Java 26, Spring Boot 3, Gradle 9 |
| **Container** | Docker |
| **Front‑end** | Vanilla JavaScript, Web Audio API, CSS |
| **CI** | GitHub Actions |
| **Version control** | Git |

---

## Getting started

### Prerequisites

- Java 26 (or newer)  
- Gradle 9 (or use the Gradle wrapper)  
- Docker (optional, for containerized deployment)

### Clone and run locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open <http://localhost:8080> in your browser. The UI will load and you can start searching for tracks.

### Docker

```bash
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

### Running the test suite

```bash
./gradlew test
```

### Linting & code quality

The project uses Spotless and Checkstyle. Run the following to ensure code style compliance:

```bash
./gradlew spotlessApply
./gradlew checkstyleMain
```

### Adding a new source provider

The architecture separates source adapters behind an interface.  
Add a new implementation (e.g., Apple Music) by extending `TrackSource` and wiring it in `SourceConfig`.  
Run the test suite to verify all integration paths.

---

## Contributing

1. Fork the repo and create a feature branch.  
2. Write tests that exercise the new feature.  
3. Run `./gradlew test` locally to confirm all tests pass.  
4. Open a pull request against `main`.  
5. Keep your commits focused and our commit style in mind.

See `CONTRIBUTING.md` for more details.

---

## License

HerEyes is released under the MIT License. See the [LICENSE](LICENSE) file for details.

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
