package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.math.max

/** The least height of a button of the bar, the size of a touch target. The taj large button is a little under it at 100 percent text. */
internal val IntroMinButtonHeight = 48.dp

/**
 * The bar at the bottom of every Intro screen that asks something: two taj buttons, the negative
 * answer in outline and the positive one filled. The positive one carries the `intro.action.primary` tag that the Maestro flow
 * taps and, except on the colour screen, is filled in [primaryFill], the screen's own colour made deep enough for a white label,
 * and its label is white.
 *
 * With no [left], which the screens that ask for a pick have, the bar is the one positive button at the full width.
 * It is the same taj button, so it is the same height as the two button bar, and nothing is left empty beside it.
 *
 * No button is ever drawn disabled. A button is live, or it is not shown. While [shown] is false the bar is not there for
 * the rider: it draws nothing, a screen reader and a tap cannot reach it, but it still takes the room its buttons will, so
 * the screen does not move when it appears. It then fades in over [BAR_REVEAL_MS], or simply appears with reduced
 * motion. [rightShown] does the same for the right button alone, which the Park & Ride bar keeps hidden until its card
 * shows.
 *
 * [acceptsPresses] is false while the screen the bar belongs to is still moving, so a second tap on a bar that changes in
 * place is dropped and never lands on the next screen's button. The bar is drawn exactly as it is, live. A button that has
 * not yet fully faded in takes no press either.
 *
 * The two are in one row, the negative on the left, when both labels fit side by side on one line each at the text size and in
 * the room there are. Otherwise the bar is vertical: the negative button on top and the positive one under it, the same width
 * and the full width of the bar, with the usual gap between them. A label is never wrapped, squeezed, shrunk or cut to make a row
 * fit. The choice is made once for the whole bar, from the real width of every label the screen's bar can show ([labelPairs]), so
 * it does not change from one state of a screen to the next and nothing above it moves. It never holds more than two buttons. The
 * reading order is the negative button and then the positive one, in a row and stacked.
 */
@Composable
internal fun IntroDecisionBar(
    left: IntroBarAction?,
    right: IntroBarAction,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    shown: Boolean = true,
    rightShown: Boolean = true,
    acceptsPresses: Boolean = true,
    labelPairs: List<Pair<String, String>> = listOfNotNull(left?.let { it.label to right.label }),
    primaryFill: Color? = null,
) {
    Revealed(shown = shown, acceptsPresses = acceptsPresses, modifier = modifier) {
        if (left == null) {
            PrimaryButton(right, onEvent, primaryFill, Modifier.fillMaxWidth())
        } else {
            IntroBarLayout(gap = KrailTheme.dimensions.spacingL, labelPairs = labelPairs) {
                OutlinedButton(
                    onClick = { onEvent(left.event) },
                    dimensions = ButtonDefaults.largeButtonSize(),
                    modifier = Modifier
                        .heightIn(min = IntroMinButtonHeight)
                        .testTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
                        .semantics { contentDescription = left.spokenLabel },
                ) {
                    Text(text = left.label, textAlign = TextAlign.Center)
                }
                // The room is kept while it is hidden, and a layout that gives the box a tight width gives it to the button.
                Revealed(shown = rightShown, propagateMinConstraints = true) {
                    PrimaryButton(right, onEvent, primaryFill)
                }
            }
        }
    }
}

/**
 * Draws [content] when [shown] and, when not, keeps its room and nothing else: it is transparent, taps are taken before the
 * content sees them, the keyboard cannot focus it and a screen reader finds nothing. It fades between the two over
 * [introBarRevealMs] and is never drawn half way in a preview or a screenshot, where the state it starts in is the state it
 * is drawn in. The content is never disabled: while it fades in, and while [acceptsPresses] is false, the touch is consumed
 * instead, so a press that is not ready is ignored without the button looking off.
 */
