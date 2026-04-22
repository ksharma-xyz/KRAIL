import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.deeplink"
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
        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)
            }
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.di.koinComposeViewmodel)
            }
        }
    }
}
