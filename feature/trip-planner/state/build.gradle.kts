plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.trip.planner.state"
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.dateTime)
                implementation(projects.core.festival)
                implementation(projects.taj)
                implementation(projects.core.analytics)

                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.serialization.json)

                implementation(compose.runtime)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
