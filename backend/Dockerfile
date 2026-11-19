# ---- Build stage ----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --shell /usr/sbin/nologin fitgear \
    # Render mounts secret files (e.g. the Aiven CA truststore) readable by
    # group 1000; our app user must be in that group to read them at runtime.
    && usermod -a -G 1000 fitgear

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER fitgear

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=mysql

# Render injects PORT (default 10000); Spring binds to it via server.port.
# No secrets are baked into this image - all config arrives as env vars or
# secret files at runtime.
HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=6 \
  CMD sh -c 'curl -fsS "http://localhost:${PORT:-8080}/actuator/health" || exit 1'

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
