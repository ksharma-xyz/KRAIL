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
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.testing"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        withHostTest {}
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
                api(projects.core.appInfo) // AppInfo + AppInfoProvider + DevicePlatformType
                api(projects.core.appVersion) // AppVersionManager + AppVersionUpdateState
                api(projects.core.festival) // FestivalManager + Festival model
                api(projects.core.share) // ShareManager
                api(projects.core.maps.data) // NearbyStop + NearbyStopsRepository
                api(projects.infoTile.network.api) // InfoTileManager + db extensions
                api(projects.infoTile.state) // InfoTileData
                api(projects.feature.parkRide.network) // NswParkRideFacilityManager + CarPark models
                api(projects.platform.ops) // PlatformOps
                api(projects.core.transport) // TransportMode (for stop.transportModes in fakes)

                api(libs.kotlinx.datetime) // LocalDate used by FakeFestivalManager
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
            }
        }
    }
}
