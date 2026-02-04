import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        namespace = "xyz.ksharma.krail.shared"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KrailApp"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=xyz.ksharma.krail")
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.foundation)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.client.okhttp)
            }
        }

        commonMain.dependencies {
            implementation(projects.core.analytics)
            implementation(projects.core.appInfo)
            implementation(projects.core.appStart)
            implementation(projects.core.appVersion)
            implementation(projects.core.coroutinesExt)
            implementation(projects.core.di)
            implementation(projects.core.festival)
            implementation(projects.core.log)
            implementation(projects.core.navigation)
            implementation(projects.core.network)
            implementation(projects.core.remoteConfig)
            implementation(projects.discover.network.real)
            implementation(projects.feature.parkRide.network)
            implementation(projects.feature.tripPlanner.network)
            implementation(projects.feature.tripPlanner.state)
            implementation(projects.feature.tripPlanner.ui)
            implementation(projects.io.gtfs)
            implementation(projects.infoTile.network.real)
            implementation(projects.platform.ops)
            implementation(projects.sandook)
            implementation(projects.taj)

            // Navigation 3
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            api(libs.di.koinComposeViewmodel)

            implementation(libs.coil3.compose)
            implementation(libs.coil3.networkKtor)
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
