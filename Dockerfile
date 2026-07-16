FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Python, FFmpeg, and yt-dlp
RUN apt-get update && apt-get install -y python3 python3-pip ffmpeg wget && \
    wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Create temp directory for songs (writable on Render)
RUN mkdir -p /tmp/poweramp-songs && chmod 777 /tmp/poweramp-songs

COPY --from=build /app/build/libs/poweramp-spring.jar app.jar
COPY youtube-cookies.txt youtube-cookies.txt

# Render provides PORT env variable; Spring Boot reads SERVER_PORT or server.port
ENV SERVER_PORT=8085
EXPOSE 8085

# JVM tuning for Render free-tier (512MB RAM)
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-Djava.io.tmpdir=/tmp", "-jar", "app.jar"]
