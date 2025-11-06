android {
    namespace = "xyz.ksharma.krail.core.debug"
}

plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "debugconfig"
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(libs.di.koinAndroid)
        }

        commonMain {
            dependencies {
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.core.network)
                implementation(projects.sandook)
                implementation(projects.taj)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                api(libs.di.koinComposeViewmodel)
            }
        }
    }
}
