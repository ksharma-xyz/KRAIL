import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import xyz.ksharma.krail.gradle.AndroidVersion

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.krail.android.kmp.library)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "xyz.ksharma.krail.core.network"
        compileSdk = AndroidVersion.COMPILE_SDK
        minSdk = AndroidVersion.MIN_SDK
    }
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

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.compose.runtime)

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

// CI opts into the API-key bypass via `-PciQuality=true` (passed by
// code-quality.yml). Detekt is static analysis and host tests use Fake* services
// that never hit the network, so neither needs real keys; placeholder values
// further down satisfy buildkonfig's codegen.
//
// Replaces an earlier `gradle.startParameter.taskNames.contains(...)` heuristic
// that silently broke whenever a task got renamed or a new no-network task was
// added — explicit property is the contract.
val isCIQualityCheck = providers.gradleProperty("ciQuality")
    .orElse(providers.environmentVariable("KRAIL_CI_QUALITY"))
    .map { it.toBoolean() }
    .getOrElse(false)
val isCIEnvironment = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"

val androidNswTransportApiKey: String = localProperties.getProperty("ANDROID_NSW_TRANSPORT_API_KEY")
    ?: System.getenv("ANDROID_NSW_TRANSPORT_API_KEY")
    ?: ""

val iosNswTransportApiKey: String = localProperties.getProperty("IOS_NSW_TRANSPORT_API_KEY")
    ?: System.getenv("IOS_NSW_TRANSPORT_API_KEY")
    ?: ""

// Optional local KRAIL-BFF override. Empty / missing means no override; the
// app uses the NSW direct path (production behavior). Set
// `krail.bffBaseUrl=http://10.0.2.2:8080` in local.properties to opt in.
val krailBffBaseUrl: String = localProperties.getProperty("krail.bffBaseUrl")
    ?: ""

// Optional production KRAIL-BFF URL. Empty / missing represents
// "BFF prod not yet deployed" — the runtime resolver falls back to NSW direct
// in that case so a `BFF_PROD` selection in the debug-settings UI cannot break
// the app before the BFF actually ships.
val krailBffProdBaseUrl: String = localProperties.getProperty("krail.bffProdBaseUrl")
    ?: ""

// Only require API keys when CI is doing a real build that ships code; quality
// checks (detekt, host tests) get placeholders below.
if (!(isCIEnvironment && isCIQualityCheck)) {
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
        // Placeholder values for CI quality-check runs (detekt + host tests),
        // real values for everything else.
        val androidKey = if (isCIEnvironment && isCIQualityCheck && androidNswTransportApiKey.isEmpty()) {
            "placeholder-android-key"
        } else {
            require(androidNswTransportApiKey.isNotEmpty()) {
                "Register API key and put in local.properties as `ANDROID_NSW_TRANSPORT_API_KEY`"
            }
            androidNswTransportApiKey
        }

        val iosKey = if (isCIEnvironment && isCIQualityCheck && iosNswTransportApiKey.isEmpty()) {
            "placeholder-ios-key"
        } else {
            require(iosNswTransportApiKey.isNotEmpty()) {
                "Register API key and put in local.properties as `IOS_NSW_TRANSPORT_API_KEY`"
            }
            iosNswTransportApiKey
        }

        buildConfigField(FieldSpec.Type.STRING, "ANDROID_NSW_TRANSPORT_API_KEY", androidKey)
        buildConfigField(FieldSpec.Type.STRING, "IOS_NSW_TRANSPORT_API_KEY", iosKey)
        buildConfigField(FieldSpec.Type.STRING, "KRAIL_BFF_BASE_URL", krailBffBaseUrl)
        buildConfigField(FieldSpec.Type.STRING, "KRAIL_BFF_PROD_BASE_URL", krailBffProdBaseUrl)
    }
}
