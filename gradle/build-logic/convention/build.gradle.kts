import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "xyz.ksharma.krail.gradle"

val javaVersion = libs.versions.java.get().toInt()

java {
    sourceCompatibility = JavaVersion.values()[javaVersion - 1]
    targetCompatibility = JavaVersion.values()[javaVersion - 1]
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

kotlin {
    jvmToolchain(javaVersion)
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.detekt.gradle.plugin)
}

gradlePlugin {
    plugins {
        /**
         * Supports building UIs for Android, Desktop, and Web using Jetpack Compose.
         */
        register("composeMultiplatform") {
            id = "krail.compose.multiplatform"
            implementationClass = "xyz.ksharma.krail.gradle.ComposeMultiplatformConventionPlugin"
        }

        /**
         * Configures the project for Android application development.
         */
        register("androidApplication") {
            id = "krail.android.application"
            implementationClass = "xyz.ksharma.krail.gradle.AndroidApplicationConventionPlugin"
        }

        /**
         * Configures the project for developing Android libraries.
         */
        register("androidLibrary") {
            id = "krail.android.library"
            implementationClass = "xyz.ksharma.krail.gradle.AndroidLibraryConventionPlugin"
        }

        /**
         * Adds support for Kotlin in Android projects.
         */
        register("kotlinAndroid") {
            id = "krail.kotlin.android"
            implementationClass = "xyz.ksharma.krail.gradle.KotlinAndroidConventionPlugin"
        }

        /**
         * Configures the project for Kotlin Multiplatform development, supporting JVM, Android, iOS,
         * and JS targets.
         */
        register("kotlinMultiplatform") {
            id = "krail.kotlin.multiplatform"
            implementationClass = "xyz.ksharma.krail.gradle.KotlinMultiplatformConventionPlugin"
        }
    }
}
