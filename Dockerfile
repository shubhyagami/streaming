FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install yt-dlp, ffmpeg, and python3 (required by yt-dlp)
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 ffmpeg curl && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/poweramp-spring.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
