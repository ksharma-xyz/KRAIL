package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

/** How the two buttons are laid out for the room there is. */
internal sealed interface IntroBarPlan {
    /** Side by side, left then right, at these widths. They and the gap fill the room. */
    data class OneRow(val leftWidth: Int, val rightWidth: Int) : IntroBarPlan

    /** One above the other at the full width, the negative button on top. */
    data object Stacked : IntroBarPlan
}

/**
 * Chooses the plan from what each button needs to show its whole label on one line ([leftNatural] and
 * [rightNatural], in pixels), the [gap] between them and the room [available].
 *
 * - If two equal halves are wide enough for the longer one, the bar is an even split.
 * - Otherwise, if the two together fit, each keeps what it needs and the room left over is shared
 *   equally, so the longer label is never given less than it needs.
 * - Otherwise the buttons stack. A label is never allowed less room than it needs in a row.
 */
internal fun introBarPlan(leftNatural: Int, rightNatural: Int, gap: Int, available: Int): IntroBarPlan {
    val half = (available - gap) / 2
    return when {
        max(leftNatural, rightNatural) <= half -> IntroBarPlan.OneRow(half, available - gap - half)
        leftNatural + rightNatural + gap <= available -> {
            val extra = (available - gap - leftNatural - rightNatural) / 2
            IntroBarPlan.OneRow(leftNatural + extra, available - gap - (leftNatural + extra))
        }
        else -> IntroBarPlan.Stacked
    }
}

/**
 * The width a button needs to show [label] on one line: the label as the button draws it, in the button's own text style and at
 * the text size in [scope], and the button's padding each side. It is measured from the real text, never wrapped.
 */
internal fun introButtonNaturalWidth(
    label: String,
    measurer: TextMeasurer,
    style: TextStyle,
    sidePaddingPx: Int,
    scope: MeasureScope,
): Int = measurer.measure(
    text = AnnotatedString(label),
    style = style,
    softWrap = false,
    maxLines = 1,
    constraints = Constraints(),
    layoutDirection = scope.layoutDirection,
    density = scope,
).size.width + sidePaddingPx
