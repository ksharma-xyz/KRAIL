package xyz.ksharma.krail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for KMP Android libraries using the new android-kotlin-multiplatform-library plugin.
 *
 * Usage in module build.gradle.kts:
 * ```
 * plugins {
 *     alias(libs.plugins.krail.android.kmp.library)
 * }
 *
 * kotlin {
 *     androidLibrary {
 *         namespace = "xyz.ksharma.krail.module.name"
 *         compileSdk = AndroidVersion.COMPILE_SDK
 *         minSdk = AndroidVersion.MIN_SDK
 *     }
 * }
 * ```
 */
class AndroidKmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // Apply the new KMP Android library plugin
                apply("com.android.kotlin.multiplatform.library")
                // Apply detekt for code quality
                apply("io.gitlab.arturbosch.detekt")
            }

            // Configure detekt
            configureDetekt()

            // Test-infrastructure guardrails (silent dead tests, test code leaking into
            // production, ad-hoc duplicated fakes). Pure verification, no behaviour change.
            configureTestWiringVerification()

            // Note: androidLibrary configuration must be done in the module's build.gradle.kts
            // inside the kotlin {} block, as it uses KMP DSL which can't be easily configured here
        }
    }
}

