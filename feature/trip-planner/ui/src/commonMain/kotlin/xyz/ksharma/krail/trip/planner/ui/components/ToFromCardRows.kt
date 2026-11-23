package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.math.roundToInt

/** What one row of the card says: the bar (its tag, prompt and tap) and the saved label of the stop it holds. */
@Immutable
internal class CardRowContent(
    val bar: BarContent,
    /** The stop's own name, or null while the row holds none. */
    val stopName: String?,
    /** The saved label of that stop (Home, Work), or null. It decides the row's text and its icon. */
    val stopLabel: String?,
)

/** What the card's two rows say and do, in one holder so the block hands it over as one argument. */
@Immutable
internal class CardRowsContent(
    val from: CardRowContent,
    val to: CardRowContent,
    val editing: ToFromField?,
    val editor: ToFromEditor?,
    val showGo: Boolean,
    val onGoClick: () -> Unit,
    /** Reverses From and To. Null draws no swap: no stop has been picked yet, so there is nothing to reverse. */
    val onSwap: (() -> Unit)?,
)

/**
 * The To and From block as the Timetable's own card (`docs/MAP_HOME_UX.md`, The box): two plain rows, From above To, on the
 * card the caller draws in the fixed light tint, with the 1 dp divider between them and no fill of their own. The rows are the
 * editable fields: taj's [TextField] with no container, the card's text colour for text, prompt and caret, so the card's tint is
 * all that shows.
 *
 * A row is, from the start, its marker (the 10 dp dot for From, the pin for To, or the stop's own label icon when it has one) in
 * a fixed 20 dp column at x16, then its text column at x44 at every size and width. The marker stands beside the first line of
 * the name, centred on that line, and a long name wraps by whole words onto lines that start at x44 under the first word (a
 * hanging indent), never under the marker and never above it, so a line that holds only a marker cannot exist. A stop with a
 * saved label reads the way the Timetable reads it, the label in bold and the stop in brackets after it. A row is 56 dp or its
 * lines plus 24 dp, whichever is taller, and the card grows with it. Nothing is truncated or squeezed.
 *
 * On the right, one column of controls on one axis ([CardGrid.axis], 36 dp from the card's right edge): the bare swap glyph and,
 * only when both stops are chosen, the go circle in the To row. The swap rests at the divider level while at most one stop is
 * chosen and on the From row's centre line once both are, and it glides between the two in [MapMotion.SWAP_GLIDE_MS] as the go
 * circle arrives or leaves (Design Y). It is above the go circle in z order, so it wins any overlap. While the row being edited
 * holds text, a bare X takes that row's end slot and clears it, and the swap steps aside.
 *
 * The room the column takes follows what the card draws ([cardColumnInUse]). With no stop picked nothing stands in the column, so
 * the text and the divider run to the card's right padding, [CardGrid.endPad] 16, the same as at the start, and a name never comes
 * closer to the edge than that. Once a stop is picked the swap (and with both stops the go circle) belongs to the card, and the
 * text and the divider stop 8 dp before the column. The room changes with the pick, in the frame the rows change, and never with
 * the swap's own fade or glide, so the card does not reflow while it animates or while the rider types.
 */
@Composable
internal fun ToFromCardRows(rows: CardRowsContent, modifier: Modifier = Modifier) {
    val grid = rememberCardGrid()
    val editor = rows.editor
    val hasText = editor != null && editor.state.text.isNotEmpty()
    val fromClear = hasText && rows.editing == ToFromField.From
    val toClear = hasText && rows.editing == ToFromField.To
    val swapShown = rows.onSwap != null && !fromClear && !toClear
    val swap = rememberSwapMotion(shown = swapShown, lifted = rows.showGo)
    // The room the right column takes follows what the card draws: the text and the divider run to the card's right padding while
    // nothing is drawn there, and stop short of the column once the swap or the go circle belongs to the card. A row that holds
    // the X keeps the room for it while it is typed in.
    val columnInUse = cardColumnInUse(hasSwap = rows.onSwap != null, showGo = rows.showGo)
    CardLayout(grid = grid, swap = swap, modifier = modifier) {
        CardStopRow(
            row = rows.from,
            isOrigin = true,
            isEditing = rows.editing == ToFromField.From,
            editor = editor,
            grid = grid,
            endRoom = grid.endRoom(columnInUse || fromClear),
            modifier = Modifier.layoutId(CardPart.From),
        ) { editorState ->
            if (editorState != null) ClearCross(editorState)
        }
        Box(
            modifier = Modifier
                .layoutId(CardPart.Divider)
                .padding(start = grid.textStart, end = grid.endRoom(columnInUse)),
        ) {
            Divider(
                modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_DIVIDER),
                color = mapCardColors().divider,
            )
        }
        CardStopRow(
            row = rows.to,
            isOrigin = false,
            isEditing = rows.editing == ToFromField.To,
            editor = editor,
            grid = grid,
            endRoom = grid.endRoom(columnInUse || toClear),
            modifier = Modifier.layoutId(CardPart.To),
        ) { editorState ->
            if (editorState != null) ClearCross(editorState)
            GoCircleSlot(shown = rows.showGo && editorState == null, onClick = rows.onGoClick)
        }
        if (swap.present) {
            SwapControl(
                onSwap = { rows.onSwap?.invoke() },
                shown = swapShown,
                motion = swap,
                modifier = Modifier.layoutId(CardPart.Swap),
            )
        }
    }
}

