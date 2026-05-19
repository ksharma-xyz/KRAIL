package xyz.ksharma.krail.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * KRAIL test-infrastructure guardrails. These are pure verification tasks (no behaviour
 * change) registered from the KMP / Android-KMP convention plugins and wired into `check`.
 *
 * They exist because three structural defects shipped silently:
 *
 *  1. **Silent dead tests** — a module had `src/commonTest` files but no `withHostTest {}`,
 *     so the KMP Android plugin never created `testAndroidHostTest` and CI never ran them.
 *     [VerifyTestWiringTask] makes that fail fast instead of passing green.
 *
 *  2. **Test code leaking into production** — fakes live in a `:core:testing` module that
 *     must only ever be consumed via `testImplementation`/host-test configs. If a `main`
 *     source set ever resolves it, fakes ship in the app. [VerifyTestingModuleUsageTask]
 *     forbids that and also breaks the `:core:testing -> feature -> :core:testing` cycle.
 *
 *  3. **Ad-hoc duplicated fakes** — boundary interfaces (`Sandook`, `Analytics`, `*Service`,
 *     `Flag`, `RemoteConfig`) were re-faked inline per test as anonymous `object : X { }`
 *     stubs or copy-pasted `private class Fake*`. [VerifyNoAdHocBoundaryFakesTask] points
 *     contributors at the canonical `:core:testing` fakes instead.
 */
internal const val TESTING_MODULE_PATH = ":core:testing"

private const val HOST_TEST_TASK = "testAndroidHostTest"

// Source-set roots that, if they contain Kotlin files, require a host test task.
private val TEST_SOURCE_DIRS = listOf("commonTest", "androidHostTest", "androidUnitTest")

fun Project.configureTestWiringVerification() {
    // Idempotent: both KotlinMultiplatform and AndroidKmpLibrary convention plugins call
    // this, and most modules apply both. Register once per project.
    if (tasks.findByName(VerifyTestWiringTask.NAME) != null) return

    val verifyWiring = tasks.register(VerifyTestWiringTask.NAME, VerifyTestWiringTask::class.java) {
        group = "verification"
        description = "Fails if a module has test sources but no $HOST_TEST_TASK task."
    }
    val verifyUsage = tasks.register(
        VerifyTestingModuleUsageTask.NAME,
        VerifyTestingModuleUsageTask::class.java,
    ) {
        group = "verification"
        description = "Fails if a non-test configuration depends on $TESTING_MODULE_PATH."
    }
    val verifyFakes = tasks.register(
        VerifyNoAdHocBoundaryFakesTask.NAME,
        VerifyNoAdHocBoundaryFakesTask::class.java,
    ) {
        group = "verification"
        description = "Fails if test sources re-fake a boundary interface ad-hoc."
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(verifyWiring, verifyUsage, verifyFakes)
    }
}

/**
 * Fails when a test source set has Kotlin files but the project never registered
 * [HOST_TEST_TASK] — i.e. someone forgot `withHostTest {}` in `androidLibrary {}`.
 */
abstract class VerifyTestWiringTask : DefaultTask() {

    @TaskAction
    fun verify() {
        val projectDir = project.projectDir
        val populatedTestDirs = TEST_SOURCE_DIRS.filter { name ->
            File(projectDir, "src/$name").hasKotlinSources()
        }
        if (populatedTestDirs.isEmpty()) return

        val hasHostTestTask = project.tasks.findByName(HOST_TEST_TASK) != null
        if (!hasHostTestTask) {
            throw IllegalStateException(
                buildString {
                    appendLine("Module '${project.path}' has test sources in ")
                    appendLine("  ${populatedTestDirs.joinToString { "src/$it" }}")
                    appendLine("but no '$HOST_TEST_TASK' task — these tests would NEVER run.")
                    appendLine("Add `withHostTest {}` inside the `androidLibrary {}` block of")
                    appendLine("${project.path}'s build.gradle.kts, or delete the dead test sources.")
                },
            )
        }
    }

    companion object {
        const val NAME = "verifyTestWiring"
    }
}

/**
 * Fails when any non-test configuration declares a dependency on [TESTING_MODULE_PATH].
 * Only declared dependencies are inspected (no resolution) so this is configuration-cache
 * safe and cheap. Until `:core:testing` exists this is a harmless no-op.
 */
