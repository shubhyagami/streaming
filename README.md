# HerEyes: Memory Server of Ex

A retro-arcade-styled audio streaming server with YouTube and Spotify search, real-time Web Audio EQ, and a cassette-player dashboard.

## Overview

HerEyes is a high-performance audio streaming server that combines the nostalgia of retro gaming with the power of modern technology. With its sleek, interactive cassette-player dashboard and immersive gaming theme, users can easily discover, stream, and customize their music library.

## Key Features

* **Multi-Source Streaming:** Efficiently search and stream videos from YouTube and music from Spotify, with seamless fallback to YouTube when rate limiting occurs.
* **Real-Time EQ:** Access 25 built-in equalizer presets and a full 32-band graphic EQ, with real-time tone control and dynamic spectrum analysis for precise sound customization.
* **Cassette-Player Dashboard:** A highly interactive, HTML5-powered UI featuring a spinning cassette reel, album art display, and real-time playback controls.
* **Session Persistence:** Player state is preserved across page refreshes and navigation, ensuring seamless listening experiences.

## Getting Started

### Prerequisites

* **Java 26**
* **Gradle 9.0** (or use the included Gradle wrapper)
* **Docker** (optional, for containerized deployment)

### Run Locally

```bash
git clone https://github.com/shubhyagami/streaming.git
cd streaming
./gradlew bootRun
```

Access the web interface at `http://localhost:8080`.

## Contributing

We welcome contributions to the project. If you'd like to contribute, please submit a pull request or create an issue on the GitHub repository.

## License

HerEyes is licensed under the [MIT License](https://github.com/shubhyagami/streaming/blob/master/LICENSE).

## Changelog

### 2026-08-26

* Improved README structure and readability.
### 2026-08-06

* Added three new built-in EQ presets: "Midnight Rain", "Vaporwave Drift", and "Lo-Fi Café".
* Fine-tuned all 22 existing presets to align with ISO 1/3-octave center frequencies.
* Improved stream-and-delete garbage collector logging.

<p align="center">
  <i>In the end, all that matters is the music.</i><br>
</p>

<a href="https://github.com/shubhyagami/streaming"><img src="https://custom-badge-url.com/github-source" alt="Use the source"></a>
<a href="https://github.com/shubhyagami/streaming#license"><img src="https://custom-badge-url.com/license-mit" alt="License MIT"></a>
<a href="https://github.com/shubhyagami/streaming#contributing"><img src="https://custom-badge-url.com/contributions-welcome" alt="Contributions welcome"></a>
