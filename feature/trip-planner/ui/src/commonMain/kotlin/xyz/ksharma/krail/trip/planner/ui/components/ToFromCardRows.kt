package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

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
)

/**
 * The To and From block as the Timetable's own card (`docs/MAP_HOME_UX.md`, The box): two plain rows, From above To, on the
 * card the caller draws in the soft theme tint, with the 1 dp divider between them and no fill of their own. The rows are the
 * editable fields: taj's [TextField] with no container, the page's text colour for text, prompt and caret, so the card's tint
 * is all that shows.
 *
 * A row is, from the start, its marker (the 10 dp dot for From, the pin for To, or the stop's own label icon when it has
 * one), then its text. A stop with a saved label reads the way the Timetable reads it, the label in bold and the stop in
 * brackets after it. The marker stands beside the text while the text fits beside it on one line, and wraps whole above it when
 * it does not (a narrow phone, large text), where the text takes the full width and wraps onto lines of its own. Nothing is
 * truncated or squeezed, so the card grows instead.
 *
 * On the right, a column reserved in every state ([CardGrid]) holding the go circle at the end of the To row, centred on the
 * row, so the card is the same height with and without it. While the row being edited holds text, an X takes its row's slot
 * and clears it.
 */
@Composable
internal fun ToFromCardRows(rows: CardRowsContent, modifier: Modifier = Modifier) {
    val grid = rememberCardGrid()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fromBeside = markerBeside(rememberNaturalWidth(rows.from, editing = rows.editing == ToFromField.From), maxWidth, grid)
        val toBeside = markerBeside(rememberNaturalWidth(rows.to, editing = rows.editing == ToFromField.To), maxWidth, grid)
        Column(modifier = Modifier.fillMaxWidth()) {
            CardStopRow(
                row = rows.from,
                isOrigin = true,
                isEditing = rows.editing == ToFromField.From,
                editor = rows.editor,
                beside = fromBeside,
                grid = grid,
            ) { editorState ->
                if (editorState != null) ClearCircle(editorState)
            }
            Box(
                modifier = Modifier.padding(
                    start = dividerStart(grid, anyStacked = !fromBeside || !toBeside),
                    end = grid.endReserved,
                ),
            ) {
                Divider(modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_DIVIDER))
            }
            CardStopRow(
                row = rows.to,
                isOrigin = false,
                isEditing = rows.editing == ToFromField.To,
                editor = rows.editor,
                beside = toBeside,
                grid = grid,
            ) { editorState ->
                if (editorState != null) ClearCircle(editorState)
                GoCircle(visible = rows.showGo && editorState == null, onClick = rows.onGoClick)
            }
        }
    }
}

/**
 * One row. [circle] is its end slot, given the row's editor state while that row is being edited and holds text (the X clears
 * it), and null otherwise. The slot is a 48 dp target at the row's end, centred on the row, so the circle's right edge is
 * [CardGrid.endPad] from the card's edge and its centre is on the row's centre line.
 */
@Composable
private fun CardStopRow(
    row: CardRowContent,
    isOrigin: Boolean,
    isEditing: Boolean,
    editor: ToFromEditor?,
    beside: Boolean,
    grid: CardGrid,
    circle: @Composable (TextFieldState?) -> Unit,
) {
    val bar = row.bar
    val editingHere = isEditing && editor != null
    val density = LocalDensity.current
    val padV = rowTextPadding(grid, with(density) { KrailTheme.typography.titleLarge.lineHeight.toDp() })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(bar.tag)
            .semantics { selected = editingHere },
    ) {
        val text: @Composable (PaddingValues) -> Unit = { padding ->
            if (editingHere) {
                EditingRowField(bar = bar, editor = checkNotNull(editor), padding = padding)
            } else {
                IdleRowText(row = row, padding = padding)
            }
        }
        val marker: @Composable () -> Unit = {
            RowMarker(isOrigin = isOrigin, label = row.stopLabel.takeIf { row.stopName != null }, grid = grid)
        }
        if (beside) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = grid.rowMin)
                    .padding(start = grid.startPad, end = grid.endReserved),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(grid.markerGap),
            ) {
                marker()
                Box(modifier = Modifier.weight(1f)) { text(PaddingValues(vertical = padV)) }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = grid.rowMin)
                    .padding(start = grid.startPad, end = grid.endReserved, top = grid.rowVerticalPad),
            ) {
                marker()
                Spacer(modifier = Modifier.height(grid.stackedGap))
                text(PaddingValues(bottom = grid.rowVerticalPad))
            }
        }
        if (!editingHere) {
            // The whole row up to the circles is the tap that edits it, so nothing under it is the map's.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = grid.endReserved)
                    .klickable(onClick = bar.onClick)
                    .semantics { contentDescription = bar.label },
            )
        }
        val hasText = editingHere && checkNotNull(editor).state.text.isNotEmpty()
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = grid.endPad - (TARGET - grid.circle) / 2f),
        ) {
            circle(if (hasText) editor?.state else null)
        }
    }
}

