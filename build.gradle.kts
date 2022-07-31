import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0-M1"
    `maven-publish`
    `java-library`
}

group = "group.phorus"
version = "0.0.12"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        name = "gitlabPackageRegistry"
        url = uri("https://gitlab.com/api/v4/groups/51305899/-/packages/maven")

        credentials(HttpHeaderCredentials::class.java) {
            (project.findProperty("gitlabPackageRegistryToken") as String?)
                ?.also {
                    name = "Private-Token"
                    value = it
                }
                ?: also {
                    name = "Deploy-Token"
                    value = System.getenv("CI_BUILD_TOKEN ")
                }
        }
        authentication {
            val header by registering(HttpHeaderAuthentication::class)
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "16"
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "gitlabPackageRegistry"

            val projectId = System.getenv("CI_PROJECT_ID")
            url = uri("https://gitlab.com/api/v4/projects/$projectId/packages/maven")

            credentials(HttpHeaderCredentials::class.java) {
                (project.findProperty("gitlabPackageRegistryToken") as String?)
                    ?.also {
                        name = "Private-Token"
                        value = it
                    }
                    ?: also {
                        name = "Deploy-Token"
                        value = System.getenv("CI_BUILD_TOKEN")
                    }
            }
            authentication {
                val header by registering(HttpHeaderAuthentication::class)
            }
        }
    }
}
