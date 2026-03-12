@file:Suppress("PropertyName")

plugins {
    kotlin("jvm") version("2.3.0")
    id("java")
    id("maven-publish")
}

group = "net.mcbrawls"
version = "1.3.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://maven.mcbrawls.net/releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api("net.kyori:adventure-key:4.25.0")
    api("net.mcbrawls.api:core:1.2.0")
    api("net.mcbrawls:codex:2.0.0")
    api("net.jthink:jaudiotagger:3.0.1")
    api("com.github.mgrzeszczak:json-dsl:v1.1")
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
        }
    }

    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                name = "envmaven"
                url = uri(mavenUrl)
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}
