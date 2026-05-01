plugins {
    java
    `java-library`
}

description = "Bitrix24 JCA Connector - Jakarta Connector Architecture Resource Adapter"

dependencies {
    // Jakarta Connector API (JCA)
    implementation("jakarta.resource:jakarta.resource-api:2.1.0")

    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.assertj:assertj-core")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Disable bootJar for library module
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
