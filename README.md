# HerEyes – Audio Streaming Server

**HerEyes** is a lightweight Spring Boot backend that streams audio from YouTube and Spotify, paired with a vanilla‑JavaScript front‑end that mimics a cassette player.  The player features a spinning reel, sliding album art, and a real‑time 32‑band graphic equalizer.  All state (track, position, EQ) is stored in `localStorage`, so a session survives page refreshes.

> **TL;DR**  
> ```bash
> git clone https://github.com/shubhyagami/streaming
> cd streaming
> ./gradlew bootRun
> # or
> docker run -p 8080:8080 hereyes/streaming
> ```
> Open <http://localhost:8080>.

---

## Table of Contents
1. [Features](#features)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
   - [Local Development](#local-development)
   - [Docker](#docker)
4. [Usage](#usage)
5. [Development](#development)
   - [Tests](#tests)
   - [Formatting & Linting](#formatting--linting)
   - [Adding a New TrackSource](#adding-a-new-tracksource)
6. [Contributing](#contributing)
7. [License](#license)
8. [Changelog](#changelog)

---

## Features

- **Multi‑source streaming** – play tracks from YouTube or Spotify; falls back to YouTube when a Spotify request fails.
- **Live 32‑band graphic equalizer** – 25 preset profiles plus manual sliders.
- **Cassette‑style UI** – spinning reel, sliding album art, cinematic progress bar.
- **Persisted state** – `localStorage` keeps track, position, and EQ settings across reloads.

---

## Architecture

| Layer       | Technology |
|-------------|-----------|
| Backend     | Java 26 / Spring Boot 3 (Gradle 9) |
| Front‑end   | Vanilla JS + Web Audio API |
| CI/CD       | GitHub Actions |
| Container   | Docker |
| Version control | Git |

The backend exposes a REST API to retrieve track metadata, stream audio, and serve EQ presets.  The front‑end consumes this API and drives the UI using the Web Audio API.

---

## Getting Started

### Local Development

```bash
git clone https://github.com/shubhyagami/streaming
cd streaming
./gradlew bootRun   # uses the Gradle wrapper
```

Open <http://localhost:8080> – you should see the cassette‑style player.  
The project uses Java 26 (or newer) and Gradle 9; the README assumes the Gradle wrapper is present.

### Docker

```bash
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.  
2. **Play** – click a result; playback starts automatically and the album art slides in.  
3. **Adjust EQ** – choose a preset or drag the sliders.  
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

![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Stars](https://img.shields.io/github/stars/shubhyagami/streaming.svg?style=social)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)
