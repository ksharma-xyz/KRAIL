package xyz.ksharma.krail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {

        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform") // Kotlin Multiplatform plugin - JVM, Android, iOS, JS
        }

        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()

            // Note: androidLibrary configuration (namespace, compileSdk, minSdk) must be done
            // in each module's build.gradle.kts as it varies per module
            // The androidLibrary block is provided by the android-kotlin-multiplatform-library plugin

            iosArm64()
            iosSimulatorArm64()

            targets.withType<KotlinNativeTarget>().configureEach {
                binaries.configureEach {
                    // Add linker flag for SQLite. See:
                    // https://github.com/touchlab/SQLiter/issues/77
                    // required for iOS targets
                    linkerOpts("-lsqlite3")
                }
            }

            configureJava()
        }
    }
}
