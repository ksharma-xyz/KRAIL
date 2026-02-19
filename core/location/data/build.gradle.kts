import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.location.data"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.core.ktx)
                implementation(libs.activity.compose)
                implementation(libs.lifecycle.runtime.ktx)
                implementation(libs.playServicesLocation)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.di)
                api(projects.core.location.state)
                implementation(projects.core.log)

                implementation(libs.compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.di.koinComposeViewmodel)
            }
        }

        iosMain.dependencies {
            // iOS uses CoreLocation via cinterop
        }
    }
}

