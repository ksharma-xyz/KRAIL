package xyz.ksharma.krail.trip.planner.ui.searchstop

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * The other half of `SearchQueryTextEgressTest`, which can only prove that the call sites that
 * exist today behave. This proves no new one appears that skips the redaction.
 *
 * `AnalyticsEvent.SearchStopQuery.maskedQuery` is the single parameter allowed to carry a
 * rider's typed text, and only after `SearchQueryAnalyticsRedaction.maskedQueryOrNull` has
 * masked the digits out of it. Passing the raw query straight into it compiles, type-checks and
 * reads plausibly, so the only thing standing between that and shipping house numbers to
 * Firebase is a reviewer noticing.
 *
 * Lives in androidHostTest rather than commonTest because it reads source files off disk, which
 * commonTest cannot do.
 *
 * ## What it can see
 *
 * Every `zeroResultQuery =` argument in production Kotlin, and whether a sanctioned redaction
 * function is named within the following few lines.
 *
 * ## What it cannot see
 *
 *  - An assignment whose masking call is further away than [WINDOW] lines.
 *  - A wrapper function that itself returns the raw query. It checks the name at the call site,
 *    not what that name does.
 */
class SearchQueryRedactionCallSiteTest {

    @Test
    fun `maskedQuery is only ever assigned from the masking function`() {
        val offenders = mutableListOf<String>()

        productionSources().forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (!line.isAssignmentToMaskedQuery()) return@forEachIndexed
                val window = lines.subList(index, minOf(index + WINDOW, lines.size))
                    .joinToString("\n")
                if (SANCTIONED.none { window.contains(it) }) {
                    offenders += "${file.relativeTo(repoRoot).path}:${index + 1}: ${line.trim()}"
                }
            }
        }

        if (offenders.isNotEmpty()) {
            fail(
                "maskedQuery is the one analytics parameter allowed to carry a rider's typed " +
                    "text, and only with the digits masked out of it first. These assignments " +
                    "do not go through ${SANCTIONED.joinToString(" or ")}:\n" +
                    offenders.joinToString("\n") { "  - $it" } +
                    "\n\nSee docs/SEARCH_QUERY_TELEMETRY_SPEC.md. Analytics is sent straight to " +
                    "Firebase, so a house number that reaches this parameter is a house number " +
                    "a third party has stored.",
            )
        }
    }

    /** The parameter's own declaration (`val maskedQuery: String? = null`) is not one. */
    private fun String.isAssignmentToMaskedQuery(): Boolean {
        val trimmed = trimStart()
        val isComment = trimmed.startsWith("//") || trimmed.startsWith("*")
        return !isComment &&
            !DECLARATION.containsMatchIn(this) &&
            ASSIGNMENT.containsMatchIn(this)
    }

    private fun productionSources(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val path = file.path.replace('\\', '/')
                PRODUCTION_SOURCE_SETS.any { path.contains(it) }
            }

    private companion object {

        const val WINDOW = 8

        val SANCTIONED = listOf("SearchQueryAnalyticsRedaction.maskedQueryOrNull(")

        val ASSIGNMENT = Regex("\\bmaskedQuery\\s*=")
        val DECLARATION = Regex("\\b(?:val|var)\\s+maskedQuery\\b")

        val PRODUCTION_SOURCE_SETS =
            listOf("/src/commonMain/", "/src/androidMain/", "/src/iosMain/")

        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repo root from ${File(".").absolutePath}")
    }
}
