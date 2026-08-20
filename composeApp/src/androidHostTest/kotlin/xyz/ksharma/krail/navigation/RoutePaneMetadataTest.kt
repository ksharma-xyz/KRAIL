package xyz.ksharma.krail.navigation

import xyz.ksharma.krail.feature.debug.settings.ui.navigation.DebugConfigRoute
import xyz.ksharma.krail.feature.track.ui.navigation.TrackRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerRoute
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.fail

/**
 * Keeps the pane-metadata table in `docs/TABLET_FOLDABLE_UX.md` §5 equal to the code.
 *
 * A route's `metadata = ListDetailSceneStrategy.detailPane()` is one optional argument on one
 * `entry<…>` call. Getting it wrong does nothing on a phone and everything on a tablet: the
 * route lands in the wrong pane, or takes the whole window from a screen that was managing its
 * own split. Nothing about it is checked by the compiler, and nobody reviewing a phone-shaped
 * screenshot can see it.
 *
 * The doc was the only place the intent was written down, and it had already drifted — two
 * routes were missing from it entirely, and two more exist with no `entry` at all. A table
 * that describes the code is a comment; a table the build compares against the code is a
 * contract, and this makes it the second.
 *
 * Bidirectional on purpose. A route missing from the table fails, because that is the new
 * route nobody made a pane decision for. A row naming a route that no longer exists fails too,
 * because a table that keeps its dead rows is how the first kind of drift goes unnoticed.
 *
 * Scope, stated honestly. The metadata is read from SOURCE, not from a running entry provider:
 * building one needs a composition, a Koin graph and a real `Context`. So this proves the
 * declaration in the file matches the doc, not that Navigation 3 does anything with it.
 * `NavKeySerializationConfigTest` next door covers the other half of the route registry.
 */
class RoutePaneMetadataTest {

    @Test
    fun `every route's pane metadata matches the documented table`() {
        val root = repoRoot()
        val documented = parseDocTable(root)
        val declared = scanEntryDeclarations(root)
        val routes = allRoutes().mapNotNull { it.simpleName }.toSortedSet()

        val missingFromDoc = routes - documented.keys
        val notARoute = documented.keys.toSortedSet() - routes
        val mismatched = routes
            .filter { it in documented && it !in missingFromDoc }
            .mapNotNull { route ->
                val inDoc = documented.getValue(route)
                val inCode = declared[route] ?: NO_ENTRY
                if (inDoc == inCode) null else "  $route: doc says '$inDoc', code declares '$inCode'"
            }

        if (missingFromDoc.isEmpty() && notARoute.isEmpty() && mismatched.isEmpty()) return

        fail(
            buildString {
                appendLine("$DOC_PATH §5 and the entry declarations disagree.")
                if (missingFromDoc.isNotEmpty()) {
                    appendLine()
                    appendLine("Routes with no row in the table:")
                    missingFromDoc.forEach { appendLine("  $it (code declares '${declared[it] ?: NO_ENTRY}')") }
                    appendLine("Every route needs a pane decision, including 'none'. Add a row.")
                }
                if (notARoute.isNotEmpty()) {
                    appendLine()
                    appendLine("Rows naming something that is not a route any more:")
                    notARoute.forEach { appendLine("  $it") }
                    appendLine("Delete the row in the change that deleted the route.")
                }
                if (mismatched.isNotEmpty()) {
                    appendLine()
                    appendLine("Rows that disagree with the entry declaration:")
                    mismatched.forEach { appendLine(it) }
                }
                appendLine()
                appendLine("The Metadata column takes exactly one of: ${VALID_VALUES.joinToString()}.")
            },
        )
    }

