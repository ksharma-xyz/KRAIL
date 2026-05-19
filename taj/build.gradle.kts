import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.snapshot.testing)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.taj"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        withHostTest {
            isIncludeAndroidResources = true
        }

        // MANDATORY for AGP 9 to include assets
        androidResources {
            enable = true
        }
    }

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        commonMain  {
            dependencies {
                implementation(libs.compose.animation)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.foundation)
                api(libs.compose.material3)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)

                implementation(libs.material.icons.core)
                implementation(libs.compose.ui.tooling.preview)

                // `:core:snapshot-testing-annotations` is added by `krail.snapshot.testing`.
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }

        // `:core:snapshot-testing` is added by `krail.snapshot.testing`; this srcDir
        // declaration is kept explicit so the host-test sources are unambiguous.
        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
        }

        androidMain.dependencies {
            // https://youtrack.jetbrains.com/issue/KTIJ-32720/Support-common-androidx.compose.ui.tooling.preview.Preview-in-IDEA-and-Android-Studio#focus=Comments-27-11400795.0-0Add commentMore actions
            implementation(libs.androidx.ui.tooling)
        }

        iosMain.dependencies {
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.krail.taj.resources"
    generateResClass = auto
}
