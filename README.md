[K[2m  [2mmodel z-ai/glm-5.3-flash failed, trying next...[0m[0m
[K[2m  [2mmodel deepseek-ai/deepseek-v4.1-flash failed, trying next...[0m[0m
# HerEyes — Audio Streaming Server

![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Open Issues](https://img.shields.io/github/issues/shubhyagami/streaming)

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**.  
Its companion vanilla‑JS front‑end mimics a cassette player, featuring a spinning reel, sliding album art, a cinematic progress bar, and a real‑time 32‑band graphic equalizer. The current track, playback position, and EQ settings are persisted in `localStorage` so a page reload restores the session.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally](#run-locally)
  - [Docker](#docker)
- [Usage](#usage)
- [Architecture](#architecture)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)
- [Changelog](#changelog)

---

## Quick Start

```bash
git clone https://github.com/shubhyagami/streaming
cd streaming
./gradlew bootRun
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

---

## Features

- **Multi‑source playback** – stream from YouTube or Spotify; falls back to YouTube if Spotify fails.
- **32‑band graphic equalizer** – 25 preset profiles plus manual slider control.
- **Cassette‑style UI** – spinning reel, sliding album art, cinematic progress bar.
- **Session persistence** – track, position, and EQ settings survive page reloads.
- **REST API** – exposes track metadata, audio stream, and EQ presets.
- **Docker‑ready** – comes with a `Dockerfile`.

---

## Getting Started

### Prerequisites

- Java 26 or newer
- Gradle 9 (the Gradle wrapper is included)
- Docker (optional)

### Run Locally

```bash
./gradlew bootRun
```

The application starts on `localhost:8080`.

### Docker

```bash
# Run the pre‑built image
docker run -p 8080:8080 hereyes/streaming

# Build your own image
docker build -t hereyes/streaming .
docker run -p 8080:8080 hereyes/streaming
```

---

## Usage

1. **Search** – type a query into the search bar.
2. **Play** – click a result; playback starts automatically and the album art slides in.
3. **Adjust the EQ** – choose a preset or drag individual sliders.
4. **Reload** – the last track and EQ settings are restored automatically.

---

## Architecture

| Layer        | Technology |
|--------------|------------|
| Backend      | Java 26 / Spring Boot 3, Gradle 9 |
| Front end    | Vanilla JS + Web Audio API |
| CI/CD        | GitHub Actions |
| Container    | Docker |

The backend provides a REST API that the front‑end consumes. All equalization is performed client‑side with the Web Audio API.

---

## Development

### Run tests

```bash
./gradlew test
```

### Code style

```bash
./gradlew spotlessApply    # Apply formatting
./gradlew checkstyleMain   # Static‑analysis checks
```

### Adding a new `TrackSource`

1. Create a class that implements `TrackSource`.
2. Register it in `SourceConfig`.
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repository and create a feature branch off `main`.
2. Write unit tests for your changes.
3. Run `./gradlew check` locally to ensure all checks pass.
4. Open a pull request; follow the [Conventional Commits](https://www.conventionalcommits.org/) convention.

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Changelog

| Version | Date       | Highlights                            |
|---------|------------|----------------------------------------|
| 1.0.0   | 2026‑08‑29 | Initial release with Docker support     |
| 0.9.0   | 2026‑08‑06 | Added three EQ presets, optimized GC    |

---
