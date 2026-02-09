import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.appversion"
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
                api(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.appInfo)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.core.remoteConfig)
                implementation(projects.platform.ops)
                implementation(projects.taj)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.compose.ui)
                api(libs.di.koinComposeViewmodel)
            }
        }
    }
}
