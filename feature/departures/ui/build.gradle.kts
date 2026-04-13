import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.krail.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.departures.ui"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DeparturesUI"
            isStatic = true
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.coroutinesExt)
                implementation(projects.core.dateTime)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.core.transport)
                implementation(projects.feature.departures.network)
                implementation(projects.feature.departures.state)

                api(libs.di.koinComposeViewmodel)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
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
