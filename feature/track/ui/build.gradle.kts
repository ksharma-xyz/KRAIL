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
        namespace = "xyz.ksharma.krail.feature.track.ui"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TrackUI"
            isStatic = true
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.ui.geometry.android)
                implementation(libs.androidx.ui.tooling)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.maps.state)
                implementation(projects.core.dateTime)
                implementation(projects.core.di)
                implementation(projects.core.festival)
                implementation(projects.core.log)
                implementation(projects.core.navigation)
                implementation(projects.core.transport)
                implementation(projects.core.uiTooling)
                implementation(projects.feature.track.state)
                implementation(projects.feature.tripPlanner.network)
                implementation(projects.sandook)
                implementation(projects.feature.tripPlanner.state)
                implementation(projects.taj)

                implementation(libs.compose.animation)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.material.icons.core)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)

                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.lifecycle.viewmodel.compose)

                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)
            }
        }
    }
}
