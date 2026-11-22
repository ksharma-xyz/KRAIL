package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.components.TextFieldPlaceholderColors
import xyz.ksharma.krail.taj.components.TextFieldPlaceholderDefaults
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The origin ring in the From bar's leading column: a multiple of 4, as every size here is.
private val OriginRingSize = 12.dp

/** The field the rider is editing, which takes the dark fill. Null is neither, as on home. */
internal enum class ToFromField { To, From }

/** Whether this field takes the dark fill while [editing] is being edited: only the one edited. */
internal fun ToFromField.hasDarkFill(editing: ToFromField?): Boolean = this == editing

/**
 * Diameter of the go circle: its [column] less [margin] on every side (rule 36, 40 dp in a 48 dp
 * column with 4 dp around it). Pure, so the rule is pinned without a layout.
 */
internal fun goCircleDiameter(column: Dp, margin: Dp): Dp = column - margin * 2

/**
 * What a screen reader says for a field. A field with a stop says which end it is and the stop in
 * full (the text on screen may end in an ellipsis, this never does). An empty field says its own
 * placeholder, which already names it.
 */
internal fun toFromFieldLabel(role: String, placeholder: String, stopName: String?): String =
    if (stopName.isNullOrBlank()) placeholder else "$role, $stopName"

/**
 * The To and From block, rules 35 and 36 of `docs/MAP_HOME_UX.md`.
 *
 * To is the top bar and From sits under it, and neither ever moves: [editing] only changes which
 * of the two takes the dark fill, never a size, a padding or a position. Both bars are the same
 * width and share both edges. Each is one line, 48 dp tall, and ends in an ellipsis.
 *
 * Both bars reserve a 48 dp column at the start and the From bar one at the end:
 * - The start column holds the back arrow on To ([toLeading]) and the origin ring on From, so the
 *   two values start at one x.
 * - The end column holds the clear X on To ([toTrailing]) and the go circle on From, so the two
 *   share one column. Without [toTrailing] To's text runs to the page padding instead.
 *
 * The slots are the caller's. The block only keeps the room for them and gives them the bar's
 * content colour, so the back arrow and the X read on whichever fill the bar has.
 *
 * [toEditor] and [fromEditor] put a text field in the bar being edited, in place of its text, which
 * is how Search Stop edits a stop where it is shown. A bar holding one has no tap target of its
 * own, because the field takes the tap. [fromTrailing] stands in the go circle's column and
 * [showGo] false leaves the column empty, so a From with no start has no go circle.
 *
 * [toPlaceholder] and [fromPlaceholder] are what an empty bar reads.
 *
 * [showFrom] false hides the From bar (the beat after a label pill tap, before it animates in).
 * Because From is under To, showing it never moves To.
 */
@Composable
internal fun ToFromBlock(
    toStopItem: StopItem?,
    fromStopItem: StopItem?,
    onToClick: () -> Unit,
    onFromClick: () -> Unit,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
    editing: ToFromField? = null,
    showFrom: Boolean = true,
    isFromHighlighted: Boolean = false,
    toLeading: @Composable () -> Unit = {},
    toTrailing: (@Composable () -> Unit)? = null,
    toPlaceholder: String = TO_PLACEHOLDER,
    fromPlaceholder: String = FROM_PLACEHOLDER,
    showGo: Boolean = true,
    toEditor: (@Composable () -> Unit)? = null,
    fromEditor: (@Composable () -> Unit)? = null,
    fromTrailing: (@Composable () -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    val fromBarColors = barColors(isEditing = ToFromField.From.hasDarkFill(editing))

    // Pulsing border alpha for the highlighted From bar
    val infiniteTransition = rememberInfiniteTransition(label = "fromHighlight")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fromBorderAlpha",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        ToFromBar(
            tag = TripPlannerTestTags.SEARCH_ROW_TO,
            text = toStopItem?.stopName ?: toPlaceholder,
            hasStop = toStopItem != null,
            label = toFromFieldLabel(TO_ROLE, toPlaceholder, toStopItem?.stopName),
            isEditing = ToFromField.To.hasDarkFill(editing),
            slideUp = false,
            animationLabel = "destinationText",
            onClick = onToClick,
            leading = toLeading,
            trailing = toTrailing,
            editor = toEditor,
        )

        // The gap belongs to the From bar so a hidden From leaves no gap under To, and so the
        // gap animates in with the bar.
        AnimatedVisibility(
            visible = showFrom,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(350)),
        ) {
            ToFromBar(
                tag = TripPlannerTestTags.SEARCH_ROW_FROM,
                text = fromStopItem?.stopName ?: fromPlaceholder,
                hasStop = fromStopItem != null,
                label = toFromFieldLabel(FROM_ROLE, fromPlaceholder, fromStopItem?.stopName),
                isEditing = ToFromField.From.hasDarkFill(editing),
                slideUp = true,
                animationLabel = "startingFromText",
                onClick = onFromClick,
                modifier = Modifier
                    .padding(top = dim.spacingM)
                    .then(
                        if (isFromHighlighted) {
                            Modifier.border(
                                width = dim.strokeRegular,
                                color = KrailTheme.colors.surface.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(50),
                            )
                        } else {
                            Modifier
                        },
                    ),
                leading = { OriginRing() },
                trailing = fromTrailing
                    ?: if (showGo) {
                        { GoCircle(arrowColor = fromBarColors.container, onClick = onGoClick) }
                    } else {
                        null
                    },
                editor = fromEditor,
            )
        }
    }
}

