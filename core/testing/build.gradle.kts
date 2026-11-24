import xyz.ksharma.krail.gradle.AndroidVersion

/*
 * :core:testing — KMP test fixtures for the rest of the codebase.
 *
 * Layout choice: fakes/helpers live in `commonMain`, not `commonTest`.
 * KMP has no `java-test-fixtures` equivalent and a `commonTest` artifact from
 * another module cannot be consumed via `testImplementation`. A `commonMain`
 * in a module that is ONLY ever depended on via `testImplementation` is the
 * idiomatic KMP test-fixtures pattern and sidesteps every variant-resolution
 * pitfall. The `verifyTestingModuleUsage` guardrail (introduced in the same
 * stack) enforces that no main/production configuration ever resolves this
 * module, so test code can never ship in the app.
 *
 * This skeleton ships the coroutine/scheduler harness only (KrailTestKit).
 * Canonical fakes (FakeSandook, FakeAnalytics, FakeFlag, FakeTripPlanningService,
 * FakeDeparturesService, …) and DTO builders move in a follow-up stack.
 */
plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.testing"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        withHostTest {
            // The device matrix helper's own tests run under Robolectric, which needs the merged Android resources.
            isIncludeAndroidResources = true
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
                // `api` because these types (TestScope, TestCoroutineScheduler, Turbine
                // extensions, kotlin.test assertions) appear in the public surface of
                // KrailTestKit and consumer tests use them directly.
                api(libs.test.kotlin)
                api(libs.test.kotlinxCoroutineTest)
                api(libs.test.turbine)

                // Production INTERFACE modules only — never `Real*` impl modules. This
                // is what keeps `:core:testing` thin and prevents the god-module that
                // `:core:test` became (see plan: anti-god-module guarantees).
                api(projects.sandook) // Sandook + SandookPreferences interfaces
                api(projects.core.analytics) // Analytics + AnalyticsEvent
                api(projects.core.remoteConfig) // Flag + FlagKeys + FlagValue
                api(projects.feature.tripPlanner.network) // TripPlanningService, Stop/TripResponse, StopType, DepArr
                api(projects.feature.departures.network) // DeparturesService + DepartureMonitorResponse
                api(projects.feature.track.network) // GtfsRealtimeService + GtfsRealtimeResult
                api(projects.core.appInfo) // AppInfo + AppInfoProvider + DevicePlatformType
                api(projects.core.connectivity) // ConnectivityObserver + TransportState
                api(projects.core.network) // NetworkError + NetworkException for fake failures
                api(projects.core.appVersion) // AppVersionManager + AppVersionUpdateState
                api(projects.core.festival) // FestivalManager + Festival model
                api(projects.core.share) // ShareManager
                api(projects.core.maps.data) // NearbyStop + NearbyStopsRepository
                api(projects.infoTile.network.api) // InfoTileManager + db extensions
                api(projects.infoTile.state) // InfoTileData
                api(projects.io.gtfs) // StopsManager + AppDataReadiness interfaces
                api(projects.feature.parkRide.network) // NswParkRideFacilityManager + CarPark models
                api(projects.platform.ops) // PlatformOps
                api(projects.core.transport) // TransportMode (for stop.transportModes in fakes)

                api(libs.kotlinx.datetime) // LocalDate used by FakeFestivalManager
            }
        }

        // The device profile matrix (`docs/testing/DEVICE_PROFILES.md`): Compose host test helpers that compose a screen on a list of
        // devices with each platform's real system insets. They live in androidMain because Robolectric and the Compose test rule
        // are Android only, and they are reachable only through test configurations, as every other part of this module is.
        androidMain {
            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.ui)
                api(libs.test.composeUiTestJunit4)
                api(libs.test.junit)
                api(libs.test.robolectric)

                implementation(libs.activity.compose)
                implementation(libs.compose.foundation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                // Robolectric's ActivityScenario launches a ComponentActivity, which needs it declared in a manifest. This
                // artifact ships exactly that debug only manifest.
                implementation(libs.test.composeUiTestManifest)
            }
        }
    }
}
