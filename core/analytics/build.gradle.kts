plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.core.analytics"
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
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
                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.firebase.gitLiveAnalytics)

                implementation(projects.core.appInfo)
            }
        }

        iosMain.dependencies {
        }
    }
}
