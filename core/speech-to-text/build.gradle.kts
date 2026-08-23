import io.github.frankois944.spmForKmp.swiftPackageConfig
import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.spmForKmp)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.speechtotext"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
        withHostTest {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.swiftPackageConfig(cinteropName = "speechBridge") {}
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                api(libs.di.koinAndroid)
                implementation(libs.core.ktx)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.aagya.data)
                api(libs.di.koinComposeViewmodel)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.kotlinxCoroutineTest)
                implementation(libs.test.robolectric)
            }
        }
    }
}
