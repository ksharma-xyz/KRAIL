android {
    namespace = "xyz.ksharma.krail.io.gtfs"
}

plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            api(libs.di.koinAndroid)
        }

        commonMain {
            dependencies {
                implementation(projects.core.log)
                implementation(projects.core.di)
                implementation(projects.sandook)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.datetime)
                implementation(compose.runtime)
                implementation(compose.components.resources)

                api(libs.di.koinComposeViewmodel)
            }
        }

        iosMain {
            dependencies {
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.test.turbine)
                implementation(libs.test.kotlinxCoroutineTest)
            }
        }
    }
}


wire {
    kotlin {
        javaInterop = true
        out = "$projectDir/build/generated/source/wire"
    }
    protoPath {
        srcDir("src/commonMain/proto")
    }
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}
