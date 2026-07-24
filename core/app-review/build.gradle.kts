import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.appreview"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.play.review)
                api(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.analytics)
                implementation(projects.core.log)
                implementation(projects.core.remoteConfig)
                implementation(projects.platform.ops)
                implementation(projects.sandook)

                implementation(libs.compose.runtime)
                api(libs.di.koinComposeViewmodel)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
            dependencies {
                implementation(projects.core.testing)
                implementation(libs.test.kotlin)
                implementation(libs.test.robolectric)
            }
        }
    }
}
