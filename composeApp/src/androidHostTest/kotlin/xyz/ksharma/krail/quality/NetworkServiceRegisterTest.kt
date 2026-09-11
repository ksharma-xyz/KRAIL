package xyz.ksharma.krail.quality

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Guards the service register in `docs/NETWORK_RELIABILITY.md` against the code drifting away
 * from it.
 *
 * The app once carried two incompatible error contracts in the same layer: two services threw
 * raw Ktor exceptions while a third returned a `Result`. Nobody chose that. Each service was
 * written at a different time by someone making a reasonable local decision, and the
 * inconsistency was only visible from above.
 *
 * A register fixes that only while it stays true, so this reads the doc, scans for
 * `Real*Service` classes, and fails when the two disagree. Picking the row is the review.
 *
 * ## What it can see
 *
 *  - Every class named `Real*Service` in a production source set.
 *  - Whether each appears in the doc's register table.
 *
 * ## What it cannot see
 *
 *  - Whether a service's *actual* contract matches the one its row claims. That is a type
 *    question the compiler already answers at every call site; this holds the weaker property
 *    that a human wrote the row down.
 *  - A network call made outside a `Real*Service`. `NetworkClientFactoryGuardTest` covers the
 *    client side of that, and a repository calling Ktor directly would be visible in review.
 */
class NetworkServiceRegisterTest {

    @Test
    fun `every Real service appears in the network reliability register`() {
        val doc = File(repoRoot, DOC_PATH)
        if (!doc.isFile) fail("$DOC_PATH is missing; it is the register this guard reads.")

        val registered = BACKTICKED_SERVICE.findAll(doc.readText())
            .map { it.groupValues[1] }
            .toSet()

        val found = productionKotlinSources()
            .flatMap { file ->
                CLASS_DECLARATION.findAll(file.readText())
                    .map { it.groupValues[1] to file.relativeTo(repoRoot).path }
            }
            .distinctBy { it.first }

        val unregistered = found.filterNot { (name, _) -> name in registered }

        if (unregistered.isNotEmpty()) {
            fail(
                "These services are not listed in $DOC_PATH:\n" +
                    unregistered.joinToString("\n") { (name, where) -> "  - $name  ($where)" } +
                    "\n\nAdd a row to the service register saying what the service returns. " +
                    "The default is Result<T> via NetworkCaller; anything else needs its " +
                    "reason written down, which is the point of the row.",
            )
        }
    }

    @Test
    fun `the register does not list services that no longer exist`() {
        val doc = File(repoRoot, DOC_PATH)
        val registered = BACKTICKED_SERVICE.findAll(doc.readText()).map { it.groupValues[1] }.toSet()

        val existing = productionKotlinSources()
            .flatMap { file -> CLASS_DECLARATION.findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()

        val stale = (registered - existing).sorted()

        if (stale.isNotEmpty()) {
            fail(
                "These are in the $DOC_PATH register but no longer exist in the codebase:\n" +
                    stale.joinToString("\n") { "  - $it" } +
                    "\n\nA register listing things that are gone is how it stops being read.",
            )
        }
    }

    private fun productionKotlinSources(): List<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.path.contains(MAIN_SOURCE_MARKER) }
            .toList()

    private companion object {
        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

        const val DOC_PATH = "docs/NETWORK_RELIABILITY.md"

        /** `src/commonMain`, `src/androidMain`, `src/iosMain`; excludes every test source set. */
        const val MAIN_SOURCE_MARKER = "Main/kotlin/"

        val SKIPPED_DIRS = setOf("build", ".git", ".gradle", ".claude", "generated")

        /** `class RealFooService`, with or without `internal` / `private`. */
        val CLASS_DECLARATION = Regex("""\bclass\s+(Real\w*Service)\b""")

        /** A service name in backticks anywhere in the doc, e.g. `RealTripPlanningService`. */
        val BACKTICKED_SERVICE = Regex("""`(Real\w*Service)`""")
    }
}
