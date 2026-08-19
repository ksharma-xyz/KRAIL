package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import java.io.File
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Enforces CLAUDE.md's zero-tolerance rule: `CyclomaticComplexMethod` may never be suppressed.
 *
 * The rule is worth having because a complexity limit is the only check in the build that pushes
 * back on a method growing one branch at a time, and it is also the easiest check to silence —
 * `@Suppress` is one line, and `detektBaseline` regenerates a whole module's suppressions in one
 * command without anyone reading them. Once silenced the method never gets smaller.
 *
 * Two suppression routes are covered:
 *
 * 1. **`@Suppress("CyclomaticComplexMethod")` in source**, including the `@file:` form, which is
 *    the widest possible version: it exempts every method in the file, forever, including ones
 *    written years later.
 * 2. **A `baseline.xml` entry**, which detekt treats as a permanent suppression. Detekt gives a
 *    rule no access to the build's baseline configuration, so the nearest ancestor `baseline.xml`
 *    of the analysed file is read directly (this matches `Detekt.kt`, which points the baseline at
 *    each module's own `projectDir`). A baseline ID carries the offending file's name, so the
 *    finding is reported against that file when it is visited — which also means each entry is
 *    reported exactly once, with no need to deduplicate across the module's files.
 *
 * The fix in both cases is to refactor: extract cohesive blocks into well-named private functions,
 * replacing an if/else-if ladder with a `when` or a lookup map, or splitting a Composable that
 * renders many conditional sections. Note that detekt counts branches inside a nested local `fun`
 * toward the enclosing function, so extraction has to be to a top level — in a separate file if
 * the current one is also at its `TooManyFunctions` limit.
 *
 * ## The one baseline, and why it is closed
 *
 * [BASELINE_FILE] holds the `@Suppress` sites that pre-date this rule, so the gate could be turned
 * on without rewriting live code in the same change. It is a list to empty, not a list to add to;
 * its header says so and every new suppression fails the build. Baseline **xml** entries have no
 * such escape hatch — there are none today and the rule rejects them outright.
 */
class CyclomaticSuppressBan(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "CyclomaticSuppressBan",
        severity = Severity.Defect,
        description = "CyclomaticComplexMethod must never be suppressed — not by @Suppress, " +
            "not by a baseline.xml entry. Refactor the method instead.",
        debt = Debt.TWENTY_MINS,
    )

    private val baselineCache = HashMap<String, Set<String>>()

    private val baseline = RatchetBaseline(BASELINE_FILE)

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        if (annotationEntry.shortName?.asString() != SUPPRESS) return

        val suppressesComplexity = annotationEntry.valueArguments.any { argument ->
            val expression = argument.getArgumentExpression() as? KtStringTemplateExpression
            expression?.entries?.singleOrNull()?.text == RULE_NAME
        }
        if (!suppressesComplexity) return
        if (annotationEntry.isGrandfathered()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(annotationEntry),
                message = "@Suppress(\"$RULE_NAME\") is banned — CLAUDE.md sets zero tolerance " +
                    "for suppressing it, because a silenced complexity limit never gets " +
                    "un-silenced. Refactor the method: extract cohesive blocks to top-level " +
                    "private functions (a local 'fun' does not reduce the count), or replace " +
                    "the branch ladder with a 'when' or a lookup map.",
            ),
        )
    }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val baselined = baselineEntriesFor(file)
        baselined.filter { it.fileNameOfBaselineId() == file.name }.forEach { entry ->
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(file),
                    message = "'${file.name}' has a $RULE_NAME entry in its module " +
                        "baseline.xml ($entry). Baselining is suppression, and CLAUDE.md sets " +
                        "zero tolerance for it here. Refactor the method and delete the " +
                        "baseline entry in the same change.",
                ),
            )
        }
    }

    /**
     * True when this exact suppression site is listed in [BASELINE_FILE], keyed as
     * `<repo-relative path>|<annotated declaration name>` — or `|(file)` for a `@file:Suppress`,
     * whose "declaration" is the whole file.
     */
    private fun KtAnnotationEntry.isGrandfathered(): Boolean {
        val ktFile = containingKtFile
        val path = baseline.relativePath(ktFile) ?: return false
        val target = getStrictParentOfType<KtNamedDeclaration>()?.name ?: FILE_TARGET
        return baseline.contains(ktFile, "$path|$target")
    }

    /** IDs of the form `CyclomaticComplexMethod:...` in the nearest ancestor `baseline.xml`. */
    @Synchronized
    private fun baselineEntriesFor(file: KtFile): Set<String> {
        val baselineFile = nearestBaseline(File(file.virtualFilePath).absoluteFile)
            ?: return emptySet()
        return baselineCache.getOrPut(baselineFile.path) {
            ID_PATTERN.findAll(baselineFile.readText())
                .map { it.groupValues[1] }
                .filter { it.startsWith("$RULE_NAME:") }
                .toSet()
        }
    }

    private fun nearestBaseline(from: File): File? {
        var dir: File? = from.parentFile
        while (dir != null) {
            val candidate = File(dir, BASELINE_FILE_NAME)
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        return null
    }

    /** A baseline ID is `RuleName:FileName.kt` then `$`-separated context; take the file name. */
    private fun String.fileNameOfBaselineId(): String =
        substringAfter(':').substringBefore('$')

    companion object {
        const val BASELINE_FILE = "cyclomatic-suppress-baseline.txt"

        private const val FILE_TARGET = "(file)"
        private const val SUPPRESS = "Suppress"
        private const val RULE_NAME = "CyclomaticComplexMethod"
        private const val BASELINE_FILE_NAME = "baseline.xml"

        private val ID_PATTERN = Regex("<ID>(.*?)</ID>", RegexOption.DOT_MATCHES_ALL)
    }
}
