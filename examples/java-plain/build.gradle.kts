plugins {
    application
}

group = "ai.gameragkit.examples"
version = "1.0.0"

dependencies {
    implementation(project(":client"))
}

application {
    mainClass.set("ai.gameragkit.examples.plain.Main")
}
