import com.kageiit.jacobo.JacoboTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.time.LocalDate

plugins {
    kotlin("jvm").version("1.9.25")
    id("org.jetbrains.dokka").version("1.9.20")
    id("io.github.gradle-nexus.publish-plugin").version("2.0.0")
    id("com.kageiit.jacobo") version "2.1.0"
    `maven-publish`
    `java-library`
    signing
    jacoco
}

group = "group.phorus"
description = "Kotlin based mapper with extra functionalities."
version = "1.1.4"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("reflect"))

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
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

        finalizedBy("jacobo")
    }

    withType<Test> {
        useJUnitPlatform()

        finalizedBy(jacocoTestReport)

        // If parallel tests start failing, instead of disabling this, take a look at @Execution(ExecutionMode.SAME_THREAD)
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
        systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")

        finalizedBy(jacocoTestReport)
    }

    register<JacoboTask>("jacobo") {
        description = "Transforms jacoco xml report to cobertura"
        group = "verification"

        jacocoReport = file("$buildDir/reports/jacoco/test/jacocoTestReport.xml")
        coberturaReport = file("$buildDir/reports/cobertura/cobertura.xml")
        includeFileNames = emptySet()

        val field = JacoboTask::class.java.getDeclaredField("srcDirs")
        field.isAccessible = true
        field.set(this, sourceSets["main"].allSource.srcDirs.map { it.path }.toTypedArray())

        dependsOn(jacocoTestReport)
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = java.targetCompatibility.toString()
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

    repositories {
        maven {
            name = "OSSRH"

            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = System.getenv("OSSRH_USER") ?: return@credentials
                password = System.getenv("OSSRH_PASSWORD") ?: return@credentials
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USER") ?: return@sonatype)
            password.set(System.getenv("OSSRH_PASSWORD") ?: return@sonatype)
        }
    }
}

signing {
    val key = System.getenv("PUBLIC_PUBLISH_SIGNING_KEY") ?: return@signing
    val password = System.getenv("PUBLIC_PUBLISH_SIGNING_PASSWORD") ?: return@signing

    useInMemoryPgpKeys(key, password)
    sign(publishing.publications[project.name])
}
