# HerEyes — Audio Streaming Server

![Build](https://img.shields.io/github/actions/workflow/status/shubhyagami/streaming/ci.yml?branch=main&label=build)
![Docker Pulls](https://img.shields.io/docker/pulls/hereyes/streaming.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Issues](https://img.shields.io/github/issues/shubhyagami/streaming.svg)

HerEyes is a lightweight Spring Boot backend that streams audio from **YouTube** and **Spotify**. The companion vanilla-JavaScript front end is styled like a cassette player — spinning reel, sliding album art, and a real-time 32-band graphic equalizer. The current track, playback position, and EQ settings are stored in `localStorage`, so your session survives page reloads.

---

## Quick Start

**Prerequisites:** Java 26 or newer. The Gradle wrapper is included, so no separate Gradle install is needed.

```bash
git clone https://github.com/shubhyagami/streaming
cd streaming
./gradlew bootRun
```

Open <http://localhost:8080> in your browser.

Or run the pre-built Docker image:

```bash
docker run -p 8080:8080 hereyes/streaming
```

To build the image yourself:

```bash
docker build -t hereyes/streaming .
```

---

## Features

- **Multi-source playback** — stream from YouTube or Spotify, with automatic fallback to YouTube if a Spotify stream fails.
- **32-band graphic equalizer** — 25 preset profiles plus manual sliders.
- **Cassette-style UI** — spinning reel, sliding album art, cinematic progress bar.
- **Session persistence** — track, position, and EQ settings are restored after a reload.
- **REST API** — exposes track metadata, the audio stream, and EQ presets.
- **Dockerized** — ships with a ready-to-use Dockerfile.

---

## Architecture

| Layer | Technology |
|-------|------------|
| Backend | Java 26 / Spring Boot 3, Gradle 9 |
| Front end | Vanilla JS + Web Audio API |
| CI | GitHub Actions |
| Container | Docker |

The backend exposes a REST API that the front end consumes. The UI is plain JavaScript, using the Web Audio API for real-time equalization.

---

## Usage

1. **Search** — type a query into the search bar.
2. **Play** — click a result; playback starts automatically and the album art slides in.
3. **Adjust the EQ** — pick a preset or drag individual sliders.
4. **Reload** — the last track and EQ settings are restored automatically.

---

## Development

### Run tests

```bash
./gradlew test
```

### Format and lint

```bash
./gradlew spotlessApply     # apply code formatting
./gradlew checkstyleMain    # run static-analysis checks
```

### Add a new `TrackSource`

1. Create a class that implements `TrackSource`.
2. Register it in `SourceConfig`.
3. Run the test suite to verify integration.

---

## Contributing

1. Fork the repository and create a feature branch off `main`.
2. Add unit tests for your changes.
3. Run `./gradlew check` locally to make sure all checks pass.
4. Open a pull request following [Conventional Commits](https://www.conventionalcommits.org/).

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## License

Released under the [MIT License](LICENSE).

---

## Changelog

| Version | Date       | Highlights                              |
|---------|------------|-----------------------------------------|
| 1.0.0   | 2026-08-29 | Initial release with Docker support     |
| 0.9.0   | 2026-08-06 | Added three EQ presets, optimized GC    |
