# HerEyes – Audio Streaming Server

**HerEyes** is a lightweight Spring Boot backend that streams audio from YouTube and Spotify, paired with a vanilla‑JavaScript front‑end that mimics a cassette player. The UI features a spinning reel, a 32‑band graphic equalizer, and sliding album art. All player state (track, position, EQ) is persisted in `localStorage`, so a session survives a page reload.

---

## Quick links

- [Source code](https://github.com/shubhyagami/streaming)
- [Docker image](https://hub.docker.com/r/hereyes/streaming)
- [GitHub Actions](https://github.com/shubhyagami/streaming/actions)
- [API documentation](https://github.com/shubhyagami/streaming/blob/main/docs/api.md)

---

## Features

| ✅ | Feature |
|---|---------|
| | Streaming from **YouTube** and **Spotify** (fallback to YouTube if Spotify fails) |
| | Live 32‑band graphic equalizer plus 25 preset EQ profiles |
| | Cassette‑style UI: spinning reel, sliding album art |
| | Player state persisted locally (track, position, EQ) |

---

## Architecture

| Layer | Technology |
|-------|-------------|
| Backend | Java 26 / Spring Boot 3 (Gradle 9) |
| Front‑end | Vanilla JavaScript + Web Audio API |
| CI / CD | GitHub Actions |
| Container | Docker |
| Source Control | Git |

---

## Getting started

### Prerequisites

- Java 26 (or newer)
- Gradle 9 (or the bundled Gradle wrapper)
- Docker (optional, for container deployment)

### Run locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew bootRun   # builds and starts the server
```

Open <http://localhost:8080> in a browser.

### Run with Docker

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; playback starts automatically and the album art slides in.  
3. **Adjust EQ** – select a preset or manipulate the sliders.  
4. **Persist** – refresh the page; the last track and EQ settings are restored automatically.

---

## Development

```bash
# Run tests
./gradlew test

# Apply code formatting
./gradlew spotlessApply

# Run static‑analysis checks
./gradlew checkstyleMain
```

### Adding a new source

1. Create a class that implements `TrackSource`.  
2. Register it in `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repository and create a feature branch off `main`.  
2. Add unit tests for your changes.  
3. Run `./gradlew check` locally to ensure all checks pass.  
4. Open a pull request.  
5. Follow the conventional‑commit message style.

See the [CONTRIBUTING.md](CONTRIBUTING.md) file for more information.

---

## License

HerEyes is released under the [MIT License](LICENSE).

---

## Changelog

- **1.0.0** – 2026‑08‑29 – Initial release with Docker support.  
- **0.9.0** – 2026‑08‑06 – Added three EQ presets, optimized GC.

---

## Badges

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)
