android {
    namespace = "xyz.ksharma.krail.core.navigation"
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
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)

                implementation(compose.runtime)
            }
        }

        iosMain {
            dependencies {
            }
        }

    }
}
