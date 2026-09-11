package xyz.ksharma.krail.quality

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Stops the per-feature HTTP clients growing back.
 *
 * Four feature modules each carried an `expect fun <feature>HttpClient(baseClient)` with two
 * `actual` implementations. Twelve files, every one byte-for-byte identical apart from the
 * function name and which BuildKonfig constant it read. Nobody added them carelessly: each
 * was a reasonable local decision, copied from the module next door, and the duplication was
 * only visible from above.
 *
 * That is exactly the shape a text scan can hold and a code review cannot, so it is held
 * here. The rule and the replacement are in `docs/NETWORK_RELIABILITY.md`: one factory in
 * `:core:network`, and a new upstream adds an `ApiCredential` case rather than three files.
 *
 * ## What it can see
 *
 *  - Any declaration of a function whose name ends in `HttpClient`, outside `:core:network`.
 *  - Any read of an API-key BuildKonfig constant outside `:core:network`, which is the other
 *    half of the same mistake: a module reaching for the key means it is about to build its
 *    own client.
 *
 * ## What it cannot see
 *
 *  - A client configured inline at a call site without a named function. That is visible in
 *    review in a way twelve matching files across four modules were not, and a check that
 *    tried to catch it would flag every legitimate `config { }` call.
 */
class NetworkClientFactoryGuardTest {

    @Test
    fun `no module outside core network declares its own HttpClient factory`() {
        val offenders = productionKotlinSources()
            .filterNot { it.isUnder(CORE_NETWORK) }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> FACTORY_DECLARATION.containsMatchIn(line) }
                    .map { (index, line) -> "${file.relative()}:${index + 1}  ${line.trim()}" }
            }

        if (offenders.isNotEmpty()) {
            fail(
                "These declare a per-feature HTTP client factory:\n" +
                    offenders.joinToString("\n") { "  $it" } +
                    "\n\nUse the one factory in :core:network instead:\n" +
                    "  httpClient.forApi(ApiCredential.NswApiKey)\n" +
                    "An upstream needing a different credential adds an ApiCredential case. " +
                    "See docs/NETWORK_RELIABILITY.md.",
            )
        }
    }

    @Test
    fun `no module outside core network reads a transport API key`() {
        val offenders = productionKotlinSources()
            .filterNot { it.isUnder(CORE_NETWORK) }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filterNot { (_, line) -> line.trimStart().startsWith("//") }
                    .filter { (_, line) -> line.contains(API_KEY_CONSTANT) }
                    .map { (index, _) -> "${file.relative()}:${index + 1}" }
            }

        if (offenders.isNotEmpty()) {
            fail(
                "These read an NSW API key outside :core:network:\n" +
                    offenders.joinToString("\n") { "  $it" } +
                    "\n\nThe key is applied once, by ApiCredential.NswApiKey. A module that " +
                    "needs it is about to build its own client, which is the duplication " +
                    "this guard exists to prevent.",
            )
        }
    }

    private fun productionKotlinSources(): List<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.path.contains(MAIN_SOURCE_MARKER) }
            .toList()

    private fun File.isUnder(modulePath: String): Boolean =
        relative().startsWith(modulePath)

    private fun File.relative(): String = relativeTo(repoRoot).path

    private companion object {
        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

        const val CORE_NETWORK = "core/network"

        /** `src/commonMain`, `src/androidMain`, `src/iosMain`; excludes every test source set. */
        const val MAIN_SOURCE_MARKER = "Main/kotlin/"

        val SKIPPED_DIRS = setOf("build", ".git", ".gradle", ".claude", "generated")

        /** `expect fun fooHttpClient(`, `actual fun fooHttpClient(`, `fun fooHttpClient(`. */
        val FACTORY_DECLARATION = Regex("""\b(expect\s+|actual\s+)?fun\s+\w*HttpClient\s*\(""")

        const val API_KEY_CONSTANT = "NSW_TRANSPORT_API_KEY"
    }
}
