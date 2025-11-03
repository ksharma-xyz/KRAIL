plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "xyz.ksharma.krail.core.appstart"
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
        commonMain {
            dependencies {
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.core.remoteConfig)
                implementation(projects.io.gtfs)
                implementation(projects.sandook)

                implementation(compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
