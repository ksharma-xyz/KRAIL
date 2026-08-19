package xyz.ksharma.krail.quality

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Guards `docs/POLLING_LIFECYCLE.md` against the code drifting away from it.
 *
 * `SharingStarted.WhileSubscribed` only stops background work if the UI subscribes with
 * lifecycle awareness. Get that wrong and nothing fails: the screen works, the tests pass, and
 * the app quietly polls the network behind the lock screen. The only durable defence is a
 * register of every such flow that a person has classified as "polls" or "just state" — and a
 * register is worthless the moment it silently goes out of date.
 *
 * So this test reads the doc, scans production sources, and fails when the two disagree.
 *
 * ## What it can see
 *
 *  - Every `SharingStarted.WhileSubscribed(` in `commonMain` / `androidMain` / `iosMain`,
 *    attributed to the nearest enclosing member-level `val` / `var` / `fun` / `init` (indent
 *    ≤ 4) inside the nearest enclosing `class` / `object` / `interface`. Local declarations
 *    inside `onStart { }` or `combine { }` lambdas are indented further and correctly skipped.
 *  - Any `collectAsState(` that is not `collectAsStateWithLifecycle(`, anywhere in the repo.
 *
 * ## What it cannot see
 *
 *  - Whether a flow in the polling table is *actually* collected with `repeatOnLifecycle` —
 *    that is a Compose call-graph question a text scan cannot answer. It guards the register,
 *    not the call site.
 *  - A `WhileSubscribed` built indirectly (`val started = SharingStarted.WhileSubscribed(x)`
 *    referenced elsewhere is attributed to `started`, which is correct but reads oddly).
 *  - Whether a flow is in the *right* table. Classification stays a human decision; the test
 *    only insists that one was made.
 */
class PollingLifecycleGuardTest {

    @Test
    fun `every WhileSubscribed flow is registered in the polling doc`() {
        val doc = File(repoRoot, DOC_PATH)
        if (!doc.isFile) fail("$DOC_PATH is missing; it is the register this guard reads.")

        val registered = BACKTICKED_FLOW.findAll(doc.readText()).map { it.groupValues[1] }.toSet()
        val found = productionSources().flatMap { whileSubscribedFlowsIn(it) }
        val unregistered =
            found.filterNot { it.flow in registered }.distinctBy { it.flow }.toList()

        if (unregistered.isNotEmpty()) {
            fail(
                "These flows use SharingStarted.WhileSubscribed but are not listed in " +
                    "$DOC_PATH:\n" +
                    unregistered.joinToString("\n") { "  - ${it.flow}  (${it.where})" } +
                    "\n\nAdd each to one of the doc's two tables. Polling flows (anything that " +
                    "loops or calls the network while subscribed) must be activated from the UI " +
                    "with repeatOnLifecycle(STARTED); state-only flows just need " +
                    "collectAsStateWithLifecycle(). Picking the table IS the review.",
            )
        }
    }

    @Test
    fun `no screen collects state without lifecycle awareness`() {
        val offenders = kotlinSources().flatMap { file ->
            file.readLines().withIndex()
                .filterNot { (_, line) -> line.trimStart().startsWith("//") }
                .filter { (_, line) ->
                    line.contains(COLLECT_AS_STATE) &&
                        !line.contains(COLLECT_AS_STATE_WITH_LIFECYCLE)
                }
                .map { (index, _) -> "${file.relativeTo(repoRoot).path}:${index + 1}" }
        }.toList()

        if (offenders.isNotEmpty()) {
            fail(
                "collectAsState() is not lifecycle-aware: it keeps a WhileSubscribed flow " +
                    "subscribed while the app is backgrounded, so polling never stops. Use " +
                    "collectAsStateWithLifecycle() instead.\n" +
                    offenders.joinToString("\n") { "  - $it" },
            )
        }
    }

    private data class Flow(val flow: String, val where: String)

    private fun whileSubscribedFlowsIn(file: File): List<Flow> {
        var type: String? = null
        var declaration: String? = null
        val found = mutableListOf<Flow>()

        file.readLines().forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                return@forEachIndexed
            }
            TYPE_DECLARATION.find(line)?.let { type = it.groupValues[1] }
            MEMBER_DECLARATION.find(line)?.let { declaration = it.groupValues[1].ifEmpty { "init" } }
            if (line.contains(WHILE_SUBSCRIBED)) {
                found += Flow(
                    flow = "${type ?: file.nameWithoutExtension}.${declaration ?: "?"}",
                    where = "${file.relativeTo(repoRoot).path}:${index + 1}",
                )
            }
        }
        return found
    }

    /** This file is skipped: it necessarily spells the banned call out as a search string. */
    private fun kotlinSources(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" && it.name != GUARD_SOURCE_FILE }

    private fun productionSources(): Sequence<File> =
        kotlinSources().filter { file ->
            val path = file.invariantPath()
            PRODUCTION_SOURCE_SETS.any { path.contains(it) }
        }

    private fun File.invariantPath(): String = path.replace('\\', '/')

    private companion object {

        const val DOC_PATH = "docs/POLLING_LIFECYCLE.md"
        const val WHILE_SUBSCRIBED = "SharingStarted.WhileSubscribed("
        const val COLLECT_AS_STATE = "collectAsState("
        const val COLLECT_AS_STATE_WITH_LIFECYCLE = "collectAsStateWithLifecycle"
        const val GUARD_SOURCE_FILE = "PollingLifecycleGuardTest.kt"

        val PRODUCTION_SOURCE_SETS =
            listOf("/src/commonMain/", "/src/androidMain/", "/src/iosMain/")

        /** `Owner.flowName` inside backticks, which is how the doc's tables write them. */
        val BACKTICKED_FLOW = Regex("`([A-Z][A-Za-z0-9_]*\\.[A-Za-z0-9_]+)`")

        val TYPE_DECLARATION = Regex(
            "^\\s*(?:public |internal |private |abstract |open |sealed |data )*" +
                "(?:class|object|interface)\\s+([A-Za-z0-9_]+)",
        )

        /** Member level only: indent ≤ 4, so locals inside lambdas never win. */
        val MEMBER_DECLARATION = Regex(
            "^ {0,4}(?:public |internal |private |protected )*(?:override )*" +
                "(?:val|var|fun|init)\\b\\s*(?:<[^>]*>\\s*)?([A-Za-z0-9_]*)",
        )

        /** Walks up from the test's working directory (the module dir) to the checkout root. */
        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repo root from ${File(".").absolutePath}")
    }
}
