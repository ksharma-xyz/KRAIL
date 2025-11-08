plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "xyz.ksharma.krail.taj"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
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
                implementation(compose.animation)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.foundation)
                api(compose.material3)
                implementation(compose.runtime)
                implementation(compose.ui)

                implementation(libs.material.icons.core)

                // Snapshot testing annotation
                implementation(projects.core.snapshotTesting)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                // Snapshot testing infrastructure
                implementation(projects.core.snapshotTesting)
                implementation(libs.androidx.ui.tooling)

                // Roborazzi for screenshot testing
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
                implementation(libs.roborazzi.junit)

                // ComposablePreviewScanner for scanning @Preview annotations
                implementation(libs.preview.scanner.android)

                // Robolectric for running tests
                implementation(libs.test.robolectric)

                // Testing framework
                implementation(libs.test.junit)
                implementation(libs.test.kotlin)
                implementation(libs.test.composeUiTestJunit4)
            }
        }
    }
}

dependencies {
    // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-org.jetbrains.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio#focus=Comments-27-11400795.0-0Add commentMore actions
    debugImplementation(libs.androidx.ui.tooling)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.krail.taj.resources"
    generateResClass = auto
}
