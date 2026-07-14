FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/poweramp-spring.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
