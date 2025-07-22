plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
}

android {
    namespace = "xyz.ksharma.krail.discover.ui"
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
                implementation(libs.ktor.client.okhttp)
            }
        }

        commonMain  {
            dependencies {
                implementation(compose.animation)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.networkKtor)
                implementation(libs.kotlinx.collections.immutable)

                implementation(projects.core.log)
                implementation(projects.discover.network.api)
                implementation(projects.taj)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

dependencies {
    // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-org.jetbrains.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio#focus=Comments-27-11400795.0-0Add commentMore actions
    debugImplementation(libs.androidx.ui.tooling)
}
