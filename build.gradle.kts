plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.TogethersChannel"
version = "1.5.1_Hotfix"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    mavenCentral()
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")
}


kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("ChasingTails")
        archiveClassifier.set("")
        archiveVersion.set(version.toString())
    }
    build {
        dependsOn(shadowJar)
    }
}
