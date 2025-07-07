plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.core.test"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
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
                implementation(projects.core.log)
                implementation(libs.kotlinx.serialization.json)

                implementation(compose.runtime)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.core.analytics)
                implementation(projects.core.dateTime)
                implementation(projects.core.festival)
                implementation(projects.core.log)
                implementation(projects.core.remoteConfig)
                implementation(projects.sandook)
                implementation(projects.feature.parkRide.network)
                implementation(projects.feature.tripPlanner.ui)
                implementation(projects.feature.tripPlanner.state)
                implementation(projects.feature.tripPlanner.network)
                implementation(projects.taj)

                implementation(libs.test.junit)
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.molecule.runtime)
            }
        }
    }
}
