import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "xyz.ksharma.krail.gradle"

val javaVersion = libs.versions.java.get().toInt()

java {
    // Force Java 21 for the build-logic itself
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.detekt.gradle.plugin)

    implementation("io.github.frankois944.spmForKmp:io.github.frankois944.spmForKmp.gradle.plugin:${libs.versions.spmForKmp.get()}")
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

        register("mapLibre") {
            id = "krail.maplibre"
            implementationClass = "xyz.ksharma.krail.gradle.MapLibreConventionPlugin"
        }

        /**
         * Configures Android library for KMP projects using the new android-kotlin-multiplatform-library plugin.
         * Automatically sets compileSdk and minSdk from AndroidVersion constants.
         */
        register("androidKmpLibrary") {
            id = "krail.android.kmp.library"
            implementationClass = "xyz.ksharma.krail.gradle.AndroidKmpLibraryConventionPlugin"
        }
    }
}
