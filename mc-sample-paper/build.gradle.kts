plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ai.gameragkit.samples"
version = "1.0.0"

val useRealPaper = providers.gradleProperty("useRealPaperApi").map { it.toBoolean() }.orElse(false)

dependencies {
    compileOnly(project(":stubs:paper-api-stubs"))
    if (useRealPaper.get()) {
        compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    }
    implementation(project(":client"))
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
