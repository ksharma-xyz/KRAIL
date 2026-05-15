import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.feature.debug.settings.store"
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
        commonMain {
            dependencies {
                implementation(projects.feature.debugSettings.state)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.sandook)

                implementation(libs.kotlinx.coroutines.core)

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
