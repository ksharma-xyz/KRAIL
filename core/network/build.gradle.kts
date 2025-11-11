import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec

android {
    namespace = "xyz.ksharma.krail.core.network"
}

plugins {
    alias(libs.plugins.krail.android.library)
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            api(libs.di.koinAndroid)
        }

        commonMain {
            dependencies {
                implementation(projects.core.appInfo)
                implementation(projects.core.di)
                implementation(projects.core.log)
                implementation(projects.sandook)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(compose.runtime)

                api(libs.di.koinComposeViewmodel)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
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


// READ API KEY
val localProperties = gradleLocalProperties(rootProject.rootDir, providers)

// Check if we're in CI environment running Detekt only
val isDetektOnlyBuild = gradle.startParameter.taskNames.any { it.contains("detekt", ignoreCase = true) }
val isCIEnvironment = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"

val androidNswTransportApiKey: String = localProperties.getProperty("ANDROID_NSW_TRANSPORT_API_KEY")
    ?: System.getenv("ANDROID_NSW_TRANSPORT_API_KEY")
    ?: ""

val iosNswTransportApiKey: String = localProperties.getProperty("IOS_NSW_TRANSPORT_API_KEY")
    ?: System.getenv("IOS_NSW_TRANSPORT_API_KEY")
    ?: ""

// Only require API keys for non-CI builds or non-Detekt builds
if (!(isCIEnvironment && isDetektOnlyBuild)) {
    require(androidNswTransportApiKey.isNotEmpty()) {
        "Register API key and put in local.properties as `ANDROID_NSW_TRANSPORT_API_KEY`"
    }
    require(iosNswTransportApiKey.isNotEmpty()) {
        "Register API key and put in local.properties as `IOS_NSW_TRANSPORT_API_KEY`"
    }
}

buildkonfig {
    packageName = "xyz.ksharma.krail.core.network"
    exposeObjectWithName = "NetworkBuildKonfig"

    defaultConfigs {
        // Use placeholder values for CI Detekt builds, real values otherwise
        val androidKey = if (isCIEnvironment && isDetektOnlyBuild && androidNswTransportApiKey.isEmpty()) {
            "placeholder-android-key"
        } else {
            require(androidNswTransportApiKey.isNotEmpty()) {
                "Register API key and put in local.properties as `ANDROID_NSW_TRANSPORT_API_KEY`"
            }
            androidNswTransportApiKey
        }

        val iosKey = if (isCIEnvironment && isDetektOnlyBuild && iosNswTransportApiKey.isEmpty()) {
            "placeholder-ios-key"
        } else {
            require(iosNswTransportApiKey.isNotEmpty()) {
                "Register API key and put in local.properties as `IOS_NSW_TRANSPORT_API_KEY`"
            }
            iosNswTransportApiKey
        }

        buildConfigField(FieldSpec.Type.STRING, "ANDROID_NSW_TRANSPORT_API_KEY", androidKey)
        buildConfigField(FieldSpec.Type.STRING, "IOS_NSW_TRANSPORT_API_KEY", iosKey)
    }
}
