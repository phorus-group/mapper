import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.time.LocalDate

plugins {
    kotlin("jvm") version "1.6.0-M1"
    id("org.jetbrains.dokka") version "1.7.10"
    `maven-publish`
    `java-library`
    signing
    jacoco
}

group = "group.phorus"
description = "Kotlin based mapper with extra funcitonalities."
version = "1.0.0"

java.sourceCompatibility = JavaVersion.VERSION_17
java.withSourcesJar()
java.withJavadocJar()

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("reflect"))

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
}


val repoUrl = System.getenv("CI_PROJECT_URL") ?: "not defined"

tasks {
    // Jacoco config
    jacocoTestReport {
        executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))

        reports {
            xml.required.set(true)
            csv.required.set(true)
        }
    }

    withType<Test> {
        useJUnitPlatform()

        finalizedBy(jacocoTestReport)

        // If parallel tests start failing, instead of disabling this, take a look at @Execution(ExecutionMode.SAME_THREAD)
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
        systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn")
            jvmTarget = "17"
        }
    }

    dokkaHtml.configure {
        val branch = System.getenv("CI_COMMIT_BRANCH") ?: "not defined"

        dokkaSourceSets {
            configureEach {
                reportUndocumented.set(true)
                platform.set(org.jetbrains.dokka.Platform.jvm)

                sourceRoot(file("src"))

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(URL("$repoUrl/-/tree/$branch/src/main/kotlin"))
                    remoteLineSuffix.set("#L")
                }
            }
        }

        val currentYear = LocalDate.now().year
        pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.base.DokkaBase" to
                " {\"footerMessage\":" +
                "\"© $currentYear Phorus Group - Licensed under the " +
                "<a target=\\\"_blank\\\" href=\\\"$repoUrl/-/tree/$branch/LICENSE\\\">Apache 2 license</a>.\"}"
            )
        )
    }

    named<Jar>("javadocJar") {
        from(dokkaHtml)
        dependsOn(dokkaHtml)
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            groupId = "${project.group}"
            artifactId = project.name
            version = "${project.version}"
            from(components["java"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(repoUrl)

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("irios.phorus")
                        name.set("Martin Ivan Rios")
                        email.set("irios@phorus.group")
                        organization.set("Phorus Group")
                        organizationUrl.set("https://phorus.group")
                    }
                }

                scm {
                    url.set(repoUrl)
                    connection.set("scm:git:${System.getenv("CI_PROJECT_URL")}.git")
                    developerConnection.set("scm:git:${System.getenv("CI_PROJECT_URL")}.git")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications[project.name])
}
