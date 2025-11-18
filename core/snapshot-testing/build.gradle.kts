plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "xyz.ksharma.krail.core.snapshot"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

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
                // Annotation module (lightweight, no test dependencies)
                api(projects.core.snapshotTestingAnnotations)

                // Required for annotation to be visible
                implementation(compose.runtime)
            }
        }

        androidMain {
            dependencies {
                // Compose dependencies
                implementation(compose.ui)
                implementation(compose.foundation)

                // Required for @Preview support
                api(libs.androidx.ui.tooling)

                // Roborazzi for screenshot testing (needed by SnapshotConfig)
                api(libs.roborazzi)
                api(libs.roborazzi.compose)
                api(libs.roborazzi.junit)

                // ComposablePreviewScanner (needed by BaseSnapshotTest)
                // Using common scanner to support previews in commonMain
                api(libs.preview.scanner.common)
                api(libs.preview.scanner.android)

                // Robolectric for running tests (needed by BaseSnapshotTest)
                api(libs.test.robolectric)

                // Compose test rule (needed by SnapshotTestHelper)
                api(libs.test.composeUiTestJunit4)

                // Testing framework (needed by BaseSnapshotTest)
                api(libs.test.junit)
            }
        }
    }
}
