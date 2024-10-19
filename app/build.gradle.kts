plugins {
    alias(libs.plugins.krail.android.application)
    alias(libs.plugins.krail.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "xyz.ksharma.krail"

    defaultConfig {
        applicationId = "xyz.ksharma.krail"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isDebuggable = false

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                // Includes the default ProGuard rules files that are packaged with
                // the Android Gradle plugin. To learn more, go to the section about
                // R8 configuration files.
                getDefaultProguardFile("proguard-android-optimize.txt"),

                // Includes a local, custom Proguard rules file
                "proguard-rules.pro"
            )


            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    hilt {
        enableAggregatingTask = true
    }
}

dependencies {

    // Projects
    implementation(projects.core.designSystem)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.utils)
    implementation(projects.feature.sydneyTrains.database.api)
    implementation(projects.feature.sydneyTrains.database.real)
    implementation(projects.feature.sydneyTrains.network.api)
    implementation(projects.feature.sydneyTrains.network.real)
    implementation(projects.feature.tripPlanner.network.api)
    implementation(projects.feature.tripPlanner.network.real)
    implementation(projects.feature.tripPlanner.ui)
    implementation(projects.sandook.api)
    implementation(projects.sandook.real)

    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.navigation)
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.timber)

    androidTestImplementation(libs.test.androidxTestExtJunit)
    testImplementation(libs.test.composeUiTestJunit4)
    testImplementation(libs.test.paparazzi)
}
