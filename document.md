# APK → Web Application: Complete Conversion Guide

> Use this document to convert any Android APK into a fully functional Spring Boot web application.  
> Paste the entire contents of the **Prompt Template** section into an AI coding assistant to execute the full conversion autonomously.

---

## Table of Contents

1. [Golden Prompt Template](#-golden-prompt-template) — Copy & paste this
2. [Architecture Blueprint](#-architecture-blueprint) — How it works
3. [Implementation Phases](#-implementation-phases) — What the prompt builds
4. [Decision Log](#-decision-log) — Why we chose what we did

---

## 🏆 Golden Prompt Template

Copy everything below this line and paste it into your AI. Replace words in `[[BRACKETS]]` with your specific requirements.

---

```
You are an expert full-stack engineer. Convert the Android APK `[[APK_NAME]].apk` into a fully functional Spring Boot web application from scratch. Build EVERYTHING — no placeholders, no TODOs, no "you can add later".

## TARGET APPLICATION
- Name: [[APP_NAME]]
- Source APK: [[APK_NAME]] (an Android [[CATEGORY]] app)
- Core features to replicate: [[LIST_3_TO_5_CORE_FEATURES]]
- UI theme: [[UI_THEME_DESCRIPTION — e.g. "retro arcade, dark with neon cyan/magenta/green/yellow accents, Press Start 2P font, CRT scanline overlay, pixel borders and glows"]]

## TECH STACK (MANDATORY — do NOT deviate)
- Backend: Spring Boot 3.4.4, Java 21+, Gradle with Kotlin DSL
- Frontend: Single Page Application (ALL sections in ONE HTML file), vanilla JavaScript, SVG, Canvas, CSS custom properties
- Database: H2 embedded (file-based, jdbc:h2:file:./data/[[app_name_lower]])
- Audio/Video processing: Pure Java — NO native .so files, NO CLI tools (no ffmpeg, no yt-dlp, no youtube-dl)
- Real-time audio EQ: Web Audio API (BiquadFilterNode chain), NOT server-side
- Streaming: YouTube via Innertube API (search + player endpoints, no API key needed), with RapidAPI fallback
- Persistence: sessionStorage for player state across page navigation

## PROJECT CONSTRAINTS
- NO Lombok
- NO Lombok annotations (@Data, @Slf4j, etc.)
- NO other APK resource extraction tools
- NO external CLI tools whatsoever
- All routes must redirect to "/" (single page) via WebConfig
- Every feature must be fully functional, not stubbed
- The app must build and run with: ./gradlew build --no-daemon -x test && java -jar build/libs/*.jar

## PROJECT STRUCTURE — Create exactly this tree:

```
[[APP_NAME]]/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── Dockerfile
├── .dockerignore
├── .gitignore
├── render.yaml
├── src/main/
│   ├── java/com/[[package]]/
│   │   ├── [[AppName]]Application.java
│   │   ├── config/
│   │   │   ├── WebConfig.java          — Maps ALL routes to "/"
│   │   │   ├── DataInitializer.java    — Seeds DB on startup
│   │   │   └── AudioConfig.java        — Audio pipeline beans
│   │   ├── controller/
│   │   │   ├── ApiController.java      — REST CRUD for presets
│   │   │   └── StreamController.java   — YouTube/Spotify streaming
│   │   ├── service/
│   │   │   ├── YouTubeService.java     — Innertube search + download
│   │   │   ├── SpotifyService.java     — Spotify search + RapidAPI fallback
│   │   │   ├── TempFileManager.java    — Temp file register/delete/cleanup
│   │   │   ├── EqPresetService.java    — Preset CRUD logic
│   │   │   └── AudioProcessingService.java — Offline processing jobs
│   │   ├── audio/
│   │   │   ├── AudioDecoder.java       — WAV 8/16/24-bit decoder
│   │   │   ├── AudioEncoder.java       — WAV encoder
│   │   │   └── AudioPipeline.java      — Chunked processing pipeline
│   │   ├── dsp/
│   │   │   ├── filter/
│   │   │   │   ├── BiquadFilter.java       — RBJ cookbook base
│   │   │   │   ├── FilterFactory.java      — Factory for 6 filter types
│   │   │   │   ├── LowPassFilter.java
│   │   │   │   ├── HighPassFilter.java
│   │   │   │   ├── BandPassFilter.java
│   │   │   │   ├── LowShelfFilter.java
│   │   │   │   ├── HighShelfFilter.java
│   │   │   │   └── PeakingFilter.java
│   │   │   ├── engine/
│   │   │   │   ├── DspProcessor.java       — Orchestrator
│   │   │   │   ├── GraphicEqualizer.java   — 32-band ISO centers
│   │   │   │   ├── ParametricEq.java       — Parametric bands
│   │   │   │   ├── ToneController.java     — Bass/Treble
│   │   │   │   ├── Limiter.java
│   │   │   │   ├── StereoProcessor.java
│   │   │   │   ├── TempoProcessor.java     — WSOLA
│   │   │   │   ├── ReverbProcessor.java    — Schroeder
│   │   │   │   └── FrequencyResponse.java  — Compute FRS
│   │   │   ├── model/
│   │   │   │   ├── EqBand.java
│   │   │   │   ├── EqMode.java (enum: GRAPHIC, PARAMETRIC)
│   │   │   │   ├── FilterType.java (enum)
│   │   │   │   ├── EqPreset.java (with PresetCategory enum)
│   │   │   │   ├── ToneState.java
│   │   │   │   ├── VolumeState.java
│   │   │   │   └── ReverbPreset.java
│   │   │   └── preset/
│   │   │       └── BuiltInPresets.java     — 22 presets
│   │   ├── repository/
│   │   │   ├── EqPresetEntity.java
│   │   │   ├── EqBandEntity.java
│   │   │   ├── EqPresetRepository.java
│   │   │   ├── ReverbPresetEntity.java
│   │   │   └── ReverbPresetRepository.java
│   │   └── web/dto/
│   │       ├── PresetDto.java
│   │       ├── BandDto.java
│   │       └── ProcessingRequestDto.java
│   └── resources/
│       ├── application.yml
│       ├── templates/
│       │   └── index.html              — THE ONLY PAGE (SPA)
│       └── static/
│           ├── css/poweramp.css
│           ├── js/poweramp.js
│           └── video/
│               └── background.mp4      — Fullscreen background video
├── background.mp4                      — Source file, copy to static/video/
```

## BUILD.GRADLE.KTS — Use these dependencies ONLY:
```kotlin
plugins {
    id("org.springframework.boot") version "3.4.4"
    java
}
java.sourceCompatibility = JavaVersion.VERSION_21

repositories { mavenCentral() }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.4"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.h2database:h2")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
}
```

## APPLICATION.YML — Use this configuration:
```yaml
server:
  port: 8085
spring:
  application:
    name: [[app_name_lower]]
  datasource:
    url: jdbc:h2:file:./data/[[app_name_lower]];AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: false
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
  thymeleaf:
    cache: false
    mode: HTML
    encoding: UTF-8
poweramp:
  audio:
    temp-output: /tmp/poweramp-output
  temp-dir: \${java.io.tmpdir}/poweramp-stream
  rapidapi:
    host: youtube-mp36.p.rapidapi.com
    spotify-host: spotify81.p.rapidapi.com
    key: [[YOUR_RAPIDAPI_KEY]]
  defaults:
    sample-rate: 44100
    bit-depth: 16
    channels: 2
logging:
  level:
    com.[[package]]: DEBUG
    org.springframework: WARN
```

## WEBCONFIG.JAVA — Redirect all routes to SPA:
- Map "/*" (excluding /api/**, /css/**, /js/**, /video/**, /h2-console/**) to "forward:/"
- This enables client-side routing via showSection()

## BACKEND REQUIREMENTS (implement ALL)

### DSP Core (BiquadFilter.java)
- Implement 6 filter types using RBJ cookbook formulas: LowPass, HighPass, BandPass, LowShelf, HighShelf, Peaking
- Each extends BiquadFilter with calculateCoefficients()
- GraphicEqualizer: 32 bands at ISO 1/3-octave centers (20Hz to 20kHz)
- ParametricEq: N bands with configurable frequency, Q, gain
- ToneController: bass @ 250Hz ±12dB, treble @ 4kHz ±12dB
- Limiter: threshold, ratio, attack, release, makeup gain
- StereoProcessor: balance, stereo width
- TempoProcessor: WSOLA time-stretching
- ReverbProcessor: Schroeder reverberator (damp, filter, fade, pre-delay, size, mix)
- BuiltInPresets: 22 presets (Normal, Classic, Pop, Rock, Jazz, Dance, R&B, HipHop, Metal, Acoustic, Vocal Boost, Vocal Reduction, Bass Boost, Bass Reduction, Treble Boost, Treble Reduction, Full Bass, Full Treble, Headphones, Laptop, Small Speakers, Custom)

### Data Layer
- EqPresetEntity: id, name, mode (GRAPHIC/PARAMETRIC), category (BUILT_IN/USER/AUTO_EQ), createdAt
- EqBandEntity: id, preset (ManyToOne), frequency, gain, q, filterType
- ReverbPresetEntity: id, name, damp, filter, fade, preDelay, size, mix
- Seed 22 built-in presets + 5 reverb presets on startup via DataInitializer

### REST API
- GET /api/presets — list all presets (with ?search=&category= filters)
- POST /api/presets — create preset with bands
- GET /api/presets/{id} — get preset detail
- PUT /api/presets/{id} — update preset
- DELETE /api/presets/{id} — delete preset
- POST /api/frs — compute frequency response from band array
- GET /api/processing/jobs — list async processing jobs

### YouTube Streaming
- Search: POST to youtubei/v1/search?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8
- Use client: WEB (version 2.20240201.08.00) with browser headers
- Parse twoColumnSearchResultsRenderer → sectionListRenderer → videoRenderer
- Extract: videoId, title, channel, duration, thumbnail
- Download: POST to youtubei/v1/player with clients in order:
  1. WEB_EMBEDDED_PLAYER (version 1.0)
  2. ANDROID_MUSIC (version 6.27.51)
  3. ANDROID (version 19.09.37)
  4. WEB (version 2.20240201.08.00)
- Add browser headers: User-Agent, Accept, Accept-Language, Origin, Referer, Cookie (CONSENT=YES+cb), sec-ch-ua, Sec-Fetch-*
- Parse streamingData → adaptiveFormats → pick highest bitrate audio (opus > m4a)
- If URL has signatureCipher, parse & reconstruct URL
- Download audio to temp dir, register with TempFileManager
- Fallback to RapidAPI (youtube-mp36) if all Innertube clients fail

### Spotify Fallback
- Search via RapidAPI spotify81.p.rapidapi.com/search
- Download via spotify81.p.rapidapi.com/download_track_sc
- If RapidAPI returns {} (rate limited), fallback: search via SpotifyService.search() → return results with videoId → StreamController.startSpotifyStream calls ytService.downloadAudio(videoId)
- SpotifyService.search(): call RapidAPI search, parse tracks array, return SpotifyResult(title, artist, videoId, audioUrl, size)

### Temp File Management
- TempFileManager.register(path, title) — creates token (UUID), stores TempEntry(token, path, title, createdAt)
- TempFileManager.get(token) — returns TempEntry
- TempFileManager.delete(token) — deletes file + removes entry
- @Scheduled(fixedRate = 60000) — deletes entries older than 10 minutes
- TempEntry: record with token, path, title, createdAt

### Stream Endpoints
- POST /api/yt/stream?videoId=&title= — download audio async (CompletableFuture, 120s timeout), return {token, streamUrl, title, contentType}
- GET /api/yt/stream/{token} — serve file as Resource (FileSystemResource), Content-Type based on extension (.m4a→audio/mp4, .mp3→audio/mpeg, .webm→audio/webm, .opus→audio/ogg)
- POST /api/yt/stop/{token} — delete temp file
- GET /api/yt/stream/{token}/status — check if file still exists

## FRONTEND REQUIREMENTS — index.html (SINGLE PAGE)

### HTML Structure
- Single <!DOCTYPE html> with ONE file
- <div id="app"> wraps everything
- Header nav: buttons calling showSection('dashboard'|'equalizer'|'player'|'presets')
- Content div with page-section divs for each section (only ONE has class "active" at a time)
- Audio element: <audio id="audio-player" style="display:none">
- Now-playing bar: <div id="now-playing"> with title, sinewave seekbar canvas, play/pause/stop buttons
- Status bar: <div id="status-bar"> with DSP status text
- All sections must use show/hide via JS class toggle — NEVER reload the page

### Section: Dashboard
- Header with app name and subtitle
- SVG cassette player (viewBox="0 0 340 210"): realistic tape shell, 4 screws, beige/orange label, 2 animated reels (CSS @keyframes reelSpin), album art clipped into label via SVG <image> with xlink:href, tape head/guide holes at bottom
- Cassette wrapper: width 100%, max-width 960px, centered
- Time/progress row below cassette: current time, progress track, duration
- Stats grid: preset count, processed files, DSP info
- Quick presets section: pill buttons for 8 favorite presets

### Section: Equalizer
- Spectrum analyzer: <canvas> with AnalyserNode (fftSize=256), animated HSL bars with random color cycling every ~30 frames, labeled "LED Spectrum Analyzer"
- 31 vertical EQ sliders in horizontal scrollable row (flex-direction: row, overflow-x: auto, each input[type=range] uses writing-mode: vertical-lr; direction: rtl)
- EQ controls row: toggle buttons (Equ checkbox, Limiter checkbox), preset selector <select>, menu button (autosave, save as, reset, export, import)
- No Tone panel in EQ section (tone moved to Volume tab)

### Section: Player (Search & Play)
- Source toggle: YouTube / Spotify radio buttons (switchSource() changes placeholder text and delegates to doSearch())
- Search bar: input + search button, results container
- Search results: each shows thumbnail (or SVG placeholder), title, channel/artist, duration, play button
- Loading spinner during download
- Error messages shown in search-status div with actual server error text

### Section: Presets
- Search bar + category filter (ALL/BUILT_IN/USER/AUTO_EQ)
- Action buttons: New, Export, Import (file input hidden)
- Preset list with click-to-expand detail
- Detail view: preset name, band values, Apply/Delete buttons
- Dialog for creating new preset (name + mode selector)

### Volume Tab (inside Equalizer section tab panel)
- Grid of 6 volume sections: Balance, Stereo FX, Tempo, Volume, Bass, Treble
- Each section: h3 label, slider, value labels
- Bass/Treble sliders: range -12 to +12, linked to Web Audio lowshelf/highshelf filters
- Checkboxes row: Tone toggle (calls toggleEqProcessing()), Mono, Platform FX, Reset All button

### Reverb Tab (inside Equalizer section tab panel)
- Reverb enable toggle
- 6 sliders: Damp, Filter, Fade, Pre-Delay, Size, Mix
- Preset buttons: Auditorium, Hall, Room, Echo, Studio

### Web Audio API (poweramp.js)
- AudioContext created lazily on first play
- MediaElementAudioSourceNode from <audio> element
- Chain: source → 31 BiquadFilter peaking nodes (20Hz–20kHz ISO centers) → destination
- Separate lowshelf (250Hz) and highshelf (4kHz) BiquadFilter nodes for tone
- Each slider change calls updateWebEq() which sets filter.gain.value in real-time
- AnalyserNode connected in parallel (not in chain) for spectrum display
- initWebAudio(): create context, connect nodes
- reconnectWebAudio(): disconnect old source, connect new source through chain (called when audio.src changes)
- initToneWeb(): create lowshelf/highshelf filters
- updateToneWeb(): set gain.value based on bass-slider/treble-slider, multiplied by tone-enabled checkbox (0 or 1)
- initSpectrumAnalyzer(): create AnalyserNode, start drawSpectrum() loop via requestAnimationFrame
- drawSpectrum(): getByteFrequencyData, draw HSL bars on canvas with random hue rotation

### Sinewave Seekbar (Canvas)
- Draw animated sinewave on #wave-canvas using requestAnimationFrame
- On click: seekTo(event) calculates percentage from click position, sets audio.currentTime
- Two hues cycling for waveform

### Cassette Player (SVG)
- updateCassette(): reads audio state, sets .spinning class on reel groups, updates album art image href via setAttributeNS('http://www.w3.org/1999/xlink', 'href', thumbnail), updates time text and progress bar width
- showNowPlaying(title, thumbnail): sets np-title text, calls updateCassette()
- Player state saved to sessionStorage (token, position, title, thumbnail, timestamp)
- restorePlayerState() on page load: resume from saved position

### CSS Theme (poweramp.css)
- Google Fonts: Press Start 2P
- CSS custom properties for all colors/heights
- --neon-cyan: #00f0ff, --neon-magenta: #ff00ff, --neon-green: #00ff41, --neon-yellow: #fff000, --neon-red: #ff003c
- Dark backgrounds: --bg-primary: #0a0a0a, --bg-card: rgba(17,17,17,0.82)
- CRT scanline overlay: body::after with repeating-linear-gradient
- Background video: #bg-video fixed fullscreen z-index -2, #bg-overlay rgba(0,0,0,0.55) z-index -1
- Pixel-art borders with box-shadow, neon text-shadows, uppercase text
- All cards, buttons, panels use transparent RGBA backgrounds so video shows through
- Cassette: drop-shadow, reel @keyframes reelSpin, progress bar linear-gradient
- Spectrum canvas: styled border, glow
- Vol-grid: 2-column grid for volume sections
- Toggle buttons (.dsp-toggle): when input:checked, green border + glow

## JAVASCRIPT — poweramp.js REQUIREMENTS

### Navigation & Section Switching
```javascript
function showSection(name) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    document.getElementById('section-' + name).classList.add('active');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.querySelector('[data-section="' + name + '"]')?.classList.add('active');
    savePlayerState(); // persist across sections
}
```

### Equalizer Sliders
- renderSliders(): create 31 input[type=range] elements in #eq-sliders with ISO frequencies
- loadPreset(preset): fetch /api/presets/{id}, set slider values, updateWebEq()
- resetEq(): set all sliders to 0, clear tone-enabled, updateWebEq()

### YouTube Search & Play
- searchYouTube(query): fetch /api/yt/search?q=..., pass results to renderYTResults()
- renderYTResults(data): create result divs with thumbnail, title, channel, duration, play button calling playVideo(videoId, title, thumbnail)
- playVideo(videoId, title, thumbnail): POST /api/yt/stream?videoId=...&title=..., on success set audio.src = data.streamUrl, call audio.play(), initWebAudio(), etc.

### Spotify Search & Play
- searchSpotify(query): fetch /api/spotify/search?q=..., pass to renderSpotifyResults()
- renderSpotifyResults(data): create result divs with SVG icon (no thumbnail), title, artist, size, play button calling playSpotify(audioUrl, videoId, title)
- playSpotify(audioUrl, videoId, title): POST /api/spotify/stream? with params, same flow as playVideo

### Source Toggle
- switchSource(source): update active class on radio buttons, change placeholder text, call doSearch() if query exists

### State Persistence
- savePlayerState(): write {token, currentTime, title, thumbnail, timestamp} to sessionStorage
- restorePlayerState(): read from sessionStorage, if timestamp < 10 min old, resume playback

### Error Handling
- All fetch calls: if (!r.ok) { const errBody = await r.json().catch(() => ({})); throw new Error(errBody.error || 'Failed'); }
- Display errors in #search-status with red status-msg

## DOCKERFILE
```dockerfile
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build --no-daemon -x test
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/[[jarname]].jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## RENDER.YAML
```yaml
services:
  - type: web
    name: [[app_name_lower]]
    env: docker
    repo: https://github.com/[[your_github]]/[[repo_name]]
    branch: main
    dockerfilePath: ./Dockerfile
    healthCheckPath: /
    envVars:
      - key: SERVER_PORT
        value: 8085
      - key: SPRING_DATASOURCE_URL
        value: jdbc:h2:file:./data/[[app_name_lower]];AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE
      - key: SPRING_JPA_HIBERNATE_DDL_AUTO
        value: update
```

## VERIFICATION
After writing ALL files, run:
```bash
./gradlew build --no-daemon -x test
java -jar build/libs/*.jar
```
The app MUST start on port 8085 and be fully functional. Fix any compilation errors.

## FINAL NOTES
- Do NOT skip any file. Create EVERY file listed above.
- Do NOT use Lombok. Write all getters/setters/constructors manually or use records.
- The app is a SINGLE PAGE — ALL UI is in index.html. No other templates.
- Audio must NOT stop when switching sections.
- All CSS must be in poweramp.css. All JS must be in poweramp.js.
- The background video file background.mp4 must be placed at src/main/resources/static/video/background.mp4 (copy from project root).
```

---

## 🧠 Architecture Blueprint

### High-Level Flow

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│ Browser  │────▶│  Spring Boot │────▶│  YouTube/    │
│ (SPA)    │     │  (Port 8085) │     │  Spotify API │
└────┬─────┘     └──────┬───────┘     └──────────────┘
     │                  │
     │  Web Audio API   │  H2 Database
     │  BiquadFilter    │  (Presets, Config)
     │  Chain           │
     └──────────────────┘
```

### Key Technologies Mapped to APK Features

| APK Feature | Web Equivalent | Location |
|---|---|---|
| 32-band Graphic EQ | Web Audio API BiquadFilterNode × 31 | `poweramp.js` → `initWebAudio()` |
| Bass/Treble Tone | LowShelfFilter (250Hz) + HighShelfFilter (4kHz) | `poweramp.js` → `initToneWeb()` |
| Presets | H2 database + REST CRUD + seed data | `EqPresetService.java`, `BuiltInPresets.java` |
| Audio Playback | `<audio>` element | `index.html` |
| Spectrum Analyzer | AnalyserNode + Canvas | `poweramp.js` → `drawSpectrum()` |
| Song Search | Innertube API (YouTube) + RapidAPI (Spotify) | `YouTubeService.java`, `SpotifyService.java` |
| File Processing | WAV Decoder/Encoder + Pipeline | `AudioDecoder.java`, `AudioPipeline.java` |
| DSP Effects | Pure Java: Limiter, Reverb, Stereo, Tempo | `dsp/engine/` |
| Settings/UI | sessionStorage + CSS custom properties | `poweramp.js`, `poweramp.css` |

---

## 🏗️ Implementation Phases

### Phase 1: Project Scaffolding
- Generate build.gradle.kts with all dependencies
- Create application.yml with H2 config
- Create WebConfig.java (SPA routing)
- Create Dockerfile, render.yaml, .gitignore, .dockerignore
- Create PowerampApplication.java (main class)

### Phase 2: DSP Core (Java)
- Implement BiquadFilter.java with 6 RBJ cookbook filter types
- Build DSP engines: GraphicEqualizer, ParametricEq, ToneController, Limiter, StereoProcessor, TempoProcessor, ReverbProcessor, FrequencyResponse
- Create DspProcessor orchestration class
- Built-in presets (22 presets with 31 bands each)

### Phase 3: Data Layer
- JPA entities: EqPresetEntity, EqBandEntity, ReverbPresetEntity
- Repositories
- DataInitializer (seeds presets + reverb presets)
- DTOs and EqPresetService

### Phase 4: REST API
- ApiController: preset CRUD, FRS, processing jobs
- StreamController: YouTube + Spotify streaming + temp file serving

### Phase 5: External Services
- YouTubeService: Innertube search + player API with multi-client fallback
- SpotifyService: RapidAPI search + download with YouTube fallback
- TempFileManager: register/delete/scheduled cleanup (60s interval, 10min TTL)

### Phase 6: Frontend SPA
- index.html: all 4 sections (dashboard, equalizer, player, presets) + now-playing bar + audio element
- poweramp.css: retro arcade theme, cassette, spectrum, EQ sliders, volume grid, background video
- poweramp.js: SPA nav, Web Audio EQ, spectrum analyzer, YouTube/Spotify search, cassette updates, sessionStorage persistence

### Phase 7: Deployment
- Dockerfile: multi-stage build (gradle:8-jdk21 → eclipse-temurin:21-jre)
- render.yaml: blueprint for Render deployment
- Commit + push to GitHub → connect to Render

---

## 📋 Decision Log

| Decision | Choice | Rationale |
|---|---|---|
| Frontend architecture | Single Page Application | Audio must persist across pages; SPA never reloads the page or the `<audio>` element |
| Real-time EQ | Web Audio API (client-side) | Server-side DSP for streaming audio would require re-encoding and buffering — adds latency and complexity |
| EQ slider direction | Horizontal row, each slider vertical | Maximizes visible bands at once; vertical `writing-mode` range inputs are the only CSS-native way |
| Cassette player | Inline SVG with CSS animation | Full control over appearance, no external assets; reels animate via CSS transforms on SVG `<g>` |
| YouTube download | Innertube player API (direct) | No API key needed; avoids dependency on third-party download services; multi-client fallback handles bot detection |
| Database | H2 file-based | Zero setup, embedded, sufficient for preset storage; easily swapped for PostgreSQL on Render |
| No Lombok | Manual code | Avoids annotation processor issues across IDE/build environments; records used where mutable state isn't needed |
| No native libraries | Pure Java DSP | APK's native .so files are ARM binaries; can't run on server JVM; reimplement in pure Java |
| Background video | Local MP4 with overlay | Creates visual impact without external dependencies; dark overlay ensures text readability |
| Temp file cleanup | @Scheduled 60s, TTL 10min | Prevents disk fill-up; 10min window allows scrubbing/seek without re-downloading |
| Source toggle | YouTube / Spotify radio buttons | Users can choose source; Spotify falls back to YouTube when rate-limited |

---

## 🔧 Troubleshooting Common Issues

### "Download failed: Video not playable"
- The Innertube client is being blocked. Ensure WEB_EMBEDDED_PLAYER is the FIRST client tried
- Verify CONSENT cookie is set on all YouTube requests
- Try updating client versions (YouTube occasionally deprecates old versions)
- Check if Render IP is blocked — try adding more client fallbacks

### "No streaming data available"
- The video might be age-restricted or private
- Try a different video
- Add cookies from a real logged-in session if needed

### Audio doesn't play in browser
- Check content-type mapping in StreamController.getContentType()
- .opus files need audio/ogg mime type
- Some browsers don't support .m4a (AAC) — test in Chrome

### Build fails
- Verify Java 21+ is installed
- Check Gradle version compatibility
- Ensure no Lombok annotations are present
- Verify all imports resolve correctly

### H2 database resets on restart with "create-drop"
- Change spring.jpa.hibernate.ddl-auto to "update" (not "create-drop")
- Use jdbc:h2:file: (not mem:) in datasource URL

---

*Generated from the Poweramp → HerEyes :Memory Server of Ex conversion project.*
