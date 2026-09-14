# HerEyes – Audio Streaming Server

**HerEyes** is a lightweight Spring Boot backend that streams audio from YouTube and Spotify.  
The client is a vanilla‑JavaScript app that emulates a cassette player: a spinning reel, a 32‑band equalizer, and sliding album art.  
All player state (track, position, EQ) is persisted in `localStorage`, so your session survives a page reload.

---

## Quick links

- [Source code](https://github.com/shubhyagami/streaming)  
- [Docker image](https://hub.docker.com/r/hereyes/streaming)  
- [GitHub Actions](https://github.com/shubhyagami/streaming/actions)  
- [API docs](https://github.com/shubhyagami/streaming/blob/main/docs/api.md)

---

## Features

| Feature | Description |
|---------|-------------|
| Multi‑source | Search & stream from YouTube or Spotify (fallback to YouTube if Spotify fails). |
| Real‑time EQ | 25 preset equalizers plus a live 32‑band graphic equalizer. |
| Retro UI | Cassette‑style reel animation and sliding album art. |
| Persistent state | Track, position and EQ settings are stored locally and restored on reload. |

---

## Architecture

| Layer | Technology |
|-------|------------|
| Backend | Java 26, Spring Boot 3, Gradle 9 |
| Front‑end | Vanilla JavaScript, Web Audio API, CSS |
| Container | Docker |
| CI/CD | GitHub Actions |
| Source control | Git |

---

## Getting started

### Prerequisites

* Java 26 or newer
* Gradle 9 (or the Gradle wrapper included)
* Docker (optional, for container deployment)

### Run locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open <http://localhost:8080> in a browser.

### Run with Docker

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query in the search bar.  
2. **Play** – click a result; album art slides in and playback starts automatically.  
3. **Adjust EQ** – choose a preset or manipulate the sliders.  
4. **Persist** – reload the page, and the last track with its EQ settings is restored.

---

## Development

### Run tests

```bash
./gradlew test
```

### Code quality checks

```bash
./gradlew spotlessApply
./gradlew checkstyleMain
```

### Adding a new track source

1. Create a class that implements `TrackSource` (e.g., for Apple Music).  
2. Register it in `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repo and create a branch off `main`.  
2. Add tests for your changes.  
3. Ensure all checks pass locally (`./gradlew check`).  
4. Submit a pull request.  
5. Follow the conventional‑commit style for commit messages.

See the [CONTRIBUTING.md](CONTRIBUTING.md) file for more details.

---

## License

HerEyes is released under the MIT License.  
See the [LICENSE](LICENSE) file.

---

## Changelog

| Version | Date | Notes |
|---------|------|-------|
| **1.0.0** | 2026‑08‑29 | Initial release, Docker support |
| 0.9.0 | 2026‑08‑06 | Added three EQ presets, optimized GC |
| 0.8.0 | 2026‑07‑22 | Refactored source selection logic |

---

## Badges

![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)
![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)
