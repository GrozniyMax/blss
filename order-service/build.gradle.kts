plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Order Service - Order Management"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.liquibase:liquibase-core")

    // WebClient for service-to-service communication
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JMS for async messaging
    implementation("org.springframework.boot:spring-boot-starter-artemis")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Jakarta
    implementation("jakarta.annotation:jakarta.annotation-api")
    compileOnly("jakarta.transaction:jakarta.transaction-api:2.0.1")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("order-service.jar")
}
