dependencyResolutionManagement {
    repositories {
        google()
        maven("https://repo1.maven.org/maven2")
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        google()
        maven("https://repo1.maven.org/maven2")
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
include(":convention")
