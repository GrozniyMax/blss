FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR
COPY order-service/build/libs/order-service.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
