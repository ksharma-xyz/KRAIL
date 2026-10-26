import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.feature.track.network"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.appInfo)
                implementation(projects.core.dateTime)
                implementation(projects.core.log)
                implementation(projects.core.network)
                implementation(projects.core.remoteConfig)
                implementation(projects.core.transport)
                implementation(projects.feature.debugSettings.store)
                implementation(projects.feature.track.state)
                implementation(projects.io.bffApi)
                api(projects.feature.crowding.state) // CarriageForecastRepository is bound here
                api(projects.io.gtfs) // FeedMessage on the public GtfsRealtimeService

                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.datetime)

                api(libs.di.koinComposeViewmodel)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(projects.core.testing)
            }
        }
    }
}
