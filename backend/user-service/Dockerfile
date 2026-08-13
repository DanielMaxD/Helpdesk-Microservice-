# syntax=docker/dockerfile:1

# ---- Build stage ----------------------------------------------------------
# Uses the project's own Maven Wrapper (pinned to Maven 3.9.9 via
# .mvn/wrapper/maven-wrapper.properties) so the image builds with the exact
# same Maven version developers use locally.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy wrapper + POM first so dependency resolution is cached in its own
# Docker layer and only re-runs when pom.xml actually changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is required for the container HEALTHCHECK below (hits actuator/health).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=10 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
