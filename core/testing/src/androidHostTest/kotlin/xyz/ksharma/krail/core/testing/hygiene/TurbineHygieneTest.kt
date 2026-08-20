package xyz.ksharma.krail.core.testing.hygiene

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * A Turbine block collecting an endless flow has to cancel it.
 *
 * An infinite flow left collecting inside a virtual-time test does not fail. It runs, and
 * keeps running, and whatever bounded assertions came before it still pass. That is what makes
 * this worth a guard rather than a convention: the failure mode is a green test and a growing
 * log file. `DepartureBoardRepositoryTest` produced a 98 GB Gradle log exactly this way
 * (#1601), and the rule that came out of it is written down in TESTING.md.
 *
 * Scope, stated honestly. This is a text scan, not an analysis. It looks at the ~200
 * characters before each `.test {` for a subject that names itself a poller, a ticker, an
 * auto-refresh or an `isActive` gate, and then asks whether that block cancels before it
 * closes. It cannot see an endless flow behind an innocent name, and it cannot tell a
 * cancellation that runs from one inside a dead branch. It catches the shape the incident
 * actually had — a flow whose name said "this never ends" collected in a block that never
 * said stop — and nothing more.
 *
 * It lives in `:core:testing` because that is the module that owns the harness the rule
 * belongs to, and in `androidHostTest` rather than `commonTest` for one blunt reason: common
 * Kotlin has no filesystem, and a guard that reads the repo needs one. It runs once, on the
 * host lane, which is where it would run anyway.
 */
class TurbineHygieneTest {

    @Test
    fun `every turbine block on an endless flow cancels it`() {
        val root = repoRoot()
        val allowlist = readAllowlist(root)
        val offenders = mutableListOf<String>()
        val allowlistHits = mutableSetOf<String>()

        root.testSources().forEach { file ->
            val relative = file.relativeTo(root).invariantPath()
            val text = file.readText().stripComments()

            text.turbineBlocks().forEach { block ->
                if (!block.subjectLooksEndless()) return@forEach

                if (relative in allowlist) {
                    allowlistHits += relative
                    return@forEach
                }
                if (TERMINATORS.none { it in block.body }) {
                    offenders += "$relative:${block.line}  ${block.subjectSnippet()}"
                }
            }
        }

        val staleAllowlist = (allowlist.keys - allowlistHits).sorted()

        if (offenders.isEmpty() && staleAllowlist.isEmpty()) return

        fail(
            buildString {
                if (offenders.isNotEmpty()) {
                    appendLine("Turbine blocks collecting an endless flow without cancelling it:")
                    offenders.sorted().forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("End the block with one of ${TERMINATORS.joinToString()}.")
                    appendLine("An uncancelled poller does not fail the test — it spins. That is")
                    appendLine("how DepartureBoardRepositoryTest produced a 98 GB Gradle log")
                    appendLine("(#1601). See TESTING.md.")
                    appendLine()
                }
                if (staleAllowlist.isNotEmpty()) {
                    appendLine("$ALLOWLIST_PATH lists files with no matching block any more:")
                    staleAllowlist.forEach { appendLine("  $it") }
                    appendLine("Remove them, so the allowlist keeps meaning what it says.")
                }
            },
        )
    }

    /** One `.test { … }` block: where it starts, and what it contains. */
    private data class TurbineBlock(val line: Int, val precedingContext: String, val body: String)

    private fun TurbineBlock.subjectLooksEndless(): Boolean =
        ENDLESS_SUBJECTS.any { it.containsMatchIn(precedingContext) }

    private fun TurbineBlock.subjectSnippet(): String =
        precedingContext.takeLast(SNIPPET_LENGTH).replace(Regex("\\s+"), " ").trim()

    /**
     * Every `.test {` in [this], paired with the text before it and its brace-matched body.
     *
     * Brace matching rather than a regex for the body: a Turbine block holds `if`, `repeat`
     * and lambdas of its own, and a non-greedy match to the first `}` would call any of them
     * the end of the block.
     */
    private fun String.turbineBlocks(): List<TurbineBlock> =
        TURBINE_CALL.findAll(this).mapNotNull { match ->
            val open = indexOf('{', startIndex = match.range.first)
            if (open < 0) return@mapNotNull null

            var depth = 0
            var cursor = open
            while (cursor < length) {
                when (this[cursor]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                cursor++
            }

            TurbineBlock(
                line = take(match.range.first).count { it == '\n' } + 1,
                precedingContext = substring(
                    (match.range.first - CONTEXT_LENGTH).coerceAtLeast(0),
                    match.range.first,
                ),
                body = substring(open, (cursor + 1).coerceAtMost(length)),
            )
        }.toList()

    /**
     * Comments are stripped first. A KDoc explaining why a flow is endless, or a `//` note
     * naming a poller, is exactly the right thing to write and must not be what trips a check
     * about code.
     */
    private fun String.stripComments(): String =
        replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private fun File.testSources(): Sequence<File> = walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" }
        .filter { it.isFile && it.extension == "kt" }
        .filter { TEST_SOURCE_DIR.containsMatchIn(it.invariantPath()) }

    /** `<repo-relative path>` to `<reason>`, with `#` comments and blank lines dropped. */
    private fun readAllowlist(root: File): Map<String, String> {
        val file = File(root, ALLOWLIST_PATH)
        if (!file.exists()) return emptyMap()

        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                line.substringBefore('#').trim() to line.substringAfter('#', "").trim()
            }
    }

    /**
     * Walks up from the test's working directory (the Gradle module dir) to the checkout root.
     * Anchored on `settings.gradle.kts` rather than a fixed number of `..` hops, so moving the
     * module does not silently make this scan nothing.
     */
    private fun repoRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
        }
        fail("Could not find the repo root (no settings.gradle.kts above ${File(".").absolutePath})")
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private companion object {
        const val ALLOWLIST_PATH = "config/turbine-hygiene-allowlist.txt"

        // Enough to reach back over a wrapped call chain, short enough not to swallow the
        // statement before it.
        const val CONTEXT_LENGTH = 200
        const val SNIPPET_LENGTH = 70

        val TURBINE_CALL = Regex("""\.test\s*\{""")
        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")
        val TEST_SOURCE_DIR = Regex("""/src/[^/]*[Tt]est[^/]*/""")

        // Names that promise the flow never completes on its own.
        val ENDLESS_SUBJECTS = listOf(
            Regex("""poll""", RegexOption.IGNORE_CASE),
            Regex("""autoRefresh""", RegexOption.IGNORE_CASE),
            Regex("""ticker""", RegexOption.IGNORE_CASE),
            Regex("""isActive"""),
        )

        // All three end the collection. TESTING.md names only the first, because that is what
        // the incident was fixed with; consume differs from ignore in what it hands back, not
        // in whether it cancels, and failing it would be a false positive.
        val TERMINATORS = listOf(
            "cancelAndIgnoreRemainingEvents()",
            "cancelAndConsumeRemainingEvents()",
            "cancel()",
        )
    }
}
