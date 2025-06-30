plugins {
    kotlin("jvm") version "1.8.22"
    id("fabric-loom") version "1.2.8"
}

group = "guesser.plugins"
version = "3.0.0.0"

repositories {
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    mappings("net.fabricmc:yarn:1.19.4+build.1")
    modImplementation("net.fabricmc:fabric-loader:0.14.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // ✅ Fabric API はこの1行で十分
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.76.0+1.19.4")
}

kotlin {
    jvmToolchain(17)
}
