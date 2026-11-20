# The whole app in one image (docs/decisions.md ADR-034): the web app is built with Node.js,
# copied into the backend's static resources, and served by Spring Boot together with the API.
# Built by `docker compose up --build`; no tools other than Docker are needed.

# 1. Web app: type check and production build (frontend/dist)
FROM node:22.23.5-alpine3.24 AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2. Backend jar with the web app inside. Tests are not run here: the integration tests need
# Docker themselves (Testcontainers). Run them with `./mvnw clean verify` (tradeoffs.md T33).
FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /build
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
# A Windows checkout can give mvnw CRLF line endings (git core.autocrlf), and Linux then cannot
# run it ("./mvnw: not found"). Normalize them here, whatever the checkout did (review-log R8).
RUN sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties && chmod +x mvnw
# Download dependencies in their own layer, so code changes do not download them again.
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist src/main/resources/static
RUN ./mvnw -q -B package -DskipTests && cp target/world-event-alerts-*.jar app.jar

# 3. Runtime: Java runtime only, not the JDK or the build tools, and not running as root.
FROM eclipse-temurin:21.0.13_9-jre-noble
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER app
EXPOSE 8080
# The heap may use 75% of the container's memory limit (compose.yaml); the JVM's default is 25%.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
