android {
    namespace = "xyz.ksharma.krail.platform.ops"
}

plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            api(libs.di.koinAndroid)
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)
                implementation(projects.core.di)

                implementation(compose.runtime)
                implementation(compose.components.resources)

                api(libs.di.koinComposeViewmodel)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.turbine)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }
    }
}
