package xyz.ksharma.krail.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * The iOS unit-test execution lane.
 *
 * `commonTest` has always *compiled* for iOS; until this plugin nothing ever *ran* it. The
 * Kotlin/Native test task is `iosSimulatorArm64Test`, and running it repo-wide fails, so the
 * lane is an explicit allowlist ([IOS_TEST_MODULES]) rather than a wildcard.
 *
 * Two things stop the other modules, and neither is a property of the test code:
 *
 *  1. **Firebase iOS frameworks.** `:core:analytics` and `:core:remote-config` pull the
 *     GitLive Firebase Kotlin SDK, whose iOS klibs carry `-framework FirebaseCore` linker
 *     options. Those frameworks are supplied by the Xcode project's SPM integration, so a
 *     Gradle-linked `.kexe` test binary cannot find them and `linkDebugTestIosSimulatorArm64`
 *     dies with `ld: framework 'FirebaseCore' not found`. `:core:testing` depends on both, so
 *     every module that consumes the shared fakes inherits the wall.
 *
 *  2. **Backtick test names containing `,`.** Kotlin/Native rejects them outright
 *     (`Name contains illegal characters: ","`) where the JVM accepts them. This one is
 *     cheap to fix per module — rename the test — and is called out separately below so it
 *     is not mistaken for the structural blocker.
 *
 * The allowlist is deliberately verified rather than trusted: [VerifyIosTestClassificationTask]
 * fails when a module grows shared test sources without being placed in either list, so a new
 * module cannot quietly opt out of the lane.
 *
 * Registration happens on the *root* project, driven from [KotlinMultiplatformConventionPlugin]
 * rather than from a root plugin of its own: applying any build-logic plugin to the root puts
 * build-logic on the root classpath with an unknown version, after which every subproject's
 * `alias(libs.plugins.krail.*)` request (catalog version `unspecified`) fails to resolve.
 */
fun Project.configureIosUnitTestLane() {
    rootProject.registerIosUnitTestLane()
}

private const val IOS_TEST_TASK = "iosSimulatorArm64Test"
private const val IOS_LANE_TASK = "iosUnitTest"

/** Shared-test source roots that make a module a candidate for the iOS lane. */
private val SHARED_TEST_SOURCE_DIRS = listOf("commonTest", "iosTest")

/**
 * Modules whose shared tests run on the iOS simulator. CI runs exactly these, and they are
 * expected to stay green — a failure here is a real Kotlin/Native behaviour difference.
 */
val IOS_TEST_MODULES: List<String> = listOf(
    ":core:date-time",
    ":core:deeplink",
    ":core:transport",
    ":feature:debug-settings:store",
    ":taj",
)

private const val FIREBASE_WALL =
    "links the GitLive Firebase Kotlin SDK (directly or via :core:testing); its iOS klibs " +
        "declare -framework FirebaseCore, which only the Xcode/SPM build supplies, so the " +
        "Kotlin/Native test binary cannot be linked."

private const val FIREBASE_WALL_AND_TEST_NAMES =
    "$FIREBASE_WALL Its shared tests additionally use ',' inside backtick names, which " +
        "Kotlin/Native rejects — both have to go before this module can join the lane."

/**
 * Every other module with shared test sources, and why it is host-only. Entries leave this
 * map by being fixed and added to [IOS_TEST_MODULES], never by being deleted.
 */
val IOS_TEST_EXCLUSIONS: Map<String, String> = mapOf(
    ":composeApp" to FIREBASE_WALL,
    ":core:adaptive-ui" to FIREBASE_WALL,
    ":core:analytics" to FIREBASE_WALL,
    ":core:app-version" to FIREBASE_WALL,
    ":core:festival" to FIREBASE_WALL,
    ":core:network" to FIREBASE_WALL,
    ":core:remote-config" to FIREBASE_WALL,
    ":core:testing" to FIREBASE_WALL,
    ":discover:network:real" to FIREBASE_WALL,
    ":feature:debug-settings:ui" to FIREBASE_WALL,
    ":feature:departures:network" to FIREBASE_WALL,
    ":feature:departures:ui" to FIREBASE_WALL,
    ":feature:park-ride:network" to FIREBASE_WALL,
    ":feature:track:network" to FIREBASE_WALL_AND_TEST_NAMES,
    ":feature:track:state" to FIREBASE_WALL,
    ":feature:track:ui" to FIREBASE_WALL,
    ":feature:trip-planner:network" to FIREBASE_WALL,
    ":feature:trip-planner:ui" to FIREBASE_WALL_AND_TEST_NAMES,
    ":info-tile:network:api" to FIREBASE_WALL,
    ":info-tile:network:real" to FIREBASE_WALL,
)