    /**
     * `<RouteName>` to its Metadata cell, read from the §5 table.
     *
     * Anchored on the `## 5.` heading and stopped at the next heading, so a second table
     * elsewhere in the doc cannot quietly become the contract.
     */
    private fun parseDocTable(root: File): Map<String, String> {
        val doc = File(root, DOC_PATH)
        if (!doc.isFile) fail("$DOC_PATH not found — this test reads the table it guards.")

        val lines = doc.readLines()
        val start = lines.indexOfFirst { it.startsWith(SECTION_HEADING) }
        if (start < 0) fail("$DOC_PATH has no '$SECTION_HEADING' section.")
        val end = lines
            .drop(start + 1)
            .indexOfFirst { it.startsWith("## ") }
            .let { if (it < 0) lines.size else start + 1 + it }

        val rows = lines.subList(start, end)
            .filter { it.startsWith("|") }
            .map { row -> row.trim('|').split('|').map { it.trim() } }
            .filter { cells -> cells.size >= 2 && cells[0].startsWith("`") }

        if (rows.isEmpty()) fail("$DOC_PATH §5 has no route rows — the table this guards is gone.")

        return rows.associate { cells ->
            val route = cells[0].trim('`')
            val metadata = cells[1].trim('`', '*', ' ')
            if (metadata !in VALID_VALUES) {
                fail(
                    "$DOC_PATH §5 row '$route' has Metadata '$metadata'. It must be exactly " +
                        "one of ${VALID_VALUES.joinToString()} so this test can compare it.",
                )
            }
            route to metadata
        }
    }

    /**
     * `<RouteName>` to the metadata its `entry<…>` declares, for every entry in the repo.
     *
     * The two shapes in play are `entry<X>(metadata = …) { }` and a bare `entry<X> { }`. The
     * argument list is paren-matched rather than regex-matched: metadata sits beside other
     * arguments and a lazy match to the first `)` would stop inside `detailPane()` itself.
     */
    private fun scanEntryDeclarations(root: File): Map<String, String> {
        val found = mutableMapOf<String, String>()

        root.productionSources().forEach { file ->
            val text = file.readText().stripComments()
            ENTRY_CALL.findAll(text).forEach { match ->
                val route = match.groupValues[1]
                found[route] = text.metadataAfter(match.range.last + 1)
            }
        }
        return found
    }

    private fun String.metadataAfter(index: Int): String {
        val open = indexOfFirst(from = index) { !it.isWhitespace() }
        if (open < 0 || this[open] != '(') return NONE

        var depth = 0
        var cursor = open
        while (cursor < length) {
            when (this[cursor]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) break
                }
            }
            cursor++
        }

        val args = substring(open, (cursor + 1).coerceAtMost(length))
        return when {
            "$STRATEGY.$DETAIL_PANE" in args -> DETAIL_PANE
            "$STRATEGY.$LIST_PANE" in args -> LIST_PANE
            else -> NONE
        }
    }

    private inline fun String.indexOfFirst(from: Int, predicate: (Char) -> Boolean): Int {
        for (i in from until length) if (predicate(this[i])) return i
        return -1
    }

    private fun String.stripComments(): String =
        replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private fun File.productionSources(): Sequence<File> = walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" }
        .filter { it.isFile && it.extension == "kt" }
        .filter { it.path.replace(File.separatorChar, '/').contains("/src/") }
        .filterNot { TEST_SOURCE_DIR.containsMatchIn(it.path.replace(File.separatorChar, '/')) }

    private fun repoRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
        }
        fail("Could not find the repo root above ${File(".").absolutePath}")
    }

    /** Same walk as [NavKeySerializationConfigTest]: `sealedSubclasses` is direct children only. */
    private fun allRoutes(): List<KClass<*>> =
        listOf(TripPlannerRoute::class, TrackRoute::class, DebugConfigRoute::class)
            .flatMap { it.concreteSubclasses() }

    private fun KClass<*>.concreteSubclasses(): List<KClass<*>> =
        sealedSubclasses.flatMap { subclass ->
            if (subclass.isSealed) subclass.concreteSubclasses() else listOf(subclass)
        }

    private companion object {
        const val DOC_PATH = "docs/TABLET_FOLDABLE_UX.md"
        const val SECTION_HEADING = "## 5."

        const val STRATEGY = "ListDetailSceneStrategy"
        const val DETAIL_PANE = "detailPane()"
        const val LIST_PANE = "listPane()"
        const val NONE = "none"

        // A route with no entry<…> anywhere. Declared and serialisable, but nothing renders it.
        const val NO_ENTRY = "no entry"

        val VALID_VALUES = listOf(NONE, DETAIL_PANE, LIST_PANE, NO_ENTRY)

        val ENTRY_CALL = Regex("""\bentry<\s*([A-Za-z_][A-Za-z0-9_]*)\s*>""")
        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")
        val TEST_SOURCE_DIR = Regex("""/src/[^/]*[Tt]est[^/]*/""")
    }
}
