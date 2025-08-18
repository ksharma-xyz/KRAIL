plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.core.log"
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
                implementation(libs.log.slf4j.android)
            }
        }

        androidUnitTest {
            dependencies {
                implementation(libs.test.kotlin)
            }
        }

        commonMain {
            dependencies {
                implementation(compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.log.kermit)
            }
        }

        iosMain.dependencies {
        }
    }
}
