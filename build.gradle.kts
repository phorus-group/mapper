import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0-M1"
    `maven-publish`
    `java-library`
    signing
    jacoco
}

group = "group.phorus"
description = "Kotlin based mapper with extra funcitonalities."
version = "1.0.0"

java.sourceCompatibility = JavaVersion.VERSION_17

java {
    withJavadocJar()
    withSourcesJar()
}

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

    javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
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
                url.set(System.getenv("CI_PROJECT_URL"))

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
                    url.set(System.getenv("CI_PROJECT_URL"))
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
