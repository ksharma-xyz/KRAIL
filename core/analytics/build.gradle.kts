import xyz.ksharma.krail.gradle.AndroidVersion
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test as GradleTest

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
                // Firebase BOM needed for GitLive Firebase Android platform dependencies
                implementation(project.dependencies.platform(libs.firebase.bom))
                api(libs.di.koinAndroid)
            }
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.compose.runtime)
                api(libs.di.koinComposeViewmodel)
                implementation(libs.firebase.gitLiveAnalytics)
                implementation(libs.firebase.gitLiveRemoteConfig)

                implementation(projects.core.appInfo)
                implementation(projects.core.log)
            }
        }

        iosMain.dependencies {
        }

        // AnalyticsContractTest instantiates every AnalyticsEvent subclass to read its
        // real property map, so it needs full JVM reflection — hence host test, not common.
        getByName("androidHostTest") {
            kotlin.srcDir("src/androidHostTest/kotlin")
            dependencies {
                implementation(libs.test.kotlin)
                implementation(libs.kotlinx.serialization.json)
                implementation(kotlin("reflect"))
            }
        }
    }
}

// AnalyticsContractTest can rewrite analytics-events.json from the code when this
// property is set. Gradle applies -D to its own JVM, so it has to be forwarded to the
// test JVM explicitly:
//   ./gradlew :core:analytics:testAndroidHostTest -DregenerateAnalyticsContract=1
tasks.withType<GradleTest>().configureEach {
    workingDir = projectDir
    providers.systemProperty("regenerateAnalyticsContract").orNull?.let {
        systemProperty("regenerateAnalyticsContract", it)
    }
    // The test reads analytics-events.json at runtime via File(), so Gradle cannot infer
    // it as an input. Without this, a JSON-only change is reported UP-TO-DATE / FROM-CACHE
    // and the contract test does NOT run — the exact case of editing only the contract.
    // Declaring it forces a re-run whenever the contract changes.
    inputs.file(layout.projectDirectory.file("analytics-events.json"))
        .withPropertyName("analyticsContract")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
