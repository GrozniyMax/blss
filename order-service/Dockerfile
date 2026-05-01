# Build stage - build JAR using Gradle
FROM gradle:8.5-jdk17 AS builder

WORKDIR /home/gradle/project

# Copy gradle configuration files
COPY --chown=gradle:gradle ../build.gradle.kts ../settings.gradle.kts ./
COPY --chown=gradle:gradle ../gradle ./gradle
COPY --chown=gradle:gradle ../gradlew ./

# Copy all service directories
COPY --chown=gradle:gradle ../user-service ./user-service
COPY --chown=gradle:gradle ../order-service ./order-service
COPY --chown=gradle:gradle ../status-service ./status-service
COPY --chown=gradle:gradle ../src ./src

# Build the JAR for order-service
RUN ./gradlew :order-service:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /home/gradle/project/order-service/build/libs/order-service.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
