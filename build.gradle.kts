import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    jacoco
    id("application")
    kotlin("jvm") version "1.4.10"
    id("io.gitlab.arturbosch.detekt") version "1.12.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
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
    input = files("src/main/kotlin", "src/test/kotlin")
}

ktlint {
    version.set("0.38.1")
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    reporters {
        reporter(PLAIN)
    }
}

repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.repository.redhat.com/earlyaccess/all/") }
}

dependencies {
    // Kotlin
    implementation(kotlin("reflect"))

    // CLI args
    implementation(group = "com.github.ajalt", name = "clikt", version = "2.8.0")

    // Consul
    implementation(group = "com.ecwid.consul", name = "consul-api", version = "1.4.5")

    // Yaml
    implementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.4.0")
    implementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "2.11.2")

    // JSON
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.2")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.2")
    implementation(kotlin("stdlib-jdk8"))
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

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
