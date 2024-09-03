enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {

    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Krail"
include(":app")
include(":core:coroutines-ext")
include(":core:di")
include(":core:domain")
include(":core:design-system")
include(":core:model")
include(":core:network")
include(":core:utils")
include(":feature:sydney-trains:database:api")
include(":feature:sydney-trains:database:real")
include(":feature:sydney-trains:domain")
include(":feature:sydney-trains:model")
include(":feature:sydney-trains:network:api")
include(":feature:sydney-trains:network:real")
include(":feature:sydney-trains:ui")
