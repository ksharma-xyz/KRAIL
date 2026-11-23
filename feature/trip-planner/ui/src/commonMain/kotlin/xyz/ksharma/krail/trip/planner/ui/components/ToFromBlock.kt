package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/** What a bar keeps for its two end columns, from the spacing scale. */
@Composable
internal fun toFromBarInsets(): ToFromBarInsets {
    val dim = KrailTheme.dimensions
    // The gap after the start column is taj's text field's own (a 4 dp spacer after its leading
    // control) and the gap before the end column is the 8 dp it keeps before a trailing control.
    return ToFromBarInsets(column = dim.iconXXXL, startGap = dim.spacingXS, endGap = dim.spacingM)
}

/**
 * The To and From block, rules 35 and 36 of `docs/MAP_HOME_UX.md`, built from taj's parts.
 *
 * To is the top bar and From sits under it, and neither ever moves. A bar at rest is taj's
 * [TextFieldButton]. The bar being edited ([editing], holding an [editor]) is taj's [TextField] in
 * its inverted colours, which is the dark bar. Which bar is dark is the only thing [editing]
 * changes: no size, padding or position, so no row moves. Both bars are the same width and share
 * both edges. Each is one line, 48 dp tall, and ends in an ellipsis.
 *
 * Both bars reserve a 48 dp column at the start and the From bar one at the end, which
 * [toFromBarInsets] measures:
 * - The start column holds the back arrow on To ([toLeading]) and the origin dot on From, so the
 *   two values start at one x.
 * - The end column holds the clear X on To ([toTrailing]) and the go circle on From, so the two
 *   share one column. Without [toTrailing] To's text runs to the page padding instead.
 *
 * The slots are the caller's. The block keeps the room for them and gives them the bar's content
 * colour, so the back arrow and the X read on whichever fill the bar has. In a bar at rest they sit
 * over the button as its siblings and in the dark bar they are the text field's own leading and
 * trailing controls, so a control is never inside another control. [fromTrailing] stands in the go
 * circle's column and [showGo] false leaves the column empty, so a From with no start has no go
 * circle.
 *
 * [toPlaceholder] and [fromPlaceholder] are what an empty bar reads.
 *
 * [showFrom] false hides the From bar (the beat after a label pill tap, before it animates in).
 * Because From is under To, showing it never moves To.
 *
 * [style] is the block's look. [ToFromBlockStyle.Classic] is everything above, the home's own. Search Stop
 * passes [ToFromBlockStyle.FloatingRows]: the two bars are soft pills of taj's [TextField], From on top and To below,
 * a gap apart, and neither bar swaps fill when focus moves. In that style [toLeading] stands at the start of the first
 * bar (From, or To when it is alone), [toTrailing] and [fromTrailing] are the end of the bar they name, and the go
 * circle is the end of the last bar (see `ToFromFloatingRows`).
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
    editor: ToFromEditor? = null,
    showFrom: Boolean = true,
    isFromHighlighted: Boolean = false,
    toLeading: @Composable () -> Unit = {},
    toTrailing: (@Composable () -> Unit)? = null,
    toPlaceholder: String = TO_PLACEHOLDER,
    fromPlaceholder: String = FROM_PLACEHOLDER,
    showGo: Boolean = true,
    fromTrailing: (@Composable () -> Unit)? = null,
    // Only the card style reads these: the saved label of each stop (Home, Work) and what the swap does.
    fromLabel: String? = null,
    toLabel: String? = null,
    onSwap: (() -> Unit)? = null,
    style: ToFromBlockStyle = ToFromBlockStyle.Classic,
) {
    val dim = KrailTheme.dimensions
    val insets = toFromBarInsets()
    if (style is ToFromBlockStyle.CardRows) {
        ToFromCardRows(
            rows = CardRowsContent(
                from = CardRowContent(
                    bar = BarContent(
                        tag = TripPlannerTestTags.SEARCH_ROW_FROM,
                        text = fromStopItem?.stopName ?: fromPlaceholder,
                        hasStop = fromStopItem != null,
                        label = toFromFieldLabel(FROM_ROLE, fromPlaceholder, labelledStopName(fromLabel, fromStopItem)),
                        placeholder = fromPlaceholder,
                        slideUp = true,
                        animationLabel = "startingFromText",
                        onClick = onFromClick,
                    ),
                    stopName = fromStopItem?.stopName,
                    stopLabel = fromLabel,
                ),
                to = CardRowContent(
                    bar = BarContent(
                        tag = TripPlannerTestTags.SEARCH_ROW_TO,
                        text = toStopItem?.stopName ?: toPlaceholder,
                        hasStop = toStopItem != null,
                        label = toFromFieldLabel(TO_ROLE, toPlaceholder, labelledStopName(toLabel, toStopItem)),
                        placeholder = toPlaceholder,
                        slideUp = false,
                        animationLabel = "destinationText",
                        onClick = onToClick,
                    ),
                    stopName = toStopItem?.stopName,
                    stopLabel = toLabel,
                ),
                editing = editing,
                editor = editor,
                showGo = showGo,
                onGoClick = onGoClick,
                onSwap = onSwap,
            ),
            modifier = modifier,
        )
        return
    }
    if (style is ToFromBlockStyle.FloatingRows) {
        ToFromFloatingRows(
            rows = FloatingRowsContent(
                to = BarContent(
                    tag = TripPlannerTestTags.SEARCH_ROW_TO,
                    text = toStopItem?.stopName ?: toPlaceholder,
                    hasStop = toStopItem != null,
                    label = toFromFieldLabel(TO_ROLE, toPlaceholder, toStopItem?.stopName),
                    placeholder = toPlaceholder,
                    slideUp = false,
                    animationLabel = "destinationText",
                    onClick = onToClick,
                ),
                from = BarContent(
                    tag = TripPlannerTestTags.SEARCH_ROW_FROM,
                    text = fromStopItem?.stopName ?: fromPlaceholder,
                    hasStop = fromStopItem != null,
                    label = toFromFieldLabel(FROM_ROLE, fromPlaceholder, fromStopItem?.stopName),
                    placeholder = fromPlaceholder,
                    slideUp = true,
                    animationLabel = "startingFromText",
                    onClick = onFromClick,
                ),
                editing = editing,
                editor = editor,
                showFrom = showFrom,
                firstLeading = toLeading,
                toTrailing = toTrailing,
                fromTrailing = fromTrailing,
                showGo = showGo,
                onGoClick = onGoClick,
            ),
            insets = insets,
            rowGap = style.rowGap,
            boxLook = style.boxLook,
            modifier = modifier,
        )
        return
    }
    val fromInk = barInk(dark = ToFromField.From.hasDarkFill(editing) && editor != null)

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
            bar = BarContent(
                tag = TripPlannerTestTags.SEARCH_ROW_TO,
                text = toStopItem?.stopName ?: toPlaceholder,
                hasStop = toStopItem != null,
                label = toFromFieldLabel(TO_ROLE, toPlaceholder, toStopItem?.stopName),
                placeholder = toPlaceholder,
                slideUp = false,
                animationLabel = "destinationText",
                onClick = onToClick,
            ),
            isEditing = ToFromField.To.hasDarkFill(editing),
            editor = editor,
            insets = insets,
            leading = toLeading,
            trailing = toTrailing,
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
                bar = BarContent(
                    tag = TripPlannerTestTags.SEARCH_ROW_FROM,
                    text = fromStopItem?.stopName ?: fromPlaceholder,
                    hasStop = fromStopItem != null,
                    label = toFromFieldLabel(FROM_ROLE, fromPlaceholder, fromStopItem?.stopName),
                    placeholder = fromPlaceholder,
                    slideUp = true,
                    animationLabel = "startingFromText",
                    onClick = onFromClick,
                ),
                isEditing = ToFromField.From.hasDarkFill(editing),
                editor = editor,
                insets = insets,
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
                leading = { OriginDot() },
                trailing = fromTrailing
                    ?: if (showGo) {
                        {
                            GoCircle(
                                circle = fromInk.content,
                                arrow = fromInk.container,
                                diameter = goCircleDiameter(insets.column, dim.spacingXS),
                                onClick = onGoClick,
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

/** What a screen reader says for a stop: its name, with its saved label first when it has one (Home, Central Station). */
private fun labelledStopName(label: String?, stop: StopItem?): String? =
    stop?.stopName?.let { name -> if (label.isNullOrBlank()) name else "$label, $name" }

