
import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.wire)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "xyz.ksharma.krail.io.bffapi"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        // MANDATORY for AGP 9 to include .pb files and other assets.
        // Mirrors :io:gtfs which already runs Wire 6.2.0 successfully on KMP-iOS.
        androidResources {
            enable = true
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Wire runtime is added transitively by the wire plugin
                // (matches :io:gtfs which compiles cleanly on KMP-iOS without
                // an explicit dependency block).
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
            }
        }
    }
}

// Proto sources JAR from GitHub Packages — published by KRAIL-API-PROTO on each tag.
// Version in gradle/libs.versions.toml (krail-api-proto). Renovate opens bump PRs.
// Must be populated BEFORE wire{} reads krailProto.singleFile (Kotlin DSL eval order).
val krailProto: Configuration by configurations.creating { isTransitive = false }
dependencies {
    krailProto(libs.krail.api.proto) { artifact { classifier = "proto" } }
}

wire {
    kotlin {
        javaInterop = true
        out = "$projectDir/build/generated/source/wire"
    }
    sourcePath {
        srcJar(krailProto.singleFile.absolutePath)
    }
}
