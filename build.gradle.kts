import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

group = "com.badr.vertx"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly("io.vertx:vertx-web:4.2.1")
    compileOnly(kotlin("stdlib"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest {
        attributes["version"] = project.version
    }
    archiveFileName.set("Throttling.${archiveExtension.get()}")
}