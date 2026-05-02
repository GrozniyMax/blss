plugins {
    java
    war
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Order Service - Order Management"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.liquibase:liquibase-core")

    // JMS for async messaging
    implementation("org.springframework.boot:spring-boot-starter-artemis")

    // WebClient for user-service communication
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Jakarta
    implementation("jakarta.annotation:jakarta.annotation-api")
    compileOnly("jakarta.transaction:jakarta.transaction-api:2.0.1")

    // JCA (Jakarta Connector Architecture)
    implementation("jakarta.resource:jakarta.resource-api:2.1.0")

    // Bitrix24 JCA Connector
    implementation(project(":bitrix-jca-connector"))

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootWar>("bootWar") {
    enabled = true
    archiveFileName.set("order-service.war")
}
