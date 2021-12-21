plugins {
    java
    kotlin("jvm") version "1.6.0"
}

group = "com.badr.vertx"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api("io.vertx:vertx-web:4.2.1")
    api(kotlin("stdlib"))
}
