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
include(":core:network")
include(":core:remote-config")
include(":core:test")
include(":discover:network:api")
include(":discover:network:real")
include(":discover:state")
include(":discover:ui")
include(":feature:park-ride:network")
include(":feature:park-ride:ui")
include(":feature:trip-planner:ui")
include(":feature:trip-planner:state")
include(":feature:trip-planner:network")
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
