
import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.wire)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
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

// The .proto contract is shared with KRAIL-BFF via a public submodule
// pinned to a SemVer git tag (currently v0.1.0). Wire codegen reads from
// the submodule path; we never copy generated Kotlin into the repo.
//
// Submodule: https://github.com/ksharma-xyz/KRAIL-API-PROTO
// Layout:
//   krail-api-proto/proto/api/   contains app.krail.bff.proto (JourneyList, JourneyCardInfo, ...)
//   krail-api-proto/proto/data/  contains app.krail.bff.proto.data (StopsDataset, RoutesDataset)
wire {
    kotlin {
        javaInterop = true
        out = "$projectDir/build/generated/source/wire"
    }
    protoPath {
        srcDir("$rootDir/krail-api-proto/proto")
    }
    sourcePath {
        srcDir("$rootDir/krail-api-proto/proto")
    }
}
