package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.math.max

/**
 * The bar at the bottom of every Intro screen that asks something: two taj buttons, the negative
 * answer on the left in outline and the positive one on the right in the theme colour. The right one
 * carries the `intro.action.primary` tag that the Maestro flow taps.
 *
 * With no [left], which only the theme screen has, the bar is the one positive button at the full width.
 * It is the same taj button, so it is the same height as the two button bar, and nothing is left empty beside it.
 *
 * The two sit in one row. When two whole buttons cannot fit side by side, as at 320 dp with large text,
 * the bar stacks them with the primary on top, and never squeezes, shrinks or truncates a label. It
 * never holds more than two buttons. Above the large text threshold the labels that have a compact form
 * show it, and every button keeps its whole phrase as its description for a screen reader.
 */
@Composable
internal fun IntroDecisionBar(
    left: IntroBarAction?,
    right: IntroBarAction,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    rightEnabled: Boolean = true,
) {
    val fontScale = LocalDensity.current.fontScale
    if (left == null) {
        PrimaryButton(right, onEvent, rightEnabled, fontScale, modifier.fillMaxWidth())
    } else {
        IntroBarLayout(gap = KrailTheme.dimensions.spacingL, modifier = modifier) {
            OutlinedButton(
                onClick = { onEvent(left.event) },
                dimensions = ButtonDefaults.largeButtonSize(),
                modifier = Modifier
                    .testTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
                    .semantics { contentDescription = left.label },
            ) {
                Text(text = left.visibleLabel(fontScale), textAlign = TextAlign.Center)
            }
            PrimaryButton(right, onEvent, rightEnabled, fontScale)
        }
    }
}

// A taj button colours its own label, so the label carries no colour of its own.
@Composable
private fun PrimaryButton(
    action: IntroBarAction,
    onEvent: (IntroUiEvent) -> Unit,
    enabled: Boolean,
    fontScale: Float,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(action.event) },
        enabled = enabled,
        modifier = modifier
            .testTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
            .semantics { contentDescription = action.label },
    ) {
        Text(text = action.visibleLabel(fontScale), textAlign = TextAlign.Center)
    }
}

/** How the two buttons are laid out for the room there is. */
internal sealed interface IntroBarPlan {
    /** Side by side, left then right, at these widths. They and the gap fill the room. */
    data class OneRow(val leftWidth: Int, val rightWidth: Int) : IntroBarPlan

    /** One above the other at the full width, the primary on top. */
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
 * Lays the two buttons out as [introBarPlan] says. The first child is the left button and the second
 * the right one. In a row they are centred on each other, and stacked the right one is first.
 */
@Composable
private fun IntroBarLayout(gap: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val left = measurables[0]
        val right = measurables[1]
        val leftNatural = left.maxIntrinsicWidth(Constraints.Infinity)
        val rightNatural = right.maxIntrinsicWidth(Constraints.Infinity)
        val available = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            leftNatural + gapPx + rightNatural
        }
        val maxHeight = constraints.maxHeight

        when (val plan = introBarPlan(leftNatural, rightNatural, gapPx, available)) {
            is IntroBarPlan.OneRow -> {
                val leftPlaceable = left.measure(tightWidth(plan.leftWidth, maxHeight))
                val rightPlaceable = right.measure(tightWidth(plan.rightWidth, maxHeight))
                val height = max(leftPlaceable.height, rightPlaceable.height)
                layout(available, height) {
                    leftPlaceable.place(0, (height - leftPlaceable.height) / 2)
                    rightPlaceable.place(plan.leftWidth + gapPx, (height - rightPlaceable.height) / 2)
                }
            }

            IntroBarPlan.Stacked -> {
                val rightPlaceable = right.measure(tightWidth(available, maxHeight))
                val leftPlaceable = left.measure(tightWidth(available, maxHeight))
                layout(available, rightPlaceable.height + gapPx + leftPlaceable.height) {
                    rightPlaceable.place(0, 0)
                    leftPlaceable.place(0, rightPlaceable.height + gapPx)
                }
            }
        }
    }
}

private fun tightWidth(width: Int, maxHeight: Int) =
    Constraints(minWidth = width, maxWidth = width, maxHeight = maxHeight)

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroDecisionBarAsk() {
    PreviewTheme {
        val bar = requireNotNull(introBar(PreviewAskState, PreviewReady))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightEnabled = bar.rightEnabled)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroDecisionBarAddToHome() {
    PreviewTheme {
        val bar = requireNotNull(introBar(PreviewCardContentState, PreviewReady))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightEnabled = bar.rightEnabled)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroDecisionBarAddToHomeWaiting() {
    PreviewTheme {
        val bar = requireNotNull(introBar(PreviewCardLoadingState, PreviewReady))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightEnabled = bar.rightEnabled)
    }
}
