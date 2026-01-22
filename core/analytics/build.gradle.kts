import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.analytics"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
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
                // Firebase BOM needed for GitLive Firebase Android platform dependencies
                implementation(project.dependencies.platform(libs.firebase.bom))
                api(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.firebase.gitLiveAnalytics)
                implementation(libs.firebase.gitLiveRemoteConfig)

                implementation(projects.core.appInfo)
                implementation(projects.core.log)
            }
        }

        iosMain.dependencies {
        }
    }
}
