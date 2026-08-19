package xyz.ksharma.krail.quality

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Every ticked box in `docs/learning/` claims a check exists. This proves the claim.
 *
 * A learning entry whose check has been deleted is worse than no entry: it tells the next reader
 * that a class of bug is covered when nothing is watching it any more, so nobody looks. The
 * entries are the repo's memory of expensive bugs and they are only worth reading if their
 * ticked boxes are true.
 *
 * ## What it checks
 *
 * For each `- [x]` line (and its indented continuation lines), every backticked identifier that
 * is either
 *
 *  - a file path (has a known extension) — must exist on disk, or
 *  - a test class, optionally `.withAMethod` (matches [TEST_IDENTIFIER]) — a Kotlin file under a
 *    test source set must declare that class, and must contain the method name if one is given.
 *
 * Everything else in backticks is prose or a framework symbol and is ignored.
 *
 * ## What it cannot check
 *
 *  - That the test still asserts what the entry says it asserts. Names are cheap; a test can be
 *    hollowed out and keep its name. Mutation-check when you write the entry, as the entries
 *    themselves recommend.
 *  - Claims with no backticked identifier at all, e.g. "the content description is named in the
 *    test's companion". Write the identifier in backticks and it becomes checkable.
 *  - Unticked `- [ ]` boxes, which are open work and are meant to be unresolved.
 */
class LearningLogActionsTest {

    @Test
    fun `every ticked learning-log action still resolves on disk`() {
        val entries = File(repoRoot, LEARNING_DIR).listFiles()
            .orEmpty()
            .filter { it.extension == "md" && it.name != "README.md" }
            .sortedBy { it.name }

        if (entries.isEmpty()) fail("No learning entries found under $LEARNING_DIR")

        val broken = entries.flatMap { entry ->
            tickedClaims(entry).flatMap { claim ->
                BACKTICKED.findAll(claim)
                    .map { it.groupValues[1] }
                    .mapNotNull { identifier -> unresolved(identifier)?.let { entry.name to it } }
            }
        }.distinct()

        if (broken.isNotEmpty()) {
            fail(
                "A learning entry whose check has been deleted is worse than no entry: it says " +
                    "a class of bug is covered when nothing is watching it. These ticked boxes " +
                    "name something that no longer exists:\n" +
                    broken.joinToString("\n") { (file, problem) -> "  - $LEARNING_DIR$file: $problem" } +
                    "\n\nEither restore the check, or downgrade the box to '- [ ]' with a one-" +
                    "line note saying what happened to it. Do not delete the line: the record " +
                    "of what was once covered is the point.",
            )
        }
    }

    /** A `- [x]` line plus the indented lines that continue it. */
    private fun tickedClaims(entry: File): List<String> {
        val claims = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        entry.readLines().forEach { line ->
            when {
                line.trimStart().startsWith(TICKED_PREFIX) ->
                    current = StringBuilder(line).also { claims += it }

                current != null && line.startsWith(" ") && line.isNotBlank() ->
                    current?.append(' ')?.append(line.trim())

                else -> current = null
            }
        }
        return claims.map { it.toString() }
    }

    /** Null when the identifier resolves or is not the kind of thing this test judges. */
    private fun unresolved(identifier: String): String? = when {
        identifier.looksLikeAPath() ->
            "`$identifier` does not exist".takeIf { !File(repoRoot, identifier).exists() }

        TEST_IDENTIFIER.matches(identifier) -> unresolvedTest(identifier)

        else -> null
    }

    private fun unresolvedTest(identifier: String): String? {
        val className = identifier.substringBefore('.')
        val methodName = identifier.substringAfter('.', missingDelimiterValue = "")
        val declaring = testSources().firstOrNull {
            it.readText().contains(Regex("\\bclass\\s+$className\\b"))
        } ?: return "`$identifier` names a test class that no source file declares"

        return "`$identifier` names a test that ${declaring.name} does not contain"
            .takeIf { methodName.isNotEmpty() && !declaring.readText().contains(methodName) }
    }

    private fun String.looksLikeAPath(): Boolean =
        none { it.isWhitespace() || it == '(' } &&
            PATH_EXTENSIONS.any { endsWith(it) }

    private fun testSources(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val path = file.path.replace('\\', '/')
                TEST_SOURCE_SETS.any { path.contains(it) }
            }

    private companion object {

        const val LEARNING_DIR = "docs/learning/"
        const val TICKED_PREFIX = "- [x]"

        val BACKTICKED = Regex("`([^`]+)`")
        val TEST_IDENTIFIER = Regex("[A-Z][A-Za-z0-9_]*Test(?:\\.[A-Za-z0-9_]+)?")
        val PATH_EXTENSIONS = listOf(".md", ".py", ".sh", ".kt", ".kts", ".xml", ".yml", ".yaml")

        val TEST_SOURCE_SETS = listOf(
            "/src/commonTest/",
            "/src/androidHostTest/",
            "/src/androidUnitTest/",
            "/src/androidInstrumentedTest/",
            "/src/iosTest/",
            "/src/test/",
        )

        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repo root from ${File(".").absolutePath}")
    }
}
