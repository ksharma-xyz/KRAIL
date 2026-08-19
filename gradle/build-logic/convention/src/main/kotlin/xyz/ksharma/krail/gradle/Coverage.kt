package xyz.ksharma.krail.gradle

import org.gradle.api.Project
import java.io.File

/**
 * Kover coverage wiring.
 *
 * Every module that has test sources gets the Kover plugin and is added to the root project's
 * `kover` configuration, so `./gradlew koverHtmlReport` at the root produces one merged report
 * across the whole repo rather than 31 per-module ones nobody reads. Applying it from here
 * rather than from each `build.gradle.kts` keeps the list of measured modules derived from
 * "does this module actually have tests" instead of hand-maintained.
 *
 * Modules with no test sources are deliberately left out. Adding them would report their
 * production code as 0% covered, which is true but drowns the signal from the modules that do
 * have suites; `verifyTestWiring` is what stops a module from quietly having none.
 *
 * Coverage is **host-only**. Kover instruments JVM bytecode, so it measures
 * `testAndroidHostTest` and has nothing to say about `iosSimulatorArm64Test` — Kotlin/Native
 * is not supported upstream and cannot be made to work from here.
 */
private const val KOVER_PLUGIN_ID = "org.jetbrains.kotlinx.kover"

/** Source-set roots whose Kotlin files mean this module produces coverage worth merging. */
private val COVERAGE_TEST_SOURCE_DIRS = listOf("commonTest", "androidHostTest", "androidUnitTest")

fun Project.configureCoverage() {
    // :core:testing is the fakes module. Its production code *is* test infrastructure, so a
    // coverage number for it says nothing about the app.
    if (path == TESTING_MODULE_PATH) return
    if (!hasTestSources()) return
    if (pluginManager.hasPlugin(KOVER_PLUGIN_ID)) return

    pluginManager.apply(KOVER_PLUGIN_ID)

    // Register into the root aggregate. The root applies Kover in its own build file, so the
    // `kover` configuration already exists by the time subprojects are configured.
    rootProject.dependencies.add("kover", this)
}

private fun Project.hasTestSources(): Boolean = COVERAGE_TEST_SOURCE_DIRS.any { dir ->
    File(projectDir, "src/$dir").containsKotlinFile()
}

private fun File.containsKotlinFile(): Boolean =
    isDirectory && walkTopDown().any { it.isFile && it.extension == "kt" }
