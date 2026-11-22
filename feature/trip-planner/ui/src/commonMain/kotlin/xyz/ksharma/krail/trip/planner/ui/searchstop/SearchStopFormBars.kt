package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.CloseIcon
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlock
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The form at the top of Search Stop: the app's To and From block with the search field typed into
 * the bar being edited, in place (rules 35 and 36 of `docs/MAP_HOME_UX.md`).
 *
 * - The back arrow stays on the To bar and the origin ring on the From bar in every state.
 * - The X shows only while the field has text, and only clears it. It never leaves the screen, so
 *   the recents stay where they are.
 * - Tapping the other bar moves the dark fill to it and the field with it. No row moves.
 * - The go circle is drawn only when [goVisible] says the trip can start.
 * - From shows [autoFrom], the stop nearest the rider, when location is known.
 */
@Composable
internal fun SearchStopForm(
    shape: FormShape,
    editing: ToFromField,
    toPlaceholder: String,
    fromPlaceholder: String,
    autoFrom: StopItem?,
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    onFlowEvent: (SearchStopFlowEvent) -> Unit,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasText by remember(fieldState) { derivedStateOf { fieldState.text.isNotEmpty() } }
    val placeholder = if (editing == ToFromField.From) fromPlaceholder else toPlaceholder
    val editor: @Composable () -> Unit = {
        SearchStopFieldEditor(
            placeholder = placeholder,
            fieldState = fieldState,
            focusRequester = focusRequester,
            onFocused = { onFlowEvent(SearchStopFlowEvent.BarTapped) },
            onTextChange = onTextChange,
        )
    }
    // The X stays in the column even while it has nothing to draw, so the field never shifts a
    // step to the right the moment the rider types.
    val clear: @Composable () -> Unit = {
        if (hasText) ClearTextButton(onClick = { fieldState.clearText() })
    }

    ToFromBlock(
        toStopItem = null,
        fromStopItem = autoFrom,
        onToClick = { switchEditing(fieldState, editing, ToFromField.To, onFlowEvent) },
        onFromClick = { switchEditing(fieldState, editing, ToFromField.From, onFlowEvent) },
        onGoClick = {},
        modifier = modifier,
        editing = editing,
        showFrom = shape == FormShape.ToAndFrom,
        toLeading = { BackArrowButton(onClick = onBack) },
        toTrailing = { if (editing == ToFromField.To) clear() },
        toPlaceholder = toPlaceholder,
        fromPlaceholder = fromPlaceholder,
        showGo = goVisible(from = autoFrom, to = null),
        toEditor = if (editing == ToFromField.To) editor else null,
        fromEditor = if (editing == ToFromField.From) editor else null,
        fromTrailing = if (editing == ToFromField.From) clear else null,
    )
}

/** Moves editing to [target], clearing the text that was a search for the other field. */
private fun switchEditing(
    fieldState: TextFieldState,
    current: ToFromField,
    target: ToFromField,
    onFlowEvent: (SearchStopFlowEvent) -> Unit,
) {
    if (current != target) fieldState.clearText()
    onFlowEvent(SearchStopFlowEvent.FieldTapped(target))
}

/**
 * The text field inside the dark bar: no fill of its own, the bar's colours, and the one query
 * field tag every flow and test reaches the field by. Gaining focus is the tap that raises the
 * list state.
 */
@Composable
private fun SearchStopFieldEditor(
    placeholder: String,
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onTextChange: (String) -> Unit,
) {
    val inverted = TextFieldDefaults.invertedColors()
    TextField(
        state = fieldState,
        placeholder = placeholder,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .testTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD),
        // The redaction cap, not a second number. A query longer than this is dropped from
        // analytics entirely, so a field that accepted more would collect text the rider could
        // never see reported, and one that accepts less makes that branch unreachable.
        maxLength = SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH,
        filter = { input -> input.filter { it.isLetterOrDigit() || it.isWhitespace() || it == ',' } },
        colors = TextFieldColors(
            containerColor = Color.Transparent,
            contentColor = inverted.contentColor,
            placeholderColor = inverted.placeholderColor,
            cursorColor = inverted.cursorColor,
        ),
        contentPadding = PaddingValues(0.dp),
    ) { value -> onTextChange(value.toString()) }
}

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

/** The X in the end column: it clears the field and does nothing else. */
@Composable
private fun ClearTextButton(onClick: () -> Unit) {
    BarIconButton(
        icon = CloseIcon,
        contentDescription = "Clear",
        tag = TripPlannerTestTags.SEARCH_STOP_CLEAR,
        onClick = onClick,
    )
}

/**
 * One icon button inside a bar. [RoundIconButton] swaps in its own content colour, so the bar's is
 * read first and the icon is tinted with it, which keeps the arrow and the X legible on the white
 * bar and on the dark one in both themes.
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
        modifier = Modifier.testTag(tag),
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(barContent),
            modifier = Modifier.size(KrailTheme.dimensions.iconM),
        )
    }
}
