FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR
COPY status-service/build/libs/status-service.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
