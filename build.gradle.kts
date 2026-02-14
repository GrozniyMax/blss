import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.blss"
version = "0.0.1-SNAPSHOT"
description = "blss"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    //Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    //Libs
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.mapstruct:mapstruct:1.6.3")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    //Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.processResources {
    val envMap = System.getenv()

    val srcResourcesDir = project.layout.projectDirectory.dir("src/main/resources")
    val propertiesFiles: Set<File> = srcResourcesDir.asFileTree
        .matching {
            include("**/*.yaml")
        }.files

    val placeholderRegex = Regex("""\$\{([^}]+)}""")

    val placeholdersByFile = mutableMapOf<File, Set<String>>()
    val allPlaceholders = linkedSetOf<String>()

    propertiesFiles.forEach { f ->
        val content = f.readText(Charsets.UTF_8)
        val found = placeholderRegex.findAll(content).map { it.groupValues[1] }.toSet()
        if (found.isNotEmpty()) {
            placeholdersByFile[f] = found
            allPlaceholders.addAll(found)
        }
    }

    // Базовое имя переменной — то, что до двоеточия (обрабатываем ${VAR:...})
    val requiredVars = allPlaceholders.map { it.substringBefore(':') }.toSet()

    // Инкрементальность: вносим требуемые env как inputs
    inputs.properties(requiredVars.associateWith { envMap[it] ?: "" })

    val missing = requiredVars.filter { envMap[it].isNullOrBlank() }.toSet()
    if (missing.isNotEmpty()) {
        val details = buildString {
            appendLine("Отсутствуют значения для переменных окружения: ${missing.sorted().joinToString(", ")}")
            appendLine("Использование по файлам:")
            placeholdersByFile.forEach { (file, phs) ->
                val here = phs.map { it.substringBefore(':') }.filter { it in missing }.toSet()
                if (here.isNotEmpty()) appendLine("- ${project.relativePath(file)}: ${here.joinToString(", ")}")
            }
        }
        throw GradleException(details)
    }

    // Готовим токены для замены:
    // ключ — то, что внутри ${...} (включая ':default'), значение — из env по базовому имени
    val tokensForReplace = allPlaceholders.associateWith { ph ->
        val key = ph.substringBefore(':')
        envMap[key]!!
    }

    // Подставляем значения в .properties
    filesMatching("**/*.yaml") {
        filteringCharset = "UTF-8"
        filter<ReplaceTokens>(
            "tokens" to tokensForReplace,
            "beginToken" to "\${",
            "endToken" to "}"
        )
    }
}