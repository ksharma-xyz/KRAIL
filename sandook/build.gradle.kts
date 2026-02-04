import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.sandook"
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
                implementation(libs.db.sqlAndroidDriver)
                implementation(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(libs.kotlinx.serialization.json)
                
                implementation(libs.compose.runtime)
                implementation(libs.kotlinx.datetime)
                implementation(libs.db.sqlRuntime)
                implementation(libs.db.sqlCoroutinesExtensions)

                api(libs.di.koinComposeViewmodel)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            api(libs.db.sqlNativeDriver)
        }
    }
}

sqldelight {
    databases {
        create("KrailSandook") {
            packageName.set("xyz.ksharma.krail.sandook")
            verifyMigrations.set(true)
        }
    }
}