@Composable
private fun Revealed(
    shown: Boolean,
    modifier: Modifier = Modifier,
    acceptsPresses: Boolean = true,
    propagateMinConstraints: Boolean = false,
    content: @Composable () -> Unit,
) {
    val reveal = introBarRevealMs(isReduceMotionEnabled())
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reveal == 0) snap() else tween(durationMillis = reveal),
        label = "intro-bar-reveal",
    )
    val live = shown && alpha == 1f
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(if (shown) Modifier else Modifier.clearAndSetSemantics {})
            .focusProperties { canFocus = live }
            .consumeTouches(!live || !acceptsPresses),
        propagateMinConstraints = propagateMinConstraints,
    ) {
        content()
    }
}

/**
 * The colours of the primary button: the taj button's own for a null [fill], which is the colour screen, and otherwise the [fill] and
 * a white label ([Color.White], the colour of the letter in the transport mode icon), through the one parameter the button exposes.
 */
@Composable
internal fun introPrimaryButtonColors(fill: Color?): ButtonColors =
    if (fill == null) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(customContainerColor = fill, customContentColor = Color.White)
    }

// A taj button colours its own label, so the label carries no colour of its own.
@Composable
private fun PrimaryButton(
    action: IntroBarAction,
    onEvent: (IntroUiEvent) -> Unit,
    fill: Color?,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(action.event) },
        modifier = modifier
            .heightIn(min = IntroMinButtonHeight)
            .testTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
            .semantics { contentDescription = action.spokenLabel },
        colors = introPrimaryButtonColors(fill),
    ) {
        Text(text = action.label, textAlign = TextAlign.Center)
    }
}

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
 * the text size in [density], and the button's padding each side. It is measured from the real text, never wrapped.
 */
internal fun introButtonNaturalWidth(
    label: String,
    measurer: TextMeasurer,
    style: TextStyle,
    sidePaddingPx: Int,
    density: Density,
    layoutDirection: LayoutDirection,
): Int = measurer.measure(
    text = AnnotatedString(label),
    style = style,
    softWrap = false,
    maxLines = 1,
    constraints = Constraints(),
    layoutDirection = layoutDirection,
    density = density,
).size.width + sidePaddingPx

/**
 * Lays the two buttons out as [introBarPlan] says. The first child is the negative button and the second the positive one. In a row
 * they are centred on each other, and stacked the negative one is on top. What each needs is the widest label it can have on
 * the screen, from [labelPairs], so the plan is the same for every state of the screen, and no less than the button in hand needs.
 */
@Composable
private fun IntroBarLayout(
    gap: Dp,
    labelPairs: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    // The text of a large button is the taj title style, and its padding each side is what taj gives it.
    val style = KrailTheme.typography.titleLarge
    val padding = ButtonDefaults.largeButtonSize().padding
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val left = measurables[0]
        val right = measurables[1]
        val side = padding.calculateLeftPadding(layoutDirection).roundToPx() +
            padding.calculateRightPadding(layoutDirection).roundToPx()
        fun natural(label: String) = introButtonNaturalWidth(label, measurer, style, side, this, layoutDirection)
        val leftWidest = labelPairs.maxOfOrNull { natural(it.first) } ?: 0
        val rightWidest = labelPairs.maxOfOrNull { natural(it.second) } ?: 0
        val leftNatural = max(left.maxIntrinsicWidth(Constraints.Infinity), leftWidest)
        val rightNatural = max(right.maxIntrinsicWidth(Constraints.Infinity), rightWidest)
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
                val leftPlaceable = left.measure(tightWidth(available, maxHeight))
                val rightPlaceable = right.measure(tightWidth(available, maxHeight))
                layout(available, leftPlaceable.height + gapPx + rightPlaceable.height) {
                    leftPlaceable.place(0, 0)
                    rightPlaceable.place(0, leftPlaceable.height + gapPx)
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
        val bar = requireNotNull(introBar(PreviewAskState))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightShown = bar.rightShown)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroDecisionBarAddToHome() {
    PreviewTheme {
        val bar = requireNotNull(introBar(PreviewCardContentState))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightShown = bar.rightShown)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewIntroDecisionBarAddToHomeWaiting() {
    PreviewTheme {
        val bar = requireNotNull(introBar(PreviewCardLoadingState))
        IntroDecisionBar(left = bar.left, right = bar.right, onEvent = {}, rightShown = bar.rightShown)
    }
}
