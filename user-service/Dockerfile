FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR
COPY user-service/build/libs/user-service.jar ./app.jar

# Copy users.xml
COPY user-service/users.xml ./users.xml

ENV SECURITY_USERS_XML_PATH=./users.xml

ENTRYPOINT ["java", "-jar", "app.jar"]
