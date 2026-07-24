import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Consumed by the root build's :convention module across the includeBuild("gradle/build-logic")
// composite boundary via dependency substitution (project(":detekt-rules") only resolves within
// this composite build itself, not the root build that calls configureDetekt()) — see Detekt.kt.
group = "xyz.ksharma.krail.gradle"
version = "unspecified"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}
