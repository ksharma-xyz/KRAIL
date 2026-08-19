package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

/**
 * Every `item { }` in a lazy list must pass a `key`.
 *
 * Without one, Compose identifies a slot by its position. Insert a row above it, reorder the list,
 * or let a section appear and disappear, and the slot's remembered state — scroll position,
 * expanded/collapsed flags, running animations — is handed to a different row. This is CLAUDE.md's
 * "LazyColumn / LazyRow item keys" rule.
 *
 * ## Scope: `item { }` only, for now
 *
 * The rule currently covers the no-argument `item { }` form, which is the one that is unambiguously
 * wrong and by far the most common. `items(...)` is deliberately left out: its keyed and unkeyed
 * forms are told apart by argument names spread across several lines and it has overloads taking a
 * count, a list, or an array, so a syntactic check produces false positives on the multi-line
 * calls that make up most of this codebase's usage. Extending to `items(...)` is a follow-up; do it
 * by matching named arguments on the resolved call rather than by widening this text-level match.
 *
 * ## Detecting the lazy scope
 *
 * `item` is only meaningful inside `LazyListScope` and friends, so the rule requires the call to
 * be lexically inside one of [LAZY_CONTAINERS], or inside a function that declares a lazy scope as
 * its extension receiver (the common "extract a section into a private fun LazyListScope.foo()"
 * shape). Anything else named `item` is left alone.
 *
 * Pre-existing offenders are grandfathered in [BASELINE_FILE] as `<repo-relative path>|<enclosing
 * function name>|<count>`, where the count is how many unkeyed `item { }` calls that function is
 * allowed. Fixing one means decrementing the count, and deleting the line at zero, so the file only
 * ever shrinks.
 */
class LazyItemKeyRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "LazyItemKeyRule",
        severity = Severity.Defect,
        description = "A lazy list 'item { }' without a key is identified by position, so " +
            "its remembered state moves to a different row when the list changes.",
        debt = Debt.FIVE_MINS,
    )

    private val baseline = RatchetBaseline(BASELINE_FILE)

    /** Unkeyed `item { }` calls already seen per enclosing function, to spend the allowance. */
    private val seenPerFunction = HashMap<String, Int>()

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeExpression?.text != ITEM) return
        // A key is always passed inside the parentheses — `item("spacer") { }` or
        // `item(key = it.id) { }` — while the content is the trailing lambda, which is not part
        // of the argument list. So an empty argument list is exactly the unkeyed form.
        if (expression.valueArgumentList?.arguments.orEmpty().isNotEmpty()) return
        if (!expression.isInLazyScope()) return
        if (expression.isAllowedByBaseline()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "This 'item { }' has no key, so Compose identifies it by position: " +
                    "inserting or reordering a row hands its remembered state (scroll, " +
                    "expanded flags, running animations) to a different row. Pass a stable " +
                    "key — a descriptive literal for a static item, a domain id for a dynamic " +
                    "one. See CLAUDE.md, 'LazyColumn / LazyRow item keys'.",
            ),
        )
    }

    private fun KtCallExpression.isInLazyScope(): Boolean {
        val insideLazyContainer = parentsWithSelf
            .filterIsInstance<KtCallExpression>()
            .any { it.calleeExpression?.text in LAZY_CONTAINERS }
        if (insideLazyContainer) return true

        val receiver = getStrictParentOfType<KtNamedFunction>()?.receiverTypeReference?.text
        return receiver != null && LAZY_SCOPES.any { receiver.contains(it) }
    }

    private fun KtCallExpression.isAllowedByBaseline(): Boolean {
        val ktFile = containingKtFile
        val path = baseline.relativePath(ktFile) ?: return false
        val function = getStrictParentOfType<KtNamedFunction>()?.name ?: return false
        val key = "$path|$function"
        val allowance = (MAX_BASELINE_ALLOWANCE downTo 1)
            .firstOrNull { baseline.contains(ktFile, "$key|$it") }
            ?: return false
        val soFar = seenPerFunction.merge(key, 1, Int::plus) ?: 1
        return soFar <= allowance
    }

    companion object {
        const val BASELINE_FILE = "lazy-item-key-baseline.txt"

        /**
         * Upper bound on a single function's grandfathered allowance. Nothing in this codebase is
         * near it; it only bounds the lookup so the baseline entry can record a count rather than
         * a churn-prone line number.
         */
        private const val MAX_BASELINE_ALLOWANCE = 32

        private const val ITEM = "item"

        private val LAZY_CONTAINERS = setOf(
            "LazyColumn",
            "LazyRow",
            "LazyVerticalGrid",
            "LazyHorizontalGrid",
            "LazyVerticalStaggeredGrid",
            "LazyHorizontalStaggeredGrid",
        )

        private val LAZY_SCOPES = listOf(
            "LazyListScope",
            "LazyGridScope",
            "LazyStaggeredGridScope",
        )
    }
}
