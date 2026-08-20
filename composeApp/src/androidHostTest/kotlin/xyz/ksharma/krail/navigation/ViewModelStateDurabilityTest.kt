package xyz.ksharma.krail.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Every registered ViewModel either takes a [androidx.lifecycle.SavedStateHandle] or says why
 * it does not need one.
 *
 * A ViewModel survives rotation. It does not survive the process being killed in the
 * background, which is the ordinary end of a session on a phone. Anything held only in a
 * ViewModel field is gone when the rider comes back, and the screen returns empty or wrong
 * with no crash, no log line and nothing for a test to catch. `rememberSaveable` covers the
 * screen's own state; `SavedStateHandle` is the ViewModel's half, and it is the half that gets
 * forgotten because forgetting it looks like nothing.
 *
 * Today no ViewModel in the app takes one, and for the current set that is defensible: their
 * state is re-read from Sandook, Remote Config or the network as soon as something subscribes
 * again, or the rider's input lives in a `rememberSaveable` on the screen. Each of those
 * reasons is written down in [ALLOWLIST_PATH], one line per ViewModel.
 *
 * So this guard is not here to fail today. It is here so that the next ViewModel — the one
 * that does hold something the rider typed — has to be entered as a line in that file by
 * someone who thought about it, rather than shipped by default. It also fails on a stale
 * allowlist line, so the file cannot outlive the ViewModels it excuses.
 *
 * Scope, stated honestly. Registrations are read from SOURCE, because Koin's module contents
 * are not a public API and starting the real graph from a host test would need a `Context`.
 * The constructor check is real reflection on the loaded class. What this cannot judge is
 * whether a stated reason is TRUE: "it is refetched on restore" is a claim a human makes and
 * a human has to keep honest.
 */
class ViewModelStateDurabilityTest {

    @Test
    fun `every registered ViewModel either saves its state or explains why it need not`() {
        val root = repoRoot()
        val allowed = readAllowlist(root)
        val registered = scanKoinRegistrations(root)

        if (registered.isEmpty()) {
            fail(
                "Found no ViewModel registrations at all. This test scans Koin module source " +
                    "for viewModel { } / viewModelOf(::X); if the registration style changed, " +
                    "the scan has to change with it or this guard silently checks nothing.",
            )
        }

        val undeclared = registered.keys
            .filterNot { it in allowed }
            .filterNot { registered.getValue(it).takesSavedStateHandle() }
            .sorted()

        val stale = (allowed.keys - registered.keys).sorted()

        if (undeclared.isEmpty() && stale.isEmpty()) return

        fail(
            buildString {
                if (undeclared.isNotEmpty()) {
                    appendLine("These ViewModels take no SavedStateHandle and are not listed in")
                    appendLine("$ALLOWLIST_PATH:")
                    undeclared.forEach { appendLine("  $it (${registered.getValue(it)})") }
                    appendLine()
                    appendLine("A ViewModel outlives a rotation but not the process being killed in")
                    appendLine("the background. If it holds something the rider would notice losing,")
                    appendLine("take a SavedStateHandle. If it does not, add a line to the allowlist")
                    appendLine("saying what re-reads that state on the way back.")
                }
                if (stale.isNotEmpty()) {
                    appendLine()
                    appendLine("$ALLOWLIST_PATH excuses ViewModels that are no longer registered:")
                    stale.forEach { appendLine("  $it") }
                    appendLine("Remove them, so the list keeps meaning what it says.")
                }
            },
        )
    }

    /** True when any constructor of the class takes a `SavedStateHandle`. */
    private fun String.takesSavedStateHandle(): Boolean {
        val loaded = runCatching {
            Class.forName(this, false, ViewModelStateDurabilityTest::class.java.classLoader)
        }.getOrElse {
            fail(
                "Could not load '$this' to inspect its constructor. It was found as a Koin " +
                    "registration, so either the import-based name resolution in this test is " +
                    "wrong, or :composeApp no longer has that module on its test classpath.",
            )
        }

        // Every constructor, not just the primary one. A secondary constructor taking a
        // SavedStateHandle is still a ViewModel that saves its state, and refusing to see it
        // would push someone towards suppressing this test rather than reading it.
        return loaded.constructors.any { constructor ->
            constructor.parameterTypes.any { it.name == SAVED_STATE_HANDLE }
        }
    }

