import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.blss"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = "com.blss"
    version = "0.0.1-SNAPSHOT"

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

    dependencies {
        // Lombok
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // Test
        testImplementation("org.springframework.boot:spring-boot-starter-test")
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

        val requiredVars = allPlaceholders
            .filter { !it.contains(':') }
            .map { it.substringBefore(':') }
            .toSet()

        inputs.properties(requiredVars.associateWith { envMap[it] ?: "" })

        val missing = requiredVars.filter { envMap[it].isNullOrBlank() }.toSet()
        if (missing.isNotEmpty()) {
            val details = buildString {
                appendLine("Missing environment variables: ${missing.sorted().joinToString(", ")}")
                appendLine("Usage by file:")
                placeholdersByFile.forEach { (file, phs) ->
                    val here = phs.map { it.substringBefore(':') }.filter { it in missing }.toSet()
                    if (here.isNotEmpty()) appendLine("- ${project.relativePath(file)}: ${here.joinToString(", ")}")
                }
            }
            throw GradleException(details)
        }

        val tokensForReplace = allPlaceholders.associateWith { ph ->
            val key = ph.substringBefore(':')
            val defaultValue = ph.substringAfter(':', "")
            envMap[key]
                ?.takeIf { it.isNotBlank() }
                ?: if (defaultValue.isNotEmpty()) {
                    defaultValue
                } else {
                    throw GradleException("Missing environment variable: $key")
                }
        }

        filesMatching("**/*.yaml") {
            filteringCharset = "UTF-8"
            filter<ReplaceTokens>(
                "tokens" to tokensForReplace,
                "beginToken" to "\${",
                "endToken" to "}"
            )
        }
    }
}
