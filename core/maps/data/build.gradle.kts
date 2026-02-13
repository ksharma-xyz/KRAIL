import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.krail.kotlin.multiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.maps.data"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
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
                // Core modules
                implementation(projects.core.log)
                implementation(projects.core.maps.state)

                // Sandook for database access
                implementation(projects.sandook)

                // Trip planner state for TransportMode
                implementation(projects.feature.tripPlanner.state)

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // DI
                implementation(libs.di.koinComposeViewmodel)
            }
        }
    }
}
