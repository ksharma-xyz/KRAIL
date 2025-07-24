plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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
                implementation(projects.core.appInfo)
                implementation(projects.core.analytics)
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.dateTime)
                implementation(projects.core.di)
                implementation(projects.core.festival)
                implementation(projects.core.log)
                implementation(projects.core.social)
                implementation(projects.core.remoteConfig)
                implementation(projects.discover.ui)
                implementation(projects.feature.parkRide.network)
                implementation(projects.feature.tripPlanner.network)
                implementation(projects.feature.tripPlanner.state)
                implementation(projects.io.gtfs)
                implementation(projects.platform.ops)
                implementation(projects.sandook)
                implementation(projects.taj)
                implementation(projects.discover.network.api) // todo - temp

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.animation)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.navigation.compose)

                implementation(libs.molecule.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)

                implementation(projects.sandook)
            }
        }
    }
}

android {
    namespace = "xyz.ksharma.krail.trip.planner.ui"

    // Required when using Firebase GitLive RemoteConfig. Adding here for running previews on device.
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.androidx.ui.geometry.android)

    // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-org.jetbrains.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio#focus=Comments-27-11400795.0-0Add commentMore actions
    debugImplementation(libs.androidx.ui.tooling)

    // Required when using Firebase GitLive RemoteConfig.
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
