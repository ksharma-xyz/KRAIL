plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.core.remoteconfig"
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
                implementation(projects.core.appInfo)
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.di)
                implementation(projects.core.log)

                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.firebase.gitLiveRemoteConfig)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }
    }
}
