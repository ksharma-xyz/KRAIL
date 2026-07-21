import java.util.Properties

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("gradle/build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Read local.properties for dev credentials (git-ignored, never committed).
val localProps = Properties().apply {
    rootProject.projectDir.resolve("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // KRAIL-API-PROTO proto sources JAR — published on each proto release tag.
        // CI: GITHUB_TOKEN env var is set automatically (has read:packages scope).
        // Local dev: add gpr.token=<github-pat-with-read:packages> to local.properties.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ksharma-xyz/KRAIL-API-PROTO")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: localProps.getProperty("gpr.user") ?: "token"
                password = System.getenv("GITHUB_TOKEN")
                    ?: localProps.getProperty("gpr.token") ?: ""
            }
        }
    }
}

rootProject.name = "Krail"
include(":androidApp")
include(":composeApp")
include(":taj") // Design System
include(":core:adaptive-ui")
include(":core:analytics")
include(":core:app-info")
include(":core:app-review")
include(":core:app-start")
include(":core:app-version")
include(":core:coroutines-ext")
include(":core:date-time")
include(":core:deeplink")
include(":core:di")
include(":core:festival")
include(":core:log")
include(":core:maps:data")
include(":core:maps:state")
include(":core:maps:ui")
include(":core:navigation")
include(":core:network")
include(":core:remote-config")
include(":core:share")
include(":core:snapshot-testing")
include(":core:snapshot-testing-annotations")
include(":core:testing")
include(":core:transport")
include(":core:ui-tooling")
include(":discover:network:api")
include(":discover:network:real")
include(":discover:state")
include(":discover:ui")
include(":feature:debug-settings:state")
include(":feature:debug-settings:store")
include(":feature:debug-settings:ui")
include(":feature:departures:network")
include(":feature:departures:state")
include(":feature:departures:ui")
include(":feature:park-ride:network")
include(":feature:park-ride:ui")
include(":feature:track:state")
include(":feature:track:network")
include(":feature:track:ui")
include(":feature:trip-planner:network")
include(":feature:trip-planner:state")
include(":feature:trip-planner:ui")
include(":sandook")
include(":social:network:api")
include(":social:network:real")
include(":social:state")
include(":social:ui")
include(":gtfs-static")
include(":io:bff-api")
include(":io:gtfs")
include(":info-tile:network:api")
include(":info-tile:network:fake")
include(":info-tile:network:real")
include(":info-tile:state")
include(":info-tile:ui")
include(":platform:ops")