private const val TO_ROLE = "To"
private const val FROM_ROLE = "From"

/** What an empty To bar reads. Shared with Search Stop, whose bar reads the same. */
internal const val TO_PLACEHOLDER = "Choose destination"

/** What an empty From bar reads. */
internal const val FROM_PLACEHOLDER = "Choose starting point"

/** What a bar says: its test tag, its text and how it animates, its accessible label and what a tap does. */
@Immutable
internal data class BarContent(
    val tag: String,
    val text: String,
    val hasStop: Boolean,
    val label: String,
    val placeholder: String,
    val slideUp: Boolean,
    val animationLabel: String,
    val onClick: () -> Unit,
)

/** A bar's two colours: the fill and what is drawn on it. White and near-black in light, swapped in dark. */
@Immutable
private class BarInk(val container: Color, val content: Color)

@Composable
private fun barInk(dark: Boolean): BarInk =
    if (dark) {
        val inverted = TextFieldDefaults.invertedColors()
        BarInk(container = inverted.containerColor, content = inverted.contentColor)
    } else {
        BarInk(container = KrailTheme.colors.surface, content = KrailTheme.colors.onSurface)
    }

/**
 * One bar, tagged [BarContent.tag]: the dark text field when it is the one being edited and an
 * [editor] is there to type in, otherwise the light button.
 */
