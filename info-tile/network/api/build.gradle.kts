plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "xyz.ksharma.krail.info.tile.network.api"
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
        commonMain  {
            dependencies {
                implementation(projects.core.log)
                implementation(projects.infoTile.state)
                implementation(projects.sandook)
                implementation(projects.social.network.api)

                implementation(compose.runtime)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
