package xyz.ksharma.krail.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureAndroid() {
    // In AGP 9.0, we use CommonExtension::class.java to ensure
    // we are targeting the new interface-driven DSL.
    extensions.configure(CommonExtension::class.java) {
        compileSdk = AndroidVersion.COMPILE_SDK

    }

    // targetSdk is handled separately because it's not in the base CommonExtension
    extensions.findByName("android")?.let { ext ->
        when (ext) {
            is ApplicationExtension -> ext.defaultConfig.targetSdk = AndroidVersion.TARGET_SDK
            is LibraryExtension -> ext.testOptions.targetSdk = AndroidVersion.TARGET_SDK
        }
    }
}

object AndroidVersion {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 28
    const val TARGET_SDK = 36
}