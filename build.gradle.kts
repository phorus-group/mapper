import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0-M1"
    `maven-publish`
    `java-library`
    jacoco
}

group = "group.phorus"
version = "1.0.0"
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
                    value = System.getenv("MAIN_DEPLOY_TOKEN_READONLY")
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

// Jacoco config
tasks.jacocoTestReport {
    executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))

    reports {
        xml.required.set(true)
        csv.required.set(true)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    finalizedBy(tasks.jacocoTestReport)

    // If parallel tests start failing, instead of disabling this, take a look at @Execution(ExecutionMode.SAME_THREAD)
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn")
        jvmTarget = "17"
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
                        value = System.getenv("MAIN_DEPLOY_TOKEN")
                    }
            }
            authentication {
                val header by registering(HttpHeaderAuthentication::class)
            }
        }
    }
}
