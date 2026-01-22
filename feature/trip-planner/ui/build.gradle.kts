import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.krail.maplibre)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.trip.planner.ui"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TripPlannerUI"
            isStatic = true
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.androidx.ui.geometry.android)
                // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-org.jetbrains.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio
                implementation(libs.androidx.ui.tooling)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.appInfo)
                implementation(projects.core.appVersion)
                implementation(projects.core.analytics)
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.dateTime)
                implementation(projects.core.di)
                implementation(projects.core.festival)
                implementation(projects.core.log)
                implementation(projects.core.navigation)
                implementation(projects.core.remoteConfig)
                implementation(projects.core.uiTooling)
                implementation(projects.discover.network.api)
                implementation(projects.discover.state)
                implementation(projects.discover.ui)
                implementation(projects.feature.parkRide.network)
                implementation(projects.feature.tripPlanner.network)
                implementation(projects.feature.tripPlanner.state)
                implementation(projects.io.gtfs)
                implementation(projects.infoTile.network.api)
                implementation(projects.infoTile.state)
                implementation(projects.infoTile.ui)
                implementation(projects.platform.ops)
                implementation(projects.sandook)
                implementation(projects.social.network.api)
                implementation(projects.social.state)
                implementation(projects.social.ui)
                implementation(projects.taj)

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.animation)
                implementation(compose.foundation)
                implementation(libs.material.icons.core)
                implementation(compose.ui)

                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.lifecycle.viewmodel.compose)

                // Navigation 3
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)

                implementation(libs.molecule.runtime)
                implementation(libs.material.adaptive)

                implementation(libs.maplibre.compose)
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
