import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.shared"
        compileSdk = 36
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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
                implementation(compose.preview)
                implementation(compose.foundation)
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

            implementation(libs.navigation.compose)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            api(libs.di.koinComposeViewmodel)
            // Note: All Firebase dependencies moved to androidApp
            // They require the Firebase Gradle plugins and google-services.json

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


