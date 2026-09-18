# HerEyes – Audio Streaming Server

**HerEyes** is a lightweight Spring Boot backend that streams audio from YouTube and Spotify.  
A simple vanilla‑JavaScript front‑end emulates a cassette player with a spinning reel, sliding album art, and a 32‑band graphic equalizer.  
All player state (current track, position, EQ settings) is persisted in `localStorage`, so a session survives page refreshes.

---

## Features

- **Multi‑source streaming** – play tracks from YouTube or Spotify; falls back to YouTube if a Spotify request fails.  
- **Live 32‑band graphic equalizer** – 25 preset profiles plus manual sliders.  
- **Cassette‑style UI** – spinning reel, sliding album art, and cinematic track progress.  
- **Persistence** – `localStorage` keeps track, position, and EQ settings across reloads.  

---

## Architecture Overview

| Layer       | Technology |
|-------------|-------------|
| Backend     | Java 26 / Spring Boot 3  (Gradle 9) |
| Front‑end   | Vanilla JavaScript + Web Audio API |
| CI / CD     | GitHub Actions |
| Container   | Docker |
| Source Control | Git |

The backend exposes a REST API to fetch track metadata, stream audio, and handle EQ presets.  
The front‑end consumes the API and drives the UI with the Web Audio API.

---

## Getting Started

> **Prerequisites**  
> - Java 26 (or newer)  
> - Gradle (or the bundled Gradle wrapper)  
> - Optional: Docker

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
# Start the server
./gradlew bootRun   # uses the wrapper, no separate Gradle install needed
```

Open <http://localhost:8080> to see the player.  

Alternatively build and run the Docker image:

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; playback starts automatically and the album art slides in.  
3. **Adjust EQ** – select a preset or drag the sliders.  
4. **Persist** – refresh the page; the last track and EQ settings are restored automatically.

---

## Development

### Run Tests

```bash
./gradlew test
```

### Formatting & Linting

```bash
# Apply code formatting
./gradlew spotlessApply

# Run static‑analysis checks
./gradlew checkstyleMain
```

### Adding a New `TrackSource`

1. Create a class that implements `TrackSource`.  
2. Register it in `SourceConfig`.  
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repository and create a feature branch off `main`.  
2. Add unit tests for your changes.  
3. Run `./gradlew check` locally to ensure all checks pass.  
4. Open a pull request and follow the [conventional‑commit] style.

See the detailed guidelines in [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

HerEyes is released under the [MIT License](LICENSE).

---

## Changelog

| Version | Date       | Notes                                 |
|---------|------------|---------------------------------------|
| **1.0.0** | 2026‑08‑29 | Initial release with Docker support   |
| **0.9.0** | 2026‑08‑06 | Added three EQ presets, optimized GC |

---

## Badges

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)

---
