plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "xyz.ksharma.krail.sandook"
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
                implementation(libs.db.sqlAndroidDriver)
                implementation(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)
                implementation(libs.kotlinx.serialization.json)
                
                implementation(compose.runtime)
                implementation(libs.kotlinx.datetime)
                implementation(libs.db.sqlRuntime)
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