/** The query field of the row being edited: empty with its fixed prompt, in the page's text colour and no fill. */
@Composable
private fun EditingRowField(bar: BarContent, editor: ToFromEditor, padding: PaddingValues) {
    TextField(
        state = editor.state,
        placeholder = bar.placeholder,
        modifier = Modifier.fillMaxWidth().then(editor.modifier),
        maxLength = editor.maxLength,
        filter = editor.filter,
        colors = cardFieldColors(),
        lineLimits = TextFieldLineLimits.MultiLine(),
        minHeight = 0.dp,
        contentPadding = padding,
    ) { value -> editor.onTextChange(value) }
}

/**
 * A row at rest. A stop with a saved label reads as the Timetable does, the label in bold and the stop in brackets, as whole
 * items that wrap. Any other row is the same taj field read only: the stop as its text, in bold, or the prompt.
 */
@Composable
private fun IdleRowText(row: CardRowContent, padding: PaddingValues) {
    val bar = row.bar
    val name = row.stopName
    val label = row.stopLabel
    if (name != null && label != null) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(padding),
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
            textStyle = if (bar.hasStop) KrailTheme.typography.titleMedium.copy(color = KrailTheme.colors.onSurface) else null,
            lineLimits = TextFieldLineLimits.MultiLine(),
            minHeight = 0.dp,
            contentPadding = padding,
        )
    }
}

/** No fill of its own, so the card's tint shows, and the page's text colour for the text, the prompt and the caret. */
@Composable
private fun cardFieldColors(): TextFieldColors {
    val ink = KrailTheme.colors.onSurface
    return TextFieldColors(containerColor = Color.Transparent, contentColor = ink, placeholderColor = ink, cursorColor = ink)
}

/** The marker in its 20 dp box: the stop's label icon, else the 10 dp dot for From or the pin for To, in the page's text colour. */
@Composable
private fun RowMarker(isOrigin: Boolean, label: String?, grid: CardGrid) {
    val ink = KrailTheme.colors.onSurface
    val labelIcon = label?.let { stopLabelIcon(it) }
    Box(modifier = Modifier.size(grid.marker), contentAlignment = Alignment.Center) {
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

/**
 * How wide a row's text is on one line, which decides whether its marker can stand beside it. The row being edited is
 * measured by its prompt, whatever is typed, so the layout does not change as the rider types.
 */
@Composable
private fun rememberNaturalWidth(row: CardRowContent, editing: Boolean): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = KrailTheme.typography
    return remember(row.stopName, row.stopLabel, row.bar.placeholder, editing, density, typography) {
        val prompt = typography.titleLarge.copy(fontWeight = FontWeight.Normal)
        val name = row.stopName
        val label = row.stopLabel
        fun width(text: String, style: TextStyle): Int =
            measurer.measure(text = text, style = style, softWrap = false, maxLines = 1).size.width
        val px = when {
            editing || name == null -> width(row.bar.placeholder, prompt)
            label != null -> maxOf(width(label, typography.titleMedium), width("($name)", typography.bodySmall))
            else -> width(name, typography.titleMedium)
        }
        with(density) { px.toDp() }
    }
}

/**
 * The go circle: the theme colour with the app's arrow in the colour taj gives that fill. It is there only while both stops are
 * chosen (absent, not disabled). The slot is reserved, so the card is the same height with it and without it.
 */
@Composable
private fun GoCircle(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    val fill = mapActiveChipColors().containerColor
    CircleTargetButton(
        contentDescription = "Show trips",
        onClick = onClick,
        circle = fill,
        tag = TripPlannerTestTags.SEARCH_ROW_SEARCH,
        circleTag = TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE,
    ) {
        Image(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            colorFilter = ColorFilter.tint(getForegroundColor(fill)),
            modifier = Modifier.size(KrailTheme.dimensions.iconM),
        )
    }
}

/** The X in a row's circle slot while its field holds text: it clears the field and does nothing else. */
@Composable
private fun ClearCircle(state: TextFieldState) {
    val fill = KrailTheme.colors.softLabel
    CloseCircleButton(
        contentDescription = "Clear",
        onClick = { state.clearText() },
        circle = fill,
        glyph = getForegroundColor(fill),
        glyphSize = KrailTheme.dimensions.iconL,
        tag = TripPlannerTestTags.SEARCH_STOP_CLEAR,
        circleTag = TripPlannerTestTags.SEARCH_STOP_CLEAR_CIRCLE,
    )
}
