import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.feature.debug.settings.ui"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
        withHostTest {}
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
                implementation(libs.androidx.ui.tooling)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.appInfo)
                implementation(projects.core.log)
                implementation(projects.core.network)
                implementation(projects.core.remoteConfig)
                implementation(projects.feature.debugSettings.state)
                implementation(projects.feature.debugSettings.store)
                implementation(projects.feature.pro.state)
                implementation(projects.taj)

                implementation(libs.compose.foundation)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.lifecycle.viewmodel.compose)

                // Navigation 3
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)
                implementation(libs.material.adaptive)

                api(libs.di.koinComposeViewmodel)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)
            }
        }
    }
}
