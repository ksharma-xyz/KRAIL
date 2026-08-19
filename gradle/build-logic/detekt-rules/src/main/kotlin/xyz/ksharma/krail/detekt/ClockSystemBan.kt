package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Flags an inline `Clock.System` read in production code.
 *
 * A function that reads the wall clock itself cannot be tested at a chosen instant: every
 * assertion about "yesterday", "in 3 minutes" or "peak hour" then depends on when the suite
 * happens to run. The fix is always the same shape — take the time as a dependency.
 *
 * ## The two sanctioned shapes (both exempt)
 *
 *  1. **A parameter default value** — `fun f(now: Instant = Clock.System.now())` or
 *     `clock: Clock = Clock.System`. The production call site stays terse while a test can pass
 *     a fixed instant. Detected structurally (the expression sits inside a [KtParameter]'s
 *     `defaultValue`), so it covers constructor parameters, function parameters and
 *     `@Composable` parameters alike.
 *  2. **A Koin binding** — a file in a `di` package that imports `org.koin.*`, i.e. the
 *     `single<Clock> { Clock.System }` that puts the real clock in the graph exactly once.
 *
 * Test sources are exempt entirely: a test reading the real clock is choosing its own input.
 * Exemption is by KMP source-set directory (`commonTest`, `androidHostTest`, …), never by file
 * name — `Foo.kt` under `commonMain` is production however it is named.
 *
 * ## Also flagged: the import alias evasion
 *
 * `import kotlin.time.Clock.System` followed by a bare `System.now()` reads the wall clock
 * without the text `Clock.System` appearing at the call site, which is exactly the shape a
 * text-search ban would miss. The import itself is reported.
 *
 * ## Baseline
 *
 * Pre-existing offenders live in `config/clock-system-baseline.txt` as `path|count` rows. The
 * count is an allowance: findings *beyond* it are reported, so a baselined file cannot quietly
 * grow a new inline read. The file is shrink-only — see its header. Line numbers are
 * deliberately not recorded; they churn on every unrelated edit and a stale baseline is worse
 * than none. The trade-off: in a baselined file the reported location is whichever read comes
 * last in the file, not necessarily the one just added — the count is what moved, not the line.
 *
 * The baseline is located by walking up from the analysed file until a directory containing
 * `config/clock-system-baseline.txt` is found, because a detekt rule is handed no project root.
 */
