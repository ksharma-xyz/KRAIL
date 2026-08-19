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
 * Snackbars are not part of this product's vocabulary and must not be introduced.
 *
 * KRAIL confirms and undoes destructive actions in place: long-press to enter edit mode, then a
 * visible affordance on the row itself, with a `ModalBottomSheet` when a decision genuinely needs
 * confirming. A snackbar puts the undo somewhere else, on a timer, over the content the rider is
 * reading — and on this app's screens it lands on top of the bottom-anchored controls.
 *
 * There are zero offenders today. The rule exists because `Scaffold { snackbarHost = ... }` is the
 * default answer in every Compose sample, so the pattern arrives by copy-paste rather than by
 * decision.
 *
 * Both the import and the call site are flagged, so a half-removed snackbar cannot linger as an
 * import waiting to be re-used.
 */
class SnackbarBan(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SnackbarBan",
        severity = Severity.Style,
        description = "Snackbars are banned in this codebase; confirm and undo in place " +
            "instead (long-press edit mode, or a ModalBottomSheet).",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        if (!callee.mentionsSnackbar()) return
        report(CodeSmell(issue, Entity.from(expression), message(callee)))
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val name = importDirective.importedFqName?.shortName()?.asString() ?: return
        if (!name.mentionsSnackbar()) return
        report(CodeSmell(issue, Entity.from(importDirective), message(name)))
    }

    private fun String.mentionsSnackbar() = contains(SNACKBAR, ignoreCase = true)

    private fun message(name: String) = "'$name' introduces a snackbar, which is banned in " +
        "this codebase: it puts undo on a timer, somewhere other than the thing being " +
        "undone, on top of the bottom-anchored controls. Use long-press to enter edit mode " +
        "with an in-place action, or a ModalBottomSheet when a decision needs confirming."

    private companion object {
        const val SNACKBAR = "Snackbar"
    }
}
