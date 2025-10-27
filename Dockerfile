# Multi-stage build for Memorix
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# Copy pom files first (for better caching)
COPY pom.xml .
COPY memorix-core/pom.xml memorix-core/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl memorix-core

# Copy source code
COPY memorix-core/src memorix-core/src

# Build application (skip tests for Docker build - tests run in CI)
RUN mvn clean package -pl memorix-core -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy executable JAR from build stage (with -exec classifier)
COPY --from=build /build/memorix-core/target/memorix-core-*-exec.jar /app/memorix.jar

# Copy application config (secrets loaded from environment)
COPY memorix-core/src/main/resources/application.yml /app/config/
COPY memorix-core/src/main/resources/application-docker.yml /app/config/

# Create volume for logs
VOLUME /app/logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application with Docker profile
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/memorix.jar --spring.config.location=/app/config/"]

