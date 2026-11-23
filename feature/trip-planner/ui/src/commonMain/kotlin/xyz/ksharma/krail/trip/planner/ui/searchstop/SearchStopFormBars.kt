package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.CloseCircleButton
import xyz.ksharma.krail.trip.planner.ui.components.FloatingGoCircle
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlock
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlockStyle
import xyz.ksharma.krail.trip.planner.ui.components.ToFromEditor
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import xyz.ksharma.krail.trip.planner.ui.components.toFromBarInsets
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The form at the top of Search Stop: the app's To and From block with the search field typed into
 * the bar being edited, in place (rules 35 and 36 of `docs/MAP_HOME_UX.md`).
 *
 * - With [ToFromBlockStyle.FloatingRows], which Search Stop uses, both bars are the same taj text field as a
 *   soft pill on the group card's one surface, the same colour whichever has focus. The bar being edited holds
 *   the query field, the other is that field unfocused, and tapping it moves the field and the keyboard to
 *   it. With [ToFromBlockStyle.Classic] the bar being edited is the dark one and the other the light
 *   button. No row moves either way.
 * - From is the top bar and To is the bar below it. The back arrow stays at the start of the first (top) bar in
 *   every state, which is From, or the one bar of a single field. To has the destination pin at its start.
 * - The X shows only while the field has text, and only clears it. It never leaves the screen, so
 *   the recents stay where they are.
 * - With [ToFromBlockStyle.CardRows], the From and To card of the Timetable's style, there is no back arrow and the go circle
 *   and the swap are the card's own right column (see `ToFromCardRows`); [fromLabel] and [toLabel] are the saved labels of the
 *   stops the rows hold, and [onSwap] is what the swap circle does (null draws none).
 * - The go circle is drawn on the To bar, the last one, as soon as [goVisible] says the trip can start, and it is the
 *   only way out of the screen with a trip.
 * - Each bar shows its stop as its text while it is at rest. From shows [fromStop], which is the stop the rider
 *   picked or else the one nearest them, and it stays editable. A bar that is edited starts empty with its fixed
 *   prompt as the placeholder, never the stop's name.
 * - [editing] null means both bars hold a stop and neither is being edited, so neither bar holds the field.
 */
@Composable
internal fun SearchStopForm(
    shape: FormShape,
    editing: ToFromField?,
    toPlaceholder: String,
    fromPlaceholder: String,
    toStop: StopItem?,
    fromStop: StopItem?,
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    onFlowEvent: (SearchStopFlowEvent) -> Unit,
    onBack: () -> Unit,
    onGoClick: () -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fromLabel: String? = null,
    toLabel: String? = null,
    onSwap: (() -> Unit)? = null,
    style: ToFromBlockStyle = ToFromBlockStyle.Classic,
) {
    val hasText by remember(fieldState) { derivedStateOf { fieldState.text.isNotEmpty() } }
    val editor = remember(fieldState, focusRequester, onFlowEvent, onTextChange) {
        searchStopEditor(
            fieldState = fieldState,
            focusRequester = focusRequester,
            onFocused = { onFlowEvent(SearchStopFlowEvent.BarTapped) },
            onTextChange = onTextChange,
        )
    }
    val showGo = goVisible(from = fromStop, to = toStop)
    val insets = toFromBarInsets()
    // The X stays in the column even while it has nothing to draw, so the field never shifts a
    // step to the right the moment the rider types.
    val clear: @Composable () -> Unit = {
        if (hasText) ClearTextButton(onClick = { fieldState.clearText() })
    }
    // The To bar is the last one and ends in the go circle, so while it is edited and nothing is typed the go circle
    // keeps its place, and the X takes it only while there is text to clear.
    val clearOrGo: @Composable () -> Unit = {
        if (hasText) {
            ClearTextButton(onClick = { fieldState.clearText() })
        } else if (showGo) {
            FloatingGoCircle(insets = insets, onClick = onGoClick)
        }
    }

    ToFromBlock(
        toStopItem = toStop,
        fromStopItem = fromStop,
        onToClick = { switchEditing(fieldState, editing, ToFromField.To, onFlowEvent) },
        onFromClick = { switchEditing(fieldState, editing, ToFromField.From, onFlowEvent) },
        onGoClick = onGoClick,
        modifier = modifier,
        editing = editing,
        editor = editor,
        showFrom = shape == FormShape.ToAndFrom,
        toLeading = { BackArrowButton(onClick = onBack) },
        toTrailing = if (editing == ToFromField.To) clearOrGo else null,
        toPlaceholder = toPlaceholder,
        fromPlaceholder = fromPlaceholder,
        showGo = showGo,
        fromTrailing = if (editing == ToFromField.From) clear else null,
        fromLabel = fromLabel,
        toLabel = toLabel,
        onSwap = onSwap,
        style = style,
    )
}

