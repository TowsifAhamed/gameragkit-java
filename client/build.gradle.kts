plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "ai.gameragkit"
version = "1.0.0"

val ossrhUsername: String? = providers.environmentVariable("OSSRH_USERNAME").orNull
    ?: providers.gradleProperty("ossrhUsername").orNull
val ossrhPassword: String? = providers.environmentVariable("OSSRH_PASSWORD").orNull
    ?: providers.gradleProperty("ossrhPassword").orNull
val isSnapshot = version.toString().endsWith("SNAPSHOT")

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("gameragkit-client")
                description.set("Java client for GameRAGKit service")
                url.set("https://github.com/TowsifAhamed/gameragkit-java")
                licenses {
                    license {
                        name.set("PolyForm Noncommercial 1.0.0 + Commercial License")
                        url.set("https://polyformproject.org/licenses/noncommercial/1.0.0/")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/TowsifAhamed/gameragkit-java")
                    connection.set("scm:git:https://github.com/TowsifAhamed/gameragkit-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/TowsifAhamed/gameragkit-java.git")
                }
                developers {
                    developer {
                        name.set("Towsif Ahamed")
                        email.set("towsif.kuet.ac.bd@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        if (!ossrhUsername.isNullOrBlank() && !ossrhPassword.isNullOrBlank()) {
            maven {
                name = "OSSRH"
                url = uri(
                    if (isSnapshot) {
                        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    } else {
                        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    }
                )
                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
        }
    }
}

signing {
    val signingKey: String? = providers.environmentVariable("SIGNING_KEY").orNull
        ?: providers.gradleProperty("signingKey").orNull
    val signingPassword: String? = providers.environmentVariable("SIGNING_PASSWORD").orNull
        ?: providers.gradleProperty("signingPassword").orNull

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}
