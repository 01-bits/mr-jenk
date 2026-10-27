FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ls /app -la
RUN ./gradlew --no-daemon dependencies
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app -H && apk update && apk add curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app

EXPOSE 8761
ENTRYPOINT [ "java", "-jar", "app.jar" ]