import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}


kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.trip.planner.state"
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
                implementation(projects.core.analytics)
                implementation(projects.core.dateTime)
                implementation(projects.core.festival)
                implementation(projects.infoTile.state)
                implementation(projects.social.network.api)
                implementation(projects.social.state)
                implementation(projects.taj)

                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.runtime)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
