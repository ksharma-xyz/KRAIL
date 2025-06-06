package xyz.ksharma.krail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("io.gitlab.arturbosch.detekt")
            }

            configureAndroid()
            configureDetekt()
        }
    }
}
