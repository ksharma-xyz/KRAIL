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
        maven("https://repo1.maven.org/maven2")
        gradlePluginPortal()
    }
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
        maven("https://repo1.maven.org/maven2")
    }
}

rootProject.name = "Krail"
//include(":android-app")
include(":composeApp")
include(":taj") // Design System
include(":core:analytics")
include(":core:app-info")
include(":core:app-start")
include(":core:app-version")
include(":core:coroutines-ext")
include(":core:date-time")
include(":core:di")
include(":core:festival")
include(":core:log")
include(":core:navigation")
include(":core:network")
include(":core:remote-config")
include(":core:snapshot-testing")
include(":core:snapshot-testing-annotations")
include(":core:test")
include(":core:ui-tooling")
include(":discover:network:api")
include(":discover:network:real")
include(":discover:state")
include(":discover:ui")
include(":feature:park-ride:network")
include(":feature:park-ride:ui")
include(":feature:trip-planner:network")
include(":feature:trip-planner:state")
include(":feature:trip-planner:ui")
include(":sandook")
include(":social:network:api")
include(":social:network:real")
include(":social:state")
include(":social:ui")
include(":gtfs-static")
include(":io:gtfs")
include(":info-tile:network:api")
include(":info-tile:network:fake")
include(":info-tile:network:real")
include(":info-tile:state")
include(":info-tile:ui")
include(":platform:ops")
