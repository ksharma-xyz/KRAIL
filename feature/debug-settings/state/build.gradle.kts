import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.feature.debug.settings.state"
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
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
            }
        }
    }
}
