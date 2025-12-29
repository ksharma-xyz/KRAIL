plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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
                implementation(projects.core.analytics)
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.sandook)
                implementation(projects.taj)

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.animation)
                implementation(compose.foundation)
                implementation(compose.ui)

                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.lifecycle.viewmodel.compose)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.turbine)
            }
        }
    }
}

android {
    namespace = "xyz.ksharma.krail.park.ride.ui"
}

dependencies {
    // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-org.jetbrains.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio#focus=Comments-27-11400795.0-0Add commentMore actions
    debugImplementation(libs.androidx.ui.tooling)
}
