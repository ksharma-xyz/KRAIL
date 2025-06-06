import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

android {
    namespace = "xyz.ksharma.krail"

    defaultConfig {
        applicationId = "xyz.ksharma.krail"
        versionCode = 92
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            ndk {
                isDebuggable = true
                debugSymbolLevel = "FULL"
            }
            packaging {
                jniLibs {
                    keepDebugSymbols += "**/*.so"
                }
            }
        }

        release {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            ndk {
                isDebuggable = false
                debugSymbolLevel = "FULL"
            }
            packaging {
                jniLibs {
                    keepDebugSymbols += "**/*.so"
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.krail.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlyticsPlugin)
    alias(libs.plugins.firebase.performancePlugin)
    alias(libs.plugins.detekt)
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KrailApp"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=xyz.ksharma.krail")
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(compose.preview)
                implementation(libs.activity.compose)
                implementation(compose.foundation)
                implementation(libs.core.ktx)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.lifecycle.runtime.ktx)
                api(libs.di.koinAndroid)
            }
        }

        commonMain.dependencies {
            implementation(projects.core.analytics)
            implementation(projects.core.appInfo)
            implementation(projects.core.appStart)
            implementation(projects.core.coroutinesExt)
            implementation(projects.core.di)
            implementation(projects.core.log)
            implementation(projects.core.network)
            implementation(projects.core.remoteConfig)
            implementation(projects.feature.parkRide.network)
            implementation(projects.feature.tripPlanner.network)
            implementation(projects.feature.tripPlanner.state)
            implementation(projects.feature.tripPlanner.ui)
            implementation(projects.io.gtfs)
            implementation(projects.platform.ops)
            implementation(projects.sandook)
            implementation(projects.taj)

            implementation(libs.navigation.compose)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            api(libs.di.koinComposeViewmodel)
            implementation(libs.firebase.gitLiveCrashlytics)
            implementation(libs.firebase.gitLiveAnalytics)
            implementation(libs.firebase.gitLivePerformance)
        }
    }
}

dependencies {
    implementation(projects.sandook)
    // Required when using Firebase GitLive RemoteConfig.
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
    config.setFrom("$projectDir/config/detekt.yml") // point to your custom config defining rules to run, overwriting default behavior
    baseline = file("$projectDir/config/baseline.xml") // a way of suppressing issues before introducing detekt
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin"
    )
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_17.target
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = JvmTarget.JVM_17.target
}
