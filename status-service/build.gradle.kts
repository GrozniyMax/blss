plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Status Service - Order Status Tracking and Notifications"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.liquibase:liquibase-core")

    // JMS for async messaging
    implementation("org.springframework.boot:spring-boot-starter-artemis")

    // WebClient for service-to-service communication
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson for JSON and XML
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Jakarta
    implementation("jakarta.annotation:jakarta.annotation-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("status-service.jar")
}
