import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    jacoco
    id("application")
    kotlin("jvm") version "1.4.0"
    id("io.gitlab.arturbosch.detekt") version "1.12.0"
    id("com.github.ben-manes.versions") version "0.29.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
}

group = "ru.tinkoff.qa"
version = "0.4.0"

application {
    mainClassName = "com.github.m9ffk4.konsul.KonsulKt"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = application.mainClassName
        attributes["Implementation-Version"] = project.version
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

detekt {
    toolVersion = "1.1.1"
    input = files("src/main/kotlin")
    config = files("${rootProject.projectDir.toPath()}/detekt.yml")
}

repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.repository.redhat.com/earlyaccess/all/") }
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    // CLI args
    implementation(group = "com.github.ajalt", name = "clikt", version = "2.8.0")
    // Consul
    implementation(group = "com.ecwid.consul", name = "consul-api", version = "1.4.5")
    // Log
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "+")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.8.3")
    // Yaml
    implementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.4.0")
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "2.11.2")
    // JSON
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.2")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.2")
}

private val versionRegex = "version = \"(\\d.\\d.\\d)\"".toRegex()
private val buildGradle = File("${System.getProperty("user.dir")}/build.gradle.kts")

tasks {
    register("upMajor") {
        group = "Versioning"
        description = "Up major version and replace it in build.gradle.kts"
        doLast {
            val currentVersion = versionRegex.find(String(buildGradle.readBytes()))!!.groupValues[1]
            val newVersion = currentVersion.replace(
                "^(\\d).".toRegex(),
                "${("^(\\d).".toRegex().find(currentVersion)!!.groupValues[1].toInt() + 1)}."
            )
            replaceVersionInBuildFile(newVersion)
        }
    }

    register("upMinor") {
        group = "Versioning"
        description = "Up minor version and replace it in build.gradle.kts"
        doLast {
            val currentVersion = versionRegex.find(String(buildGradle.readBytes()))!!.groupValues[1]
            val newVersion = currentVersion.replace(
                ".(\\d).".toRegex(),
                ".${(".(\\d).".toRegex().find(currentVersion)!!.groupValues[1].toInt() + 1)}."
            )
            replaceVersionInBuildFile(newVersion)
        }
    }

    register("upPatch") {
        group = "Versioning"
        description = "Up patch version and replace it in build.gradle.kts"
        doLast {
            val currentVersion = versionRegex.find(String(buildGradle.readBytes()))!!.groupValues[1]
            val newVersion = currentVersion.replace(
                ".(\\d)$".toRegex(),
                ".${(".(\\d)$".toRegex().find(currentVersion)!!.groupValues[1].toInt() + 1)}"
            )
            replaceVersionInBuildFile(newVersion)
        }
    }
}

fun replaceVersionInBuildFile(newVersion: String) {
    val file = String(buildGradle.readBytes()).replaceFirst(versionRegex, "version = \"$newVersion\"")
    buildGradle.writeText(file, Charsets.UTF_8)
}
