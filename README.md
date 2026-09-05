# HerEyes – Retro‑Arcade Audio Streaming Server

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**.  
The front‑end, built with vanilla JavaScript, the Web Audio API, and CSS, mimics a cassette player: a spinning reel, an animated 32‑band equalizer, and album art displayed off‑screen. Player state (track, position, EQ) is persisted to `localStorage`, so you resume exactly where you left off.

---

## Quick links

| Resource | URL |
|----------|-----|
| Repository | <https://github.com/shubhyagami/streaming> |
| Docker image | <https://hub.docker.com/r/hereyes/streaming> |
| CI status | <https://github.com/shubhyagami/streaming/actions> |
| Issues | <https://github.com/shubhyagami/streaming/issues> |
| API docs | <https://github.com/shubhyagami/streaming/blob/main/docs/api.md> |

---

## Features

| Feature | Description |
|---|---|
| **Multi‑source playback** | Search and play from YouTube or Spotify, with a YouTube fallback when Spotify fails or the quota is exceeded. |
| **Real‑time equalizer** | 25 presets plus a 32‑band graphic EQ that updates live. |
| **Retro UI** | Spinning cassette reel, animated equalizer, off‑screen album art. |
| **Session persistence** | Track, position, and EQ settings are restored after a page reload. |

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 26, Spring Boot 3, Gradle 9 |
| Container | Docker |
| Front‑end | Vanilla JavaScript, Web Audio API, CSS |
| CI | GitHub Actions |
| VCS | Git |

---

## Getting started

> **Prerequisites**  
> - Java 26 (or newer)  
> - Gradle 9 (or the Gradle wrapper)  
> - Docker (optional, for containerised deployment)

### Clone and run locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open <http://localhost:8080> and start searching for tracks.

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
4. **Persist** – refresh the page; the last track and EQ settings load automatically.

---

## Development

### Running tests

```bash
./gradlew test
```

### Linting & code quality

```bash
./gradlew spotlessApply
./gradlew checkstyleMain
```

### Adding a new source provider

1. Create a new implementation of `TrackSource` (e.g., for Apple Music).  
2. Wire it into `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repository and create a feature branch.  
2. Add tests covering your changes.  
3. Ensure all checks pass locally (`./gradlew check`).  
4. Open a pull request against `main`.  
5. Keep commits focused and follow the commit‑style guidelines.

See `CONTRIBUTING.md` for additional details.

---

## License

HerEyes is released under the MIT License. See the [LICENSE](LICENSE) file.

---

## Changelog

| Version | Date | Notes |
|---|---|---|
| **1.0.0** | 2026‑08‑29 | Initial release, Docker support |
| 0.9.0 | 2026‑08‑06 | Added three new EQ presets, optimized GC logs |
| 0.8.0 | 2026‑07‑22 | Refactored source selection logic |

---

## Badges

![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)
![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)
