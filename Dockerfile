FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install yt-dlp (standalone binary, auto-detect architecture), ffmpeg
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg curl && \
    (curl -sL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux -o /usr/local/bin/yt-dlp || \
     curl -sL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64 -o /usr/local/bin/yt-dlp) && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    /usr/local/bin/yt-dlp --version && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/poweramp-spring.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