/** Moves editing to [target], clearing the text that was a search for the other field. */
private fun switchEditing(
    fieldState: TextFieldState,
    current: ToFromField?,
    target: ToFromField,
    onFlowEvent: (SearchStopFlowEvent) -> Unit,
) {
    if (current != target) fieldState.clearText()
    onFlowEvent(SearchStopFlowEvent.FieldTapped(target))
}

/**
 * The text field the dark bar holds: the one query field tag every flow and test reaches the field
 * by, and focus. Gaining focus is the tap that raises the list state.
 */
private fun searchStopEditor(
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onTextChange: (String) -> Unit,
): ToFromEditor = ToFromEditor(
    state = fieldState,
    modifier = Modifier
        .focusRequester(focusRequester)
        .onFocusChanged { if (it.isFocused) onFocused() }
        .testTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD),
    // The redaction cap, not a second number. A query longer than this is dropped from
    // analytics entirely, so a field that accepted more would collect text the rider could
    // never see reported, and one that accepts less makes that branch unreachable.
    maxLength = SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH,
    filter = ::stopSearchInputFilter,
    onTextChange = { value -> onTextChange(value.toString()) },
)

/**
 * The back arrow in the To bar's start column: taj's [RoundIconButton] with no fill, so it is the app's
 * own round button at 48, drawn in the bar's colour and not the button's own, which reads the wrong
 * way round on the dark bar. The tag is the one the arrow always had.
 */
@Composable
private fun BackArrowButton(onClick: () -> Unit) {
    BarIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tag = TripPlannerTestTags.SEARCH_STOP_BACK,
        onClick = onClick,
    )
}

/**
 * The X in the end column: it clears the field and does nothing else. It is a circle with a cross in it, the go circle's
 * own colours, the bar's text colour for the circle and the bar's fill for the cross, so it is the same kind of round control
 * as the go circle it takes the place of while there is text, and reads on the soft pill and on the lone pill alike.
 */
@Composable
private fun ClearTextButton(onClick: () -> Unit) {
    val colors = softFieldColors()
    CloseCircleButton(
        contentDescription = "Clear",
        onClick = onClick,
        circle = colors.contentColor,
        glyph = colors.containerColor,
        glyphSize = KrailTheme.dimensions.iconL,
        tag = TripPlannerTestTags.SEARCH_STOP_CLEAR,
        circleTag = TripPlannerTestTags.SEARCH_STOP_CLEAR_CIRCLE,
    )
}

/**
 * One icon button inside a bar. [RoundIconButton] swaps in its own content colour, so the bar's is
 * read first and the icon is tinted with it, which keeps the arrow and the X legible on the white
 * bar and on the dark one in both themes.
 *
 * In the dark bar the button is inside taj's text field, whose semantics merge everything in it, and
 * a tag on a node that does not merge is dropped by that. The tag's node merges its own children, so
 * it stays a node of its own and the tag is still how a test or a flow finds the button.
 */
@Composable
private fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit,
) {
    val barContent = LocalContentColor.current
    RoundIconButton(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.testTag(tag).semantics(mergeDescendants = true) {},
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(barContent),
            modifier = Modifier.size(KrailTheme.dimensions.iconM),
        )
    }
}