@Composable
private fun ToFromBar(
    bar: BarContent,
    isEditing: Boolean,
    editor: ToFromEditor?,
    insets: ToFromBarInsets,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val dark = isEditing && editor != null
    CompositionLocalProvider(LocalContentColor provides barInk(dark).content) {
        Box(modifier = modifier.fillMaxWidth()) {
            if (editor != null && dark) {
                EditingBar(bar, editor, insets, TextFieldDefaults.invertedColors(), leading, trailing)
            } else {
                IdleBar(bar, selected = isEditing, insets, leading, trailing)
            }
        }
    }
}

/**
 * A bar at rest: taj's [TextFieldButton]. Its tap target is the whole pill. The two end columns are
 * siblings drawn over it, so the back arrow, the X and the go circle are their own buttons and never
 * a button nested inside another. The button pads its text by the spacing scale's 16 dp, so the text
 * adds what is left of the inset.
 */
@Composable
private fun IdleBar(
    bar: BarContent,
    selected: Boolean,
    insets: ToFromBarInsets,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val buttonPadding = KrailTheme.dimensions.spacingXL
    Box(modifier = Modifier.fillMaxWidth()) {
        TextFieldButton(
            onClick = bar.onClick,
            modifier = Modifier
                .testTag(bar.tag)
                .semantics(mergeDescendants = true) {
                    contentDescription = bar.label
                    this.selected = selected
                },
        ) {
            // The button leaves 4 dp above and below the text, which is room for 100 percent text
            // only. Measuring the text without that limit keeps one line whole in the 48 dp bar at
            // larger sizes, and the bar's own clip is the edge.
            Box(
                modifier = Modifier
                    .padding(
                        start = insets.textStart - buttonPadding,
                        end = if (trailing != null) insets.textEndReserved - buttonPadding else 0.dp,
                    )
                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
            ) {
                StopFieldText(
                    text = bar.text,
                    hasStop = bar.hasStop,
                    slideUp = bar.slideUp,
                    label = bar.animationLabel,
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) { BarSlot(insets.column, leading) }
        if (trailing != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { BarSlot(insets.column, trailing) }
        }
    }
}

/**
 * The bar being edited: taj's [TextField] in [colors] (the inverted colours on home, the floating ones on
 * Search Stop), with the slots as its leading and trailing controls. Its own insets are zero so the controls take the whole 48 dp, and it
 * keeps 16 dp at the end when there is no trailing control, as the button does. A field takes the
 * tap itself, so the bar has no tap target of its own and says it is selected.
 */
@Composable
internal fun EditingBar(
    bar: BarContent,
    editor: ToFromEditor,
    insets: ToFromBarInsets,
    colors: TextFieldColors,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(bar.tag)
            .semantics { selected = true },
    ) {
        TextField(
            state = editor.state,
            placeholder = bar.placeholder,
            modifier = Modifier.fillMaxWidth().then(editor.modifier),
            maxLength = editor.maxLength,
            filter = editor.filter,
            colors = colors,
            leadingIcon = { BarSlot(insets.column, leading) },
            trailingIcon = trailing?.let { slot -> { BarSlot(insets.column, slot) } },
            contentPadding = PaddingValues(end = if (trailing == null) KrailTheme.dimensions.spacingXL else 0.dp),
        ) { value -> editor.onTextChange(value) }
    }
}
