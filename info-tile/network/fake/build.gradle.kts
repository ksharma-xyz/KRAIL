import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.info.tile.network.fake"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)

                implementation(projects.core.log)

                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