class ClockSystemBan(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ClockSystemBan",
        severity = Severity.Defect,
        description = MESSAGE,
        debt = Debt.TWENTY_MINS,
    )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val path = file.name.replace('\\', '/')
        if (isTestSource(path)) return
        if (file.isKoinBindingFile()) return

        val offenders: List<PsiElement> = (
            file.collectDescendantsOfType<KtImportDirective> { it.isClockSystemImport() } +
                file.collectDescendantsOfType<KtDotQualifiedExpression> {
                    it.isClockSystemRead() &&
                        !it.isInsideParameterDefaultValue() &&
                        !it.isInsideImportDirective()
                }
            ).sortedBy { it.startOffset }

        offenders.drop(allowanceFor(path)).forEach { element ->
            report(CodeSmell(issue = issue, entity = Entity.from(element), message = MESSAGE))
        }
    }

    /** `Clock.System`, including a fully qualified `kotlin.time.Clock.System`. */
    private fun KtDotQualifiedExpression.isClockSystemRead(): Boolean {
        val selector = selectorExpression as? KtNameReferenceExpression ?: return false
        if (selector.getReferencedName() != CLOCK_SYSTEM_MEMBER) return false
        return receiverExpression.text.substringAfterLast('.').trim() == CLOCK_TYPE
    }

    /**
     * An import's `importedReference` is itself a dot-qualified expression, so without this the
     * alias evasion would be counted twice — once as the import, once as its reference.
     */
    private fun PsiElement.isInsideImportDirective(): Boolean {
        var current: PsiElement? = parent
        while (current != null && current !is KtFile) {
            if (current is KtImportDirective) return true
            current = current.parent
        }
        return false
    }

    /** `import kotlin.time.Clock.System` — the bare-`System.now()` evasion. */
    private fun KtImportDirective.isClockSystemImport(): Boolean =
        importedFqName?.asString()?.endsWith(".$CLOCK_TYPE.$CLOCK_SYSTEM_MEMBER") == true

    /**
     * True when this expression IS a parameter's default value, or sits inside one. Walking up
     * rather than down means a default value built out of several expressions
     * (`Clock.System.now().plus(1.minutes)`) is exempt as a whole.
     */
    private fun PsiElement.isInsideParameterDefaultValue(): Boolean {
        var current: PsiElement = this
        while (current !is KtFile) {
            val parent = current.parent ?: return false
            if (parent is KtParameter && parent.defaultValue === current) return true
            current = parent
        }
        return false
    }

    private fun KtFile.isKoinBindingFile(): Boolean {
        val inDiPackage = packageFqName.pathSegments().any { it.asString() == DI_PACKAGE_SEGMENT }
        if (!inDiPackage) return false
        return importDirectives.any {
            it.importedFqName?.asString()?.startsWith(KOIN_PACKAGE_PREFIX) == true
        }
    }

    /**
     * By KMP source-set directory only. Detekt hands rules the file's absolute path as
     * `KtFile.name`, so the source set is always readable from it.
     */
    private fun isTestSource(path: String): Boolean =
        TEST_SOURCE_SET_MARKERS.any { path.contains(it) }

    /** How many pre-existing findings this file is allowed before the rule reports. */
    private fun allowanceFor(path: String): Int {
        val root = repoRootFor(path) ?: return 0
        val entries = BASELINE_CACHE.getOrPut(root) { loadBaseline(File(root, BASELINE_PATH)) }
        return entries[path.removePrefix(root).trimStart('/')] ?: 0
    }

    private companion object {

        const val MESSAGE: String =
            "Read time from the injected Clock (constructor param or `now:` parameter), never " +
                "Clock.System inline — inline reads are untestable. See core/date-time's " +
                "dateTimeModule KDoc."

        const val CLOCK_TYPE = "Clock"
        const val CLOCK_SYSTEM_MEMBER = "System"
        const val DI_PACKAGE_SEGMENT = "di"
        const val KOIN_PACKAGE_PREFIX = "org.koin"
        const val BASELINE_PATH = "config/clock-system-baseline.txt"

        val TEST_SOURCE_SET_MARKERS = listOf(
            "/commonTest/",
            "/androidHostTest/",
            "/androidUnitTest/",
            "/androidInstrumentedTest/",
            "/iosTest/",
            "/jvmTest/",
            "/src/test/",
            "/src/androidTest/",
        )

        /** Keyed by repo root, so one parse serves every module in a build. */
        val BASELINE_CACHE = ConcurrentHashMap<String, Map<String, Int>>()
        val ROOT_CACHE = ConcurrentHashMap<String, String>()

        /** Nearest ancestor directory holding [BASELINE_PATH], or null outside a checkout. */
        fun repoRootFor(path: String): String? {
            val start = File(path).parentFile ?: return null
            ROOT_CACHE[start.path]?.let { return it }
            var dir: File? = start
            while (dir != null) {
                if (File(dir, BASELINE_PATH).isFile) {
                    val root = dir.path.replace('\\', '/')
                    ROOT_CACHE[start.path] = root
                    return root
                }
                dir = dir.parentFile
            }
            return null
        }

        fun loadBaseline(file: File): Map<String, Int> {
            if (!file.isFile) return emptyMap()
            return file.readLines()
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() && it.contains('|') }
                .associate { line ->
                    line.substringBefore('|').trim() to
                        (line.substringAfter('|').trim().toIntOrNull() ?: 0)
                }
        }
    }
}