private fun Project.registerIosUnitTestLane() {
    // Every KMP module calls in; the root only needs the tasks once.
    if (tasks.findByName(IOS_LANE_TASK) != null) return

    val classified = IOS_TEST_MODULES.toSet() + IOS_TEST_EXCLUSIONS.keys
    val withSharedTests = subprojects
        .filter { it.hasSharedTestSources() }
        .map { it.path }
        .sorted()

    val verify = tasks.register(
        VerifyIosTestClassificationTask.NAME,
        VerifyIosTestClassificationTask::class.java,
    ) {
        group = "verification"
        description = "Fails if a module has shared test sources but no iOS lane classification."
        modulesWithSharedTests.set(withSharedTests)
        classifiedModules.set(classified.sorted())
    }

    tasks.register(IOS_LANE_TASK) {
        group = "verification"
        description = "Runs $IOS_TEST_TASK for every module in the iOS lane (macOS only)."
        dependsOn(verify)
        if (isMacOs()) {
            dependsOn(IOS_TEST_MODULES.map { "$it:$IOS_TEST_TASK" })
        } else {
            doFirst {
                throw IllegalStateException(
                    "iosUnitTest needs macOS with Xcode — Kotlin/Native cannot link or boot " +
                        "an iOS simulator binary on this host. Run testAndroidHostTest instead.",
                )
            }
        }
    }
}

private fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

private fun Project.hasSharedTestSources(): Boolean = SHARED_TEST_SOURCE_DIRS.any { dir ->
    File(projectDir, "src/$dir").hasKotlinSourcesIn()
}

private fun File.hasKotlinSourcesIn(): Boolean =
    isDirectory && walkTopDown().any { it.isFile && it.extension == "kt" }

/**
 * Keeps [IOS_TEST_MODULES] + [IOS_TEST_EXCLUSIONS] honest. A new module with `commonTest`
 * sources is neither silently run on iOS nor silently skipped — the contributor has to state
 * which it is, and record why if it is skipped.
 */
abstract class VerifyIosTestClassificationTask : DefaultTask() {

    @get:Input
    abstract val modulesWithSharedTests: ListProperty<String>

    @get:Input
    abstract val classifiedModules: ListProperty<String>

    @TaskAction
    fun verify() {
        val classified = classifiedModules.get().toSet()
        val actual = modulesWithSharedTests.get()

        val unclassified = actual.filterNot { it in classified }
        val stale = classified.filterNot { it in actual }.sorted()

        if (unclassified.isEmpty() && stale.isEmpty()) return

        throw IllegalStateException(
            buildString {
                appendLine("iOS test lane classification is out of date.")
                if (unclassified.isNotEmpty()) {
                    appendLine()
                    appendLine("These modules have shared test sources but are in neither list:")
                    unclassified.forEach { appendLine("  $it") }
                    appendLine("Add each to IOS_TEST_MODULES (if `./gradlew <module>:$IOS_TEST_TASK`")
                    appendLine("passes) or to IOS_TEST_EXCLUSIONS with the reason it cannot run.")
                }
                if (stale.isNotEmpty()) {
                    appendLine()
                    appendLine("These are listed but no longer have shared test sources:")
                    stale.forEach { appendLine("  $it") }
                    appendLine("Remove them from IosUnitTests.kt.")
                }
                appendLine()
                appendLine("Both lists live in gradle/build-logic/convention/src/main/kotlin/")
                appendLine("xyz/ksharma/krail/gradle/IosUnitTests.kt")
            },
        )
    }

    companion object {
        const val NAME = "verifyIosTestClassification"
    }
}