abstract class VerifyTestingModuleUsageTask : DefaultTask() {

    @TaskAction
    fun verify() {
        val offenders = project.configurations
            .filter { config -> !config.name.containsTestToken() }
            .filter { config ->
                config.dependencies.any { dep ->
                    dep is ProjectDependency && dep.dependencyPath() == TESTING_MODULE_PATH
                }
            }
            .map { it.name }
            .sorted()

        if (offenders.isNotEmpty()) {
            throw IllegalStateException(
                buildString {
                    appendLine("Module '${project.path}' depends on $TESTING_MODULE_PATH from a")
                    appendLine("non-test configuration: ${offenders.joinToString()}.")
                    appendLine("$TESTING_MODULE_PATH holds test fakes and must only be consumed via")
                    appendLine("testImplementation / host-test configurations — never main code.")
                },
            )
        }
    }

    companion object {
        const val NAME = "verifyTestingModuleUsage"
    }
}

/**
 * Scans test sources for ad-hoc re-implementations of boundary interfaces. Contributors
 * must use the canonical configurable fakes in `:core:testing` instead of hand-rolling an
 * `object : Sandook { … error("x") }` or a copy-pasted `private class FakeFlag`.
 */
abstract class VerifyNoAdHocBoundaryFakesTask : DefaultTask() {

    @TaskAction
    fun verify() {
        // The module that *owns* the canonical fakes is exempt.
        if (project.path == TESTING_MODULE_PATH) return

        val anonStub = Regex(
            """object\s*:\s*($BOUNDARY_ALTERNATION)\b""",
        )
        val privateFake = Regex(
            """private\s+class\s+Fake($BOUNDARY_ALTERNATION)\b""",
        )

        // Grandfathered pre-existing violations — ratchet only tightens. Entries are
        // removed by the migration PRs that replace them with :core:testing fakes.
        val baseline = readBaseline(project)

        val violations = mutableListOf<String>()
        project.projectDir.resolve("src").walkTopDownSafe()
            .filter { it.isFile && it.extension == "kt" && it.isTestSource() }
            .forEach { file ->
                val relPath = file.relativeTo(project.projectDir)
                    .path.replace(File.separatorChar, '/')
                val baselined = "${project.path}|$relPath" in baseline
                val text = file.readText()
                if (!baselined &&
                    (anonStub.containsMatchIn(text) || privateFake.containsMatchIn(text))
                ) {
                    violations += relPath
                }
            }

        if (violations.isNotEmpty()) {
            throw IllegalStateException(
                buildString {
                    appendLine("Module '${project.path}' re-fakes a boundary interface ad-hoc in:")
                    violations.sorted().forEach { appendLine("  $it") }
                    appendLine("Use the canonical configurable fakes from $TESTING_MODULE_PATH")
                    appendLine("(FakeSandook, FakeAnalytics, FakeFlag, FakeTripPlanningService, …)")
                    appendLine("instead of an `object : X {}` stub or a `private class Fake*`.")
                },
            )
        }
    }

    companion object {
        const val NAME = "verifyNoAdHocBoundaryFakes"
        private const val BOUNDARY_ALTERNATION =
            "Sandook|SandookPreferences|Analytics|Flag|RemoteConfig|" +
                "[A-Za-z]*Service|[A-Za-z]*Repository"
    }
}

// ---- helpers ----

private const val BASELINE_PATH = "config/test-wiring-baseline.txt"

/** Reads the grandfathered "<module>|<relPath>" entries; empty/missing = strict mode. */
private fun readBaseline(project: Project): Set<String> {
    val file = project.rootProject.file(BASELINE_PATH)
    if (!file.exists()) return emptySet()
    return file.readLines()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

private fun File.hasKotlinSources(): Boolean =
    isDirectory && walkTopDownSafe().any { it.isFile && it.extension == "kt" }

private fun File.walkTopDownSafe(): Sequence<File> =
    if (exists()) walkTopDown() else emptySequence()

private fun File.isTestSource(): Boolean =
    invariantPath().contains("/src/") &&
        (invariantPath().contains("test/", ignoreCase = true) ||
            name.contains("Test", ignoreCase = false))

private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

private fun String.containsTestToken(): Boolean =
    contains("test", ignoreCase = true)

private fun ProjectDependency.dependencyPath(): String = path
