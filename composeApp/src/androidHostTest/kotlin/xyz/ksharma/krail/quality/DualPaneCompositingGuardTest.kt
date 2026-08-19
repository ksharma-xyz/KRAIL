package xyz.ksharma.krail.quality

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Keeps a `DualPaneScaffold` out from under an offscreen-compositing ancestor.
 *
 * On iOS a `UIKitView` (MapLibre's `MLNMapView`) cannot composite into an offscreen GPU buffer.
 * Put the dual-pane split inside `CloudGradientBackground` — which applies
 * `graphicsLayer { compositingStrategy = Offscreen }` to its whole subtree — and the right-pane
 * map renders **blank**, at the correct size, in the correct window position, with no error
 * anywhere. Android is unaffected, so the Android build, the tests and detekt are all green
 * while the iOS screen is empty. That combination cost a long debugging saga once already; see
 * `DualPaneScaffold`'s KDoc and `docs/MAPLIBRE_IOS_PITFALLS.md` §1.
 *
 * The rule: any gradient or render-effect wrapper goes **inside** the `listPane` lambda. The
 * scaffold call itself must have no such ancestor.
 *
 * ## What this scan can see
 *
 * A forward pass over each file tracking brace depth. A line naming a wrapper
 * ([COMPOSITING_WRAPPERS]) arms a marker; the next content lambda opened by a `) {` within
 * [ARM_WINDOW] lines pushes that wrapper onto a stack until its block closes. A
 * `DualPaneScaffold(` reached with a non-empty stack is a violation. This catches the real
 * shapes: `CloudGradientBackground(...) { … DualPaneScaffold(…) }` and
 * `Box(modifier = Modifier.blur(x)) { … DualPaneScaffold(…) }`.
 *
 * ## What it cannot see
 *
 *  - A wrapper applied in a **different function** — `Screen()` wrapping `ScreenDualPane()`
 *    which calls the scaffold. It is lexical only; it does not follow composable calls.
 *  - A wrapper reached through a hoisted `@Composable` lambda variable.
 *  - `CompositingStrategy.Offscreen` set through a modifier built elsewhere and passed in.
 *  - A wrapper whose `) {` is more than [ARM_WINDOW] lines after the modifier that names it.
 *  - Braces inside string literals, which are counted as real braces.
 *
 * So it is a tripwire on the shape that broke before, not a proof of correctness. The iOS
 * checklist in `docs/MAPLIBRE_IOS_PITFALLS.md` is still the thing to work through for a new map
 * screen.
 */
class DualPaneCompositingGuardTest {

    @Test
    fun `no DualPaneScaffold sits under an offscreen compositing ancestor`() {
        val violations =
            productionSources().flatMap { file -> scaffoldsUnderWrappers(file) }.toList()

        if (violations.isNotEmpty()) {
            fail(
                "A DualPaneScaffold is nested inside a compositing wrapper. On iOS the " +
                    "right-pane UIKitView map cannot composite into an offscreen buffer and " +
                    "renders blank, while Android, detekt and every test stay green:\n" +
                    violations.joinToString("\n") { "  - ${it.where} is inside ${it.wrappers}" } +
                    "\n\nMove the wrapper INSIDE the listPane lambda so the right pane stays a " +
                    "sibling. See DualPaneScaffold's KDoc and docs/MAPLIBRE_IOS_PITFALLS.md §1; " +
                    "docs/TABLET_FOLDABLE_UX.md §4b has the per-screen rules.",
            )
        }
    }

    private data class Violation(val where: String, val wrappers: String)

    private fun scaffoldsUnderWrappers(file: File): List<Violation> {
        val wrapperStack = WrapperStack()
        return file.readLines().mapIndexedNotNull { index, line ->
            val code = line.substringBefore("//")
            wrapperStack.advance(index, code)
            val wrappers = wrapperStack.openWrappers().filterNot { it.isExcepted(file) }
            violationOrNull(file, index, code, wrappers)
        }
    }

    private fun violationOrNull(
        file: File,
        index: Int,
        code: String,
        wrappers: List<String>,
    ): Violation? {
        if (!code.contains(SCAFFOLD) || wrappers.isEmpty()) return null
        return Violation(
            where = "${file.relativeTo(repoRoot).path}:${index + 1}",
            wrappers = wrappers.joinToString(" > "),
        )
    }

    /**
     * Brace-depth tracker. A wrapper name arms a marker; the next `) {` within [ARM_WINDOW]
     * lines pushes it, and it pops when its block closes.
     */
    private class WrapperStack {
        private val open = ArrayDeque<Pair<Int, String>>()
        private var armed: String? = null
        private var armedAt = 0
        private var depth = 0

        fun openWrappers(): List<String> = open.map { it.second }

        fun advance(index: Int, code: String) {
            if (armed != null && index - armedAt > ARM_WINDOW) armed = null
            COMPOSITING_WRAPPERS.firstOrNull { code.contains(it) }?.let { wrapper ->
                armed = wrapper
                armedAt = index
            }

            val opens = code.count { it == '{' }
            val armedWrapper = armed
            if (opens > 0 && armedWrapper != null && CONTENT_LAMBDA.containsMatchIn(code)) {
                open.addLast(depth + 1 to armedWrapper)
                armed = null
            }

            depth += opens - code.count { it == '}' }
            while (open.isNotEmpty() && open.last().first > depth) open.removeLast()
        }
    }

    /**
     * `SavedTripsScreen` blurs everything behind the Ask KRAIL dialog, and the scaffold is under
     * that Box. The radius is 0.dp whenever the dialog is closed, and `Modifier.blur` installs no
     * render effect at radius 0, so there is no offscreen layer in the state a rider spends the
     * screen in; while the dialog is up it covers the map anyway. Narrow on purpose: adding a
     * `CloudGradientBackground` to the same file still fails.
     */
    private fun String.isExcepted(file: File): Boolean =
        this == BLUR && file.name == BLUR_EXCEPTED_FILE

    private fun productionSources(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val path = file.path.replace('\\', '/')
                PRODUCTION_SOURCE_SETS.any { path.contains(it) }
            }

    private companion object {

        const val SCAFFOLD = "DualPaneScaffold("
        const val BLUR = ".blur("
        const val BLUR_EXCEPTED_FILE = "SavedTripsScreen.kt"

        /** How far a wrapper's name may sit above the `) {` that opens its content lambda. */
        const val ARM_WINDOW = 25

        val COMPOSITING_WRAPPERS = listOf("CloudGradientBackground(", ".graphicsLayer", BLUR)

        /** A call's trailing content lambda, as opposed to a `graphicsLayer { }` scope lambda. */
        val CONTENT_LAMBDA = Regex("\\)\\s*\\{")

        val PRODUCTION_SOURCE_SETS =
            listOf("/src/commonMain/", "/src/androidMain/", "/src/iosMain/")

        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repo root from ${File(".").absolutePath}")
    }
}
