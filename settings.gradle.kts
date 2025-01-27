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
include(":core:coroutines-ext")
include(":core:date-time")
include(":core:di")
include(":core:io")
include(":core:log")
include(":core:remote-config")
include(":core:test")
include(":feature:trip-planner:ui")
include(":feature:trip-planner:state")
include(":feature:trip-planner:network")
include(":sandook")
include(":gtfs-static")