    /**
     * `<SimpleName>` to `<fully qualified name>`, for every ViewModel registered in a Koin
     * module in production source.
     *
     * Two registration shapes exist here: `viewModelOf(::X)`, and `viewModel { … X(…) }` where
     * the type is inferred from the constructor call inside the lambda. Test-only modules are
     * excluded by source set, so the fakes registered by the flow tests do not count as the
     * app registering a ViewModel.
     */
    private fun scanKoinRegistrations(root: File): Map<String, String> {
        val found = mutableMapOf<String, String>()

        root.productionSources().forEach { file ->
            val text = file.readText().stripComments()
            if (!text.contains(KOIN_MODULE_MARKER)) return@forEach

            val names = buildSet {
                VIEW_MODEL_OF.findAll(text).forEach { add(it.groupValues[1]) }
                text.viewModelLambdaBodies().forEach { body ->
                    CONSTRUCTOR_CALL.findAll(body).forEach { add(it.groupValues[1]) }
                }
            }

            names.forEach { name -> found[name] = text.resolveFqn(name) }
        }
        return found
    }

    /** The brace-matched body of every `viewModel { … }` / `viewModel<T> { … }` in [this]. */
    private fun String.viewModelLambdaBodies(): List<String> =
        VIEW_MODEL_LAMBDA.findAll(this).mapNotNull { match ->
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
            substring(open, (cursor + 1).coerceAtMost(length))
        }.toList()

    /** Resolves [name] against the file's imports, falling back to its own package. */
    private fun String.resolveFqn(name: String): String {
        Regex("""^import\s+([\w.]+\.$name)\s*$""", RegexOption.MULTILINE)
            .find(this)
            ?.let { return it.groupValues[1] }

        val packageName = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
            .find(this)
            ?.groupValues
            ?.get(1)
            .orEmpty()

        return if (packageName.isEmpty()) name else "$packageName.$name"
    }

    /** `<ViewModel simple name>` to `<reason>`, with `#` comments and blank lines dropped. */
    private fun readAllowlist(root: File): Map<String, String> {
        val file = File(root, ALLOWLIST_PATH)
        if (!file.isFile) fail("$ALLOWLIST_PATH not found — this test reads the list it guards.")

        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                val name = line.substringBefore('#').trim()
                val reason = line.substringAfter('#', "").trim()
                if (reason.isEmpty()) {
                    fail(
                        "$ALLOWLIST_PATH entry '$name' has no reason after the '#'. The reason " +
                            "is the whole point of the line: it is what someone reads before " +
                            "adding the next one.",
                    )
                }
                name to reason
            }
    }

    private fun String.stripComments(): String =
        replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private fun File.productionSources(): Sequence<File> = walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" }
        .filter { it.isFile && it.extension == "kt" }
        .filter { it.invariantPath().contains("/src/") }
        .filterNot { TEST_SOURCE_DIR.containsMatchIn(it.invariantPath()) }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private fun repoRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
        }
        fail("Could not find the repo root above ${File(".").absolutePath}")
    }

    private companion object {
        const val ALLOWLIST_PATH = "config/no-saved-state-allowlist.txt"
        const val SAVED_STATE_HANDLE = "androidx.lifecycle.SavedStateHandle"

        // Cheap pre-filter: only files that actually declare a Koin module are worth walking.
        const val KOIN_MODULE_MARKER = "module {"

        val VIEW_MODEL_OF = Regex("""viewModelOf\(\s*::\s*([A-Z][A-Za-z0-9_]*ViewModel)\s*\)""")
        val VIEW_MODEL_LAMBDA = Regex("""\bviewModel\s*(?:<[^>]+>)?\s*(?:\([^)]*\))?\s*\{""")
        val CONSTRUCTOR_CALL = Regex("""\b([A-Z][A-Za-z0-9_]*ViewModel)\s*\(""")

        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")
        val TEST_SOURCE_DIR = Regex("""/src/[^/]*[Tt]est[^/]*/""")
    }
}
