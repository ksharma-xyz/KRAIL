package xyz.ksharma.krail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // The new KMP-specific Android plugin
                apply("com.android.kotlin.multiplatform.library")
                apply("io.gitlab.arturbosch.detekt")

                // IMPORTANT: Do NOT apply "org.jetbrains.kotlin.android" here.
                // AGP 9 handles Kotlin natively.
            }

            // Call the fixed configuration extension
            configureAndroid()

            // Your existing detekt setup
            configureDetekt()
        }
    }
}