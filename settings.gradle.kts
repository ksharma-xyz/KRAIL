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
include(":core:coroutines-ext")
include(":core:date-time")
include(":core:di")
include(":core:festival")
include(":core:log")
include(":core:network")
include(":core:remote-config")
include(":core:social")
include(":core:test")
include(":discover:network:api")
include(":discover:network:real")
include(":discover:ui")
include(":feature:park-ride:network")
include(":feature:park-ride:ui")
include(":feature:trip-planner:ui")
include(":feature:trip-planner:state")
include(":feature:trip-planner:network")
include(":sandook")
include(":gtfs-static")
include(":io:gtfs")
include(":platform:ops")
