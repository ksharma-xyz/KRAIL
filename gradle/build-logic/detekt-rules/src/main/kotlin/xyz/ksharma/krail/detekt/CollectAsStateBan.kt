package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Bans `collectAsState()`; `collectAsStateWithLifecycle()` is the only allowed form.
 *
 * `collectAsState` keeps collecting while the screen is in the background, so a polling flow
 * behind it keeps firing network requests with the app not visible, and a `WhileSubscribed`
 * upstream never sees its subscriber count drop. `collectAsStateWithLifecycle` stops collecting
 * at `STOPPED` and restarts at `STARTED`, which is what every backgrounding rule in
 * `docs/POLLING_LIFECYCLE.md` assumes.
 *
 * There are zero offenders today — this rule exists to keep it that way, since the wrong function
 * is one autocomplete away from the right one and the difference is invisible in review.
 *
 * The import is flagged as well as the call: an unused-but-present import is the residue of a
 * half-finished migration and will be completed by the next person who needs a flow here.
 */
class CollectAsStateBan(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "CollectAsStateBan",
        severity = Severity.Defect,
        description = "collectAsState() keeps collecting while the screen is backgrounded; " +
            "use collectAsStateWithLifecycle().",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        if (callee != BANNED_FUNCTION) return
        report(CodeSmell(issue, Entity.from(expression), MESSAGE))
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        if (importDirective.importedFqName?.shortName()?.asString() != BANNED_FUNCTION) return
        report(CodeSmell(issue, Entity.from(importDirective), MESSAGE))
    }

    private companion object {
        const val BANNED_FUNCTION = "collectAsState"

        const val MESSAGE = "'collectAsState()' keeps collecting while the screen is " +
            "backgrounded, so polling never pauses and a WhileSubscribed upstream never sees " +
            "its subscriber count drop. Use 'collectAsStateWithLifecycle()' — see " +
            "docs/POLLING_LIFECYCLE.md."
    }
}
