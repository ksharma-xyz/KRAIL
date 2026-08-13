package xyz.ksharma.krail.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * One-line snapshot-testing adoption for KRAIL modules.
 *
 * Before:
 * ```
 * plugins {
 *     alias(libs.plugins.roborazzi)            // 1
 *     // …other plugins
 * }
 * kotlin {
 *     sourceSets {
 *         commonMain.dependencies {
 *             implementation(projects.core.snapshotTestingAnnotations)   // 2
 *         }
 *         getByName("androidHostTest").dependencies {
 *             implementation(projects.core.snapshotTesting)              // 3
 *         }
 *     }
 * }
 * ```
 *
 * After:
 * ```
 * plugins {
 *     alias(libs.plugins.krail.snapshot.testing)                          // single line
 *     // …other plugins
 * }
 * ```
 *
 * Still required in the module (intentionally — the convention plugins stay thin and
 * don't touch `androidLibrary {}`):
 * ```
 * androidLibrary {
 *     withHostTest {
 *         isIncludeAndroidResources = true        // Roborazzi needs Android resources.
 *     }
 *     androidResources {
 *         enable = true                           // MANDATORY for AGP 9.
 *     }
 * }
 * sourceSets {
 *     getByName("androidHostTest") {
 *         kotlin.srcDir("src/androidHostTest/kotlin")  // explicit srcDir for clarity
 *     }
 * }
 * ```
 *
 * Then write `<Module>SnapshotTest.kt` extending `BaseSnapshotTest`, annotate the
 * `@PreviewComponent` / `@PreviewScreen` previews with `@ScreenshotTest`, and run
 * `./gradlew :module:recordRoborazziAndroidHostTest` to capture goldens.
 */
class SnapshotTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            // Default JVM test heap (usually ~512m-1g) isn't enough once a module accumulates
            // a few hundred @ScreenshotTest previews in one process — capture is Robolectric +
            // Skia bitmap rendering held in memory across the whole `generateSnapshots()` run,
            // not released between previews. Confirmed via `feature:trip-planner:ui` (280+
            // previews) hitting `java.lang.OutOfMemoryError` at the default heap, reproducibly,
            // even on a freshly restarted Gradle daemon.
            tasks.withType(Test::class.java).configureEach {
                maxHeapSize = "4g"
            }

            // Wire the deps once the KMP extension is configured (which happens after the
            // module's `plugins { alias(libs.plugins.krail.kotlin.multiplatform) }` block).
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.named("commonMain") {
                        dependencies {
                            implementation(project(":core:snapshot-testing-annotations"))
                        }
                    }
                    // androidHostTest source set is created by `withHostTest {}` in the
                    // module's androidLibrary block — match by name (`configureEach` is
                    // lazy, so it fires whether the source set exists yet or not).
                    sourceSets.configureEach {
                        if (name == "androidHostTest") {
                            dependencies {
                                implementation(project(":core:snapshot-testing"))
                            }
                        }
                    }
                }
            }
        }
    }
}
