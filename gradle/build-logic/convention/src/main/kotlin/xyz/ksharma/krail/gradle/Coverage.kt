package xyz.ksharma.krail.gradle

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import java.io.File

/**
 * Kover coverage wiring.
 *
 * Every module with production code gets the Kover plugin and is registered into the root
 * project's `kover` configuration, so `./gradlew koverHtmlReport` at the root produces one merged
 * report across the whole repo rather than 60-odd per-module ones nobody reads.
 *
 * **Modules with no tests are included on purpose.** They used to be left out, on the argument
 * that reporting them as 0% would drown the signal from modules that do have suites. That is a
 * readability argument doing duty as a measurement one: it computed the repo-wide percentage over
 * the friendly half of the repo. A module with no tests is exactly the thing an honest number
 * should show, and `verifyTestWiring` is what stops a module quietly having none.
 *
 * The exclusion filters live here, and are applied to **both** each module's own reports and the
 * root's merged report, so `./gradlew :taj:koverLog` and the taj slice of the aggregate mean the
 * same thing. They previously sat in the root `build.gradle.kts` only, which left every per-module
 * number computed over a different set of classes.
 *
 * Coverage is **host-only**. Kover instruments JVM bytecode, so it measures `testAndroidHostTest`
 * and has nothing to say about `iosSimulatorArm64Test` — Kotlin/Native is not supported upstream
 * and cannot be made to work from here. `commonMain` *is* measured, through the Android host run;
 * only `iosMain` is invisible. See `docs/testing/COVERAGE.md`.
 */
private const val KOVER_PLUGIN_ID = "org.jetbrains.kotlinx.kover"

/** Marks the root as configured, so the merged-report setup runs once rather than per module. */
private const val ROOT_CONFIGURED_FLAG = "krail.coverage.rootConfigured"

/** Source roots that mean this module has production code worth measuring. */
private val PRODUCTION_SOURCE_DIRS = listOf("commonMain", "androidMain", "iosMain")

/**
 * Floor for the merged report, checked by `koverVerify` at the root.
 *
 * Two points under the measured baseline (63.48% across 58 modules, with the exclusions below),
 * so it catches a regression — a deleted suite, or a large untested surface landing — and never
 * blocks ordinary work. It is not a target.
 */
private const val MERGED_COVERAGE_FLOOR = 61

/**
 * Per-module floors, same rule: two points under what each currently measures.
 *
 * Measured 2026-08-21: departures:ui 89.12%, track:ui 84.54%, core:date-time 70.27%.
 *
 * Raising one is a deliberate act, in its own commit, quoting the new measurement. Lowering one
 * requires saying what was given up.
 */
private val MODULE_COVERAGE_FLOORS = mapOf(
    ":feature:departures:ui" to 87,
    ":feature:track:ui" to 82,
    ":core:date-time" to 68,
)

fun Project.configureCoverage() {
    configureMergedCoverageOnce()

    // :core:testing is the fakes module. Its production code *is* test infrastructure, exercised
    // by every other module's suite, so a coverage number for it says nothing about the app.
    if (path == TESTING_MODULE_PATH) return
    if (!hasProductionSources()) return
    if (pluginManager.hasPlugin(KOVER_PLUGIN_ID)) return

    pluginManager.apply(KOVER_PLUGIN_ID)

    extensions.configure<KoverProjectExtension> {
        reports {
            filters { excludes { applyKrailExclusions() } }

            MODULE_COVERAGE_FLOORS[path]?.let { floor ->
                verify {
                    rule("Line coverage for $path") {
                        bound { minValue.set(floor) }
                    }
                }
            }
        }
    }

    // Register into the root aggregate. The root applies Kover in its own build file, so the
    // `kover` configuration already exists by the time subprojects are configured.
    rootProject.dependencies.add("kover", this)
}

/** Applies the same exclusions and a floor to the root's merged report. Idempotent. */
private fun Project.configureMergedCoverageOnce() {
    val root = rootProject
    if (root.extra.has(ROOT_CONFIGURED_FLAG)) return
    if (!root.pluginManager.hasPlugin(KOVER_PLUGIN_ID)) return
    root.extra.set(ROOT_CONFIGURED_FLAG, true)

    root.extensions.configure<KoverProjectExtension> {
        reports {
            filters { excludes { applyKrailExclusions() } }
            verify {
                rule("Merged line coverage") {
                    bound { minValue.set(MERGED_COVERAGE_FLOOR) }
                }
            }
        }
    }
}

/**
 * The one definition of "code a coverage percentage should not be computed over".
 *
 * Everything here is either generated, or a declaration whose execution proves nothing. Nothing is
 * excluded for merely being inconvenient to test — that would make the number less truthful, which
 * is the opposite of the point.
 */
private fun KoverReportFilter.applyKrailExclusions() {
    // --- Compose ---------------------------------------------------------------------------
    // A @Composable is executed by any snapshot test that renders it, marking every line covered
    // while asserting nothing beyond "it did not crash". Counting them means the number rises when
    // a preview is added rather than when behaviour is checked. What stays measured is the code
    // where a wrong branch is a bug you can write an assertion about: reducers, mappers,
    // repositories, ranking, date handling.
    annotatedBy("androidx.compose.runtime.Composable")
    // The Compose compiler emits one of these per file holding @Composable lambdas.
    classes("*.ComposableSingletons*")

    // --- Codegen ---------------------------------------------------------------------------
    // SQLDelight. The generated queries and table classes have their own package now; they used to
    // share `xyz.ksharma.krail.sandook` with the hand-written repositories, so no package filter
    // could separate them and all but one generated file counted as covered production code. The
    // repositories above this are measured.
    packages("xyz.ksharma.krail.sandook.db")
    // Wire, from the .proto definitions behind :io:bff-api and :io:gtfs.
    packages("app.krail.bff.proto", "app.krail.kgtfs.proto", "com.google.transit.realtime")
    // BuildKonfig.
    classes("*.BuildKonfig")
    // Compose Resources accessors (`Res.drawable.*`).
    packages("*.generated.resources")

    // --- Declarations, not logic -----------------------------------------------------------
    // Koin module declarations. A `module { single { … } }` block is a wiring list; executing it
    // proves the graph parses, which every test that starts Koin already proves.
    classes("*ModuleKt", "*.di.*Kt")
    // expect/actual wrappers around an Android SDK service. A host test cannot drive the real
    // service, so these report as uncovered no matter what anyone writes.
    classes("*.Android*Service")
    // AnalyticsEvent is a sealed hierarchy of data holders. Kotlin generates componentN, copy,
    // equals, hashCode and toString on each; none is hand-written and none can be wrong.
    classes("*.AnalyticsEvent\$*")
}

private fun Project.hasProductionSources(): Boolean = PRODUCTION_SOURCE_DIRS.any { dir ->
    File(projectDir, "src/$dir").containsKotlinFile()
}

private fun File.containsKotlinFile(): Boolean =
    isDirectory && walkTopDown().any { it.isFile && it.extension == "kt" }
