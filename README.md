[K[2m  [2mmodel z-ai/glm-5.3-flash failed, trying next...[0m[0m
[K[2m  [2mmodel deepseek-ai/deepseek-v4.1-flash failed, trying next...[0m[0m
# HerEyes – Audio Streaming Server

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**. Its companion vanilla‑JavaScript front‑end looks and feels like a cassette player, complete with a spinning reel, sliding album art, and a real‑time 32‑band graphic equalizer. All state (current track, playback position, EQ settings) is persisted in `localStorage`, so the session survives page reloads.

---

## Quick Start

```bash
git clone https://github.com/shubhyagami/streaming
cd streaming
./gradlew bootRun    # Java 26 required
```

Open <http://localhost:8080> to see the player.  
Or run the pre‑built Docker image:

```
docker run -p 8080:8080 hereyes/streaming
```

---

## Features

- **Multi‑source playback** – play tracks from YouTube or Spotify; automatically falls back to YouTube if a Spotify stream fails.  
- **Live equalizer** – 32‑band graphic EQ with 25 preset profiles plus manual sliders.  
- **Cassette‑style UI** – spinning reel, sliding album art, cinematic progress bar.  
- **Session persistence** – `localStorage` keeps track, position, and EQ settings across page reloads.  
- **REST API** – exposes track metadata, audio stream, and EQ presets.  
- **Dockerized** – ready to deploy via Docker.

---

## Architecture

| Layer | Technology |
|-------|------------|
| Backend | Java 26 / Spring Boot 3, Gradle 9 |
| Front‑end | Vanilla JS + Web Audio API |
| CI | GitHub Actions |
| Container | Docker |
| VCS | Git |

The backend offers a REST API that the front‑end consumes. The UI is built with plain JavaScript, leveraging the Web Audio API for real‑time equalisation.

---

## Getting Started

### Local Development

```bash
git clone https://github.com/shubhyagami/streaming
cd streaming
./gradlew bootRun
```

Open <http://localhost:8080> in your browser. The project uses Java 26 (or newer) and Gradle 9; the Gradle wrapper is included.

### Docker

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – Type a query into the search bar.  
2. **Play** – Click a result; playback starts automatically and the album art slides in.  
3. **Adjust EQ** – Select a preset or drag individual sliders.  
4. **Persist** – Reload the page; the last track and EQ settings are restored automatically.

---

## Development

### Run Tests

```bash
./gradlew test
```

### Format & Lint

```bash
./gradlew spotlessApply     # Apply code formatting
./gradlew checkstyleMain    # Run static‑analysis checks
```

### Add a New `TrackSource`

1. Create a class that implements `TrackSource`.  
2. Register it in `SourceConfig`.  
3. Run the test suite to ensure integration.

---

## Contributing

1. Fork the repository and create a feature branch off `main`.  
2. Add unit tests for your changes.  
3. Run `./gradlew check` locally to ensure all checks pass.  
4. Submit a pull request and follow the [conventional‑commit] style.

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## License

HerEyes is released under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Changelog

| Version | Date       | Highlights |
|---------|------------|------------|
| 1.0.0   | 2026‑08‑29 | Initial release with Docker support |
| 0.9.0   | 2026‑08‑06 | Added three EQ presets, optimized GC |

---

## Badges

![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)  
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)  
![License](https://img.shields.io/badge/license-MIT-blue.svg)  
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)  
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
