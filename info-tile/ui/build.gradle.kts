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
        namespace = "xyz.ksharma.krail.info.tiles.ui"
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
                // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-androidx.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio
                implementation(libs.androidx.ui.tooling)
            }
        }

        commonMain  {
            dependencies {
                implementation(libs.compose.animation)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.foundation)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.material.adaptive)

                implementation(projects.core.log)
                implementation(projects.infoTile.state)
                implementation(projects.taj)
            }
        }
    }
}
