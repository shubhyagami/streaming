FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# Create temp directory for songs (writable on Render)
RUN mkdir -p /tmp/poweramp-songs && chmod 777 /tmp/poweramp-songs

COPY --from=build /app/build/libs/poweramp-spring.jar app.jar

# Render provides PORT env variable; Spring Boot reads SERVER_PORT or server.port
ENV SERVER_PORT=8085
EXPOSE 8085

# JVM tuning for Render free-tier (512MB RAM)
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-Djava.io.tmpdir=/tmp", "-jar", "app.jar"]