/** The four parts of the card, by the id each is laid out under. */
private enum class CardPart { From, Divider, To, Swap }

/**
 * Lays the card out: the From row, the divider and the To row stacked at the card's width, so the card is as tall as they are
 * and nothing else, and the swap's 48 dp box on top of them (placed last, so it is drawn and hit first). The swap is placed at the
 * divider level, centred on the card's controls axis, and [swap] moves it up toward the From row's centre line in a graphics
 * layer, so the move is a draw phase animation that moves its touch target with it and never relays out the rows.
 */
@Composable
private fun CardLayout(
    grid: CardGrid,
    swap: SwapMotion,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val width = constraints.maxWidth
        val rowConstraints = constraints.copy(minWidth = width, maxWidth = width, minHeight = 0)
        fun part(id: CardPart) = measurables.first { it.layoutId == id }
        val from = part(CardPart.From).measure(rowConstraints)
        val divider = part(CardPart.Divider).measure(rowConstraints)
        val to = part(CardPart.To).measure(rowConstraints)
        val targetPx = grid.target.roundToPx()
        val swapPlaceable = measurables.firstOrNull { it.layoutId == CardPart.Swap }?.measure(Constraints.fixed(targetPx, targetPx))
        layout(width, from.height + divider.height + to.height) {
            from.placeRelative(0, 0)
            divider.placeRelative(0, from.height)
            to.placeRelative(0, from.height + divider.height)
            swapPlaceable?.let { placeable ->
                val restCentre = from.height + divider.height / 2f
                val glide = from.height / 2f + divider.height / 2f
                placeable.placeRelativeWithLayer(
                    x = width - grid.axis.roundToPx() - targetPx / 2,
                    y = (restCentre - targetPx / 2f).roundToInt(),
                ) {
                    translationY = -glide * swap.lift.value
                    alpha = swap.fade.value
                }
            }
        }
    }
}

/**
 * One row. [endRoom] is how far its text and its tap stop from the card's right edge ([CardGrid.endRoom]). [end] is its end slot,
 * given the row's editor state while that row is being edited and holds text (the X clears it), and null otherwise. The slot is a
 * 48 dp target at the row's end, centred on the row, so a circle's right edge is [CardGrid.endPad] from the card's edge and its
 * centre is on the row's centre line.
 *
 * The row is [CardGrid.rowMin] tall or its text plus [CardGrid.rowChrome], and the text block is centred in it. The text block is
 * a marker column [CardGrid.marker] wide and one line tall beside the text column, so the marker is centred on the first line.
 */
@Composable
private fun CardStopRow(
    row: CardRowContent,
    isOrigin: Boolean,
    isEditing: Boolean,
    editor: ToFromEditor?,
    grid: CardGrid,
    endRoom: Dp,
    modifier: Modifier = Modifier,
    end: @Composable (TextFieldState?) -> Unit,
) {
    val bar = row.bar
    val editingHere = isEditing && editor != null
    val firstLine = rememberFirstLineHeight(isPrompt = editingHere || row.stopName == null)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = grid.rowMin)
            .testTag(bar.tag)
            .semantics { selected = editingHere },
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = grid.startPad, end = endRoom, top = grid.rowVerticalPad, bottom = grid.rowVerticalPad),
            horizontalArrangement = Arrangement.spacedBy(grid.markerGap),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.width(grid.marker).height(firstLine), contentAlignment = Alignment.Center) {
                RowMarker(isOrigin = isOrigin, label = row.stopLabel.takeIf { row.stopName != null }, grid = grid)
            }
            Box(modifier = Modifier.weight(1f)) {
                if (editingHere) {
                    EditingRowField(bar = bar, editor = checkNotNull(editor))
                } else {
                    IdleRowText(row = row)
                }
            }
        }
        if (!editingHere) {
            // The whole row up to the controls is the tap that edits it, so nothing under it is the map's.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = endRoom)
                    .klickable(onClick = bar.onClick)
                    .semantics { contentDescription = bar.label },
            )
        }
        val hasText = editingHere && checkNotNull(editor).state.text.isNotEmpty()
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = grid.endPad - (grid.target - grid.circle) / 2f),
        ) {
            end(if (hasText) editor?.state else null)
        }
    }
}

