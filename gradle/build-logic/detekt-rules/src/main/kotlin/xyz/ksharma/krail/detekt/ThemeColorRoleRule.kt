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
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * The rider's theme colour is asked for in four situations with four different contrast
 * obligations, so `taj` exposes four accessors instead of one. This holds the split.
 *
 * - `themeGroundColor()` fills a shape that content is drawn on top of, and stays unadapted.
 * - `themeInkColor()` is text, an icon or a stroke drawn onto an app surface, and is adapted.
 * - `themeBackgroundColor()` is the translucent wash used for card fills.
 * - `themeDecorColor()` is a gradient stop or a ripple, with no legibility obligation at all.
 *
 * Two things are flagged.
 *
 * **`themeColor()`**, the deprecated accessor that could not say which of the four it meant. A
 * deprecation warning alone does not hold: warnings are not errors here, so the call would compile
 * and the colour would be wrong only for the themes that happen to fail.
 *
 * **`themeGroundColor()` in an ink position** — passed to a `color =` or `tint =` argument, or into
 * `BorderStroke(`. Reaching past the ink accessor back to the raw colour is exactly how Purple Drip
 * came to be drawn at 2.95:1 on the dark surface, and it reads as deliberate at the call site.
 *
 * `themeDecorColor()` is the sanctioned way out. A gradient stop genuinely has no obligation, and
 * saying so explicitly is what lets this rule stay an unconditional gate with no baseline rather
 * than a list of grandfathered offenders that never shrinks.
 *
 * No type resolution, so the check is syntactic: an argument named `color` or `tint` whose
 * expression mentions `themeGroundColor()`, on a callee that is not one of the fill-taking ones in
 * [FILL_TAKING_CALLEES]. `Modifier.background(color = ...)` takes a ground and
 * `Modifier.border(color = ...)` takes ink, and both name the argument `color`, so the callee has
 * to be part of the decision.
 *
 * Two known limits: a ground colour laundered through a local `val` first is invisible to it, and a
 * fill-taking callee outside that set would false-positive. The accessor split is what makes the
 * common case checkable, not this rule alone.
 */
class ThemeColorRoleRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ThemeColorRoleRule",
        severity = Severity.Defect,
        description = "The theme colour must be read through the accessor for the role it is " +
            "drawn in: themeGroundColor, themeInkColor, themeBackgroundColor or themeDecorColor.",
        debt = Debt.TEN_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text
        if (callee == DEPRECATED_ACCESSOR) {
            report(CodeSmell(issue, Entity.from(expression), deprecatedMessage()))
            return
        }

        // `color =` means a fill on some callees and a stroke on others: Modifier.background()
        // takes a ground, Modifier.border() takes ink, and both name the argument `color`. The
        // argument name alone cannot tell them apart, so callees that take a fill are skipped.
        if (callee in FILL_TAKING_CALLEES) return

        expression.valueArguments.forEach { argument ->
            if (!argument.mentionsGround()) return@forEach
            val position = argument.getArgumentName()?.asName?.asString()
            when {
                position in INK_ARGUMENT_NAMES ->
                    report(CodeSmell(issue, Entity.from(argument), inkMessage(position.orEmpty())))

                position == null && callee == BORDER_STROKE ->
                    report(CodeSmell(issue, Entity.from(argument), inkMessage(BORDER_STROKE)))
            }
        }
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)

        val name = importDirective.importedFqName?.shortName()?.asString() ?: return
        if (name != DEPRECATED_ACCESSOR) return
        report(CodeSmell(issue, Entity.from(importDirective), deprecatedMessage()))
    }

    private fun KtValueArgument.mentionsGround() =
        getArgumentExpression()?.text?.contains(GROUND_ACCESSOR) == true

    private fun deprecatedMessage() = "'$DEPRECATED_ACCESSOR()' does not say which role the " +
        "theme colour is being used in, so nothing can check it. Use themeGroundColor() for a " +
        "fill with content on top, themeInkColor() for text, icons and strokes drawn on a " +
        "surface, themeBackgroundColor() for the translucent wash, or themeDecorColor() for a " +
        "gradient or ripple that nothing is read off."

    private fun inkMessage(position: String) = "'$GROUND_ACCESSOR()' is the unadapted theme " +
        "colour, and '$position' draws ink onto a surface. Some theme colours do not clear WCAG " +
        "AA there. Use themeInkColor(), or themeInkColorOn(background) when the ground is not " +
        "an app surface. If nothing is read off this colour, say so with themeDecorColor()."

    private companion object {
        const val DEPRECATED_ACCESSOR = "themeColor"
        const val GROUND_ACCESSOR = "themeGroundColor"
        const val BORDER_STROKE = "BorderStroke"
        val INK_ARGUMENT_NAMES = setOf("color", "tint", "textColor", "contentColor")

        /**
         * Callees whose `color` argument is a fill with content drawn on top, not ink. The ground
         * accessor is the right choice there, so flagging it would be a false positive with no
         * correct fix available at the call site.
         */
        val FILL_TAKING_CALLEES = setOf("background", "drawRect", "drawCircle", "drawRoundRect")
    }
}
