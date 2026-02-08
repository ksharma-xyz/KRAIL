import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.krail.maplibre)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.maps.ui"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.maps.state)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.maplibre.compose)
                implementation(libs.compose.runtime)
            }
        }
    }
}
