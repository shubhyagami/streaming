# HerEyes – Audio Streaming Server

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**.  
A small vanilla‑JavaScript front‑end reproduces a cassette player: a spinning reel, a 32‑band equalizer, and album art shown off‑screen.  
Player state (track, position, EQ) is stored in `localStorage`, so you resume precisely where you left off.

---

## Quick links
- **Repository** – <https://github.com/shubhyagami/streaming>  
- **Docker image** – <https://hub.docker.com/r/hereyes/streaming>  
- **CI status** – <https://github.com/shubhyagami/streaming/actions>  
- **API docs** – <https://github.com/shubhyagami/streaming/blob/main/docs/api.md>

---

## Features

- **Multi‑source playback** – Search and play from YouTube or Spotify, with a YouTube fallback when Spotify fails or the quota is exceeded.  
- **Real‑time equalizer** – 25 preset EQs plus a live‑updating 32‑band graphic equalizer.  
- **Retro UI** – Spinning cassette reel, animated equalizer, and off‑screen album art.  
- **Session persistence** – Track, position, and EQ settings survive page reloads.

---

## Tech stack

- **Backend** : Java 26, Spring Boot 3, Gradle 9  
- **Front‑end** : Vanilla JavaScript, Web Audio API, CSS  
- **Container** : Docker  
- **CI/CD** : GitHub Actions  
- **Version control** : Git

---

## Getting started

> **Prerequisites**  
> • Java 26 (or newer)  
> • Gradle 9 (or the Gradle wrapper)  
> • Docker (optional)

### Clone and run locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew build
./gradlew bootRun
```

Open <http://localhost:8080> in a browser to start searching for tracks.

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

### Run tests

```bash
./gradlew test
```

### Code quality

```bash
./gradlew spotlessApply
./gradlew checkstyleMain
```

### Add a new source provider

1. Implement a new class that extends `TrackSource` (e.g., for Apple Music).  
2. Register it in `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork and branch off `main`.  
2. Write tests for any changes.  
3. Run `./gradlew check` locally – all tests and checks must pass.  
4. Submit a pull request.  
5. Keep commits small, focused, and use the conventional‑commit style.

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## License

HerEyes is released under the MIT License.  
See the [LICENSE](LICENSE) file.

---

## Changelog

| Version | Date       | Notes                               |
|---------|------------|-------------------------------------|
| **1.0.0** | 2026‑08‑29 | Initial release, Docker support     |
| 0.9.0   | 2026‑08‑06 | Added three EQ presets, optimized GC |
| 0.8.0   | 2026‑07‑22 | Refactored source selection logic   |

---

## Badges

![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)
![CI](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)