private const val TO_ROLE = "To"
private const val FROM_ROLE = "From"

/** What an empty To bar reads. Shared with Search Stop, whose bar reads the same. */
internal const val TO_PLACEHOLDER = "Choose destination"

/** What an empty From bar reads. */
internal const val FROM_PLACEHOLDER = "Choose starting point"

/** The two surface colours a bar can have: white and near-black in light, swapped in dark (rule 33). */
@Immutable
private class BarColors(val container: Color, val content: Color, val text: TextFieldPlaceholderColors)

@Composable
private fun barColors(isEditing: Boolean): BarColors =
    if (isEditing) {
        val inverted = TextFieldDefaults.invertedColors()
        BarColors(
            container = inverted.containerColor,
            content = inverted.contentColor,
            text = TextFieldPlaceholderColors(
                activeColor = inverted.contentColor,
                inactiveColor = inverted.placeholderColor,
            ),
        )
    } else {
        BarColors(
            container = KrailTheme.colors.surface,
            content = KrailTheme.colors.onSurface,
            text = TextFieldPlaceholderDefaults.colors(),
        )
    }

/**
 * One bar. Its tap target is the whole pill, tagged [tag]. The two end columns are siblings drawn
 * over it rather than children, so the back arrow, the X and the go circle are their own buttons
 * and never a button nested inside another.
 */
@Composable
private fun ToFromBar(
    tag: String,
    text: String,
    hasStop: Boolean,
    label: String,
    isEditing: Boolean,
    slideUp: Boolean,
    animationLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
    editor: (@Composable () -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    val colors = barColors(isEditing)

    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(dim.textFieldHeight)
                .clip(RoundedCornerShape(50))
                .background(color = colors.container),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(tag)
                    // A bar holding a text field is edited by tapping into the field, and a merged
                    // label would stand over the field's own semantics, so it has neither.
                    .then(
                        if (editor == null) {
                            Modifier
                                .klickable(onClick = onClick)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = label
                                    selected = isEditing
                                }
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(dim.iconXXXL + dim.spacingXS))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (editor != null) {
                        editor()
                    } else {
                        StopFieldText(
                            text = text,
                            hasStop = hasStop,
                            colors = colors.text,
                            slideUp = slideUp,
                            label = animationLabel,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(if (trailing != null) dim.iconXXXL else dim.spacingXL))
            }

            Box(
                modifier = Modifier.size(dim.iconXXXL).align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) { leading() }

            if (trailing != null) {
                Box(
                    modifier = Modifier.size(dim.iconXXXL).align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center,
                ) { trailing() }
            }
        }
    }
}

@Composable
private fun StopFieldText(
    text: String,
    hasStop: Boolean,
    colors: TextFieldPlaceholderColors,
    slideUp: Boolean,
    label: String,
) {
    val enterOffset: (Int) -> Int = if (slideUp) ({ it / 2 }) else ({ -it / 2 })
    val exitOffset: (Int) -> Int = if (slideUp) ({ -it / 2 }) else ({ it / 2 })
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = enterOffset,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                ) togetherWith fadeOut(animationSpec = tween(200)) +
                slideOutVertically(targetOffsetY = exitOffset, animationSpec = tween(500))
        },
        contentAlignment = Alignment.CenterStart,
        label = label,
    ) { targetText ->
        ThemeTextFieldPlaceholderText(text = targetText, isActive = hasStop, colors = colors)
    }
}

/** The origin ring that marks the From bar. Drawn in the bar's own content colour. */
@Composable
private fun OriginRing() {
    Box(
        modifier = Modifier
            .size(OriginRingSize)
            .testTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_RING)
            .border(
                width = KrailTheme.dimensions.strokeRegular,
                color = LocalContentColor.current,
                shape = CircleShape,
            ),
    )
}

/**
 * The go circle: one state and a plain arrow. The screen it opens owns loading, success and
 * error, so this one never shows any of them.
 *
 * Its tap target is the 48 dp end column, tagged [TripPlannerTestTags.SEARCH_ROW_SEARCH] as the
 * Search button always was. The visible circle inside it is [goCircleDiameter] across, filled with
 * the bar's content colour and drawn with an arrow in [arrowColor], the bar's own fill, so it is the
 * opposite of the bar on a white bar and on a dark one.
 */
@Composable
private fun GoCircle(arrowColor: Color, onClick: () -> Unit) {
    val dim = KrailTheme.dimensions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TripPlannerTestTags.SEARCH_ROW_SEARCH)
            .clip(CircleShape)
            .klickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Search" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(goCircleDiameter(dim.iconXXXL, dim.spacingXS))
                .testTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)
                .background(color = LocalContentColor.current, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                colorFilter = ColorFilter.tint(arrowColor),
                modifier = Modifier.size(dim.iconDefault),
            )
        }
    }
}
