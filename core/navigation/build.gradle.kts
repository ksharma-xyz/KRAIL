import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.navigation"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)

                implementation(libs.compose.runtime)
                implementation(compose.ui)

                // Navigation 3
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)

                // Serialization for navigation
                implementation(libs.kotlinx.serialization.json)
            }
        }

        iosMain {
            dependencies {
            }
        }

    }
}