/**
 * The height of the first line of a row's text, measured with the style the row is drawn in and not assumed from a number, so
 * the marker is centred on the line the text engine really lays out at this density and font size: the prompt and the typed text
 * are taj's field style (`titleLarge` at normal weight), a stop and a label are `titleMedium`. A row is drawn in one style at a
 * time, and the line it measures is the first one however many follow.
 */
@Composable
private fun rememberFirstLineHeight(isPrompt: Boolean): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = KrailTheme.typography
    return remember(isPrompt, density, typography) {
        val style = if (isPrompt) typography.titleLarge.copy(fontWeight = FontWeight.Normal) else typography.titleMedium
        with(density) { measurer.measure(text = "Ag", style = style, softWrap = false, maxLines = 1).size.height.toDp() }
    }
}

/** The query field of the row being edited: empty with its fixed prompt, in the page's text colour and no fill. */
@Composable
private fun EditingRowField(bar: BarContent, editor: ToFromEditor) {
    TextField(
        state = editor.state,
        placeholder = bar.placeholder,
        modifier = Modifier.fillMaxWidth().then(editor.modifier),
        maxLength = editor.maxLength,
        filter = editor.filter,
        colors = cardFieldColors(),
        lineLimits = TextFieldLineLimits.MultiLine(),
        minHeight = 0.dp,
        contentPadding = PaddingValues(),
    ) { value -> editor.onTextChange(value) }
}

/**
 * A row at rest. A stop with a saved label reads as the Timetable does, the label in bold and the stop in brackets, as whole
 * items that wrap. Any other row is the same taj field read only: the stop as its text, in bold, or the prompt.
 */
@Composable
private fun IdleRowText(row: CardRowContent) {
    val bar = row.bar
    val name = row.stopName
    val label = row.stopLabel
    if (name != null && label != null) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS),
            verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS),
        ) {
            Text(text = label, style = KrailTheme.typography.titleMedium)
            Text(text = "($name)", style = KrailTheme.typography.bodySmall)
        }
    } else {
        val state = remember(bar.text, bar.hasStop) { TextFieldState(if (bar.hasStop) bar.text else "") }
        TextField(
            state = state,
            placeholder = bar.placeholder,
            readOnly = true,
            modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
            colors = cardFieldColors(),
            textStyle = if (bar.hasStop) KrailTheme.typography.titleMedium.copy(color = mapCardColors().onTint) else null,
            lineLimits = TextFieldLineLimits.MultiLine(),
            minHeight = 0.dp,
            contentPadding = PaddingValues(),
        )
    }
}

/** No fill of its own, so the card's tint shows, and the page's text colour for the text, the prompt and the caret. */
@Composable
private fun cardFieldColors(): TextFieldColors {
    val ink = mapCardColors().onTint
    return TextFieldColors(containerColor = Color.Transparent, contentColor = ink, placeholderColor = ink, cursorColor = ink)
}

/** The marker in its 20 dp box: the stop's label icon, else the 10 dp dot for From or the pin for To, in the page's text colour. */
@Composable
private fun RowMarker(isOrigin: Boolean, label: String?, grid: CardGrid) {
    val ink = mapCardColors().onTint
    val labelIcon = label?.let { stopLabelIcon(it) }
    Box(modifier = Modifier.requiredSize(grid.marker), contentAlignment = Alignment.Center) {
        when {
            labelIcon != null -> Image(
                painter = painterResource(labelIcon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ink),
                modifier = Modifier.fillMaxSize().testTag(TripPlannerTestTags.SEARCH_ROW_LABEL_ICON),
            )

            isOrigin -> Box(
                modifier = Modifier
                    .size(ORIGIN_DOT_SIZE)
                    .background(ink, CircleShape)
                    .testTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT),
            )

            else -> Image(
                painter = painterResource(Res.drawable.ic_location_on),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ink),
                modifier = Modifier.fillMaxSize().testTag(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN),
            )
        }
    }
}

/** The Timetable's own From dot. */
private val ORIGIN_DOT_SIZE = 10.dp

