import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
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

        withHostTest {
            isIncludeAndroidResources = true
        }

        // MANDATORY for AGP 9 to include assets
        androidResources {
            enable = true
        }
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
                // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-androidx.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio
                implementation(libs.androidx.ui.tooling)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.snapshotTestingAnnotations)
                implementation(projects.core.adaptiveUi)
                implementation(projects.core.appInfo)
                implementation(projects.core.appVersion)
                implementation(projects.core.analytics)
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.dateTime)
                implementation(projects.core.di)
                implementation(projects.core.festival)
                implementation(libs.dhruva.data)
                implementation(projects.core.log)
                implementation(projects.core.maps.data)
                implementation(projects.core.maps.state)
                implementation(projects.core.maps.ui)
                implementation(projects.core.navigation)
                implementation(libs.aagya.data)
                implementation(projects.core.remoteConfig)
                implementation(projects.core.share)
                implementation(projects.core.transport)
                implementation(projects.core.uiTooling)
                implementation(projects.discover.network.api)
                implementation(projects.discover.state)
                implementation(projects.discover.ui)
                implementation(projects.feature.debugSettings.state)
                implementation(projects.feature.debugSettings.store)
                implementation(projects.feature.debugSettings.ui)
                implementation(projects.feature.departures.state)
                implementation(projects.feature.departures.ui)
                implementation(projects.feature.parkRide.network)
                implementation(projects.feature.parkRide.ui)
                implementation(projects.feature.track.state)
                implementation(projects.feature.track.ui)
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

                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.animation)
                implementation(libs.compose.foundation)
                implementation(libs.material.icons.core)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.reorderable)

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

                implementation(projects.core.analytics)
                implementation(projects.sandook)
                implementation(projects.feature.departures.network)
                implementation(projects.core.testing)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
            resources.srcDir("src/androidHostTest/resources")
            dependencies {
                implementation(projects.core.snapshotTesting)
                // Robolectric's createComposeRule() launches a ComponentActivity via
                // ActivityScenario, which needs it declared in a manifest to resolve the
                // launch intent — this artifact ships exactly that debug-only manifest.
                implementation(libs.test.composeUiTestManifest)
            }
        }
    }
}
