package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchQueryAnalyticsRedaction
import xyz.ksharma.krail.trip.planner.ui.searchstop.barPlaceholderFor
import xyz.ksharma.krail.trip.planner.ui.searchstop.stopSearchInputFilter
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

/** What the list says when the typed search finds no stop. The app's own wording, without its exclamation mark. */
internal const val STOP_NO_MATCH = "No match found"

/**
 * The destination and origin screens, which differ only in their headline and in the stop they choose: the
 * headline, a search field, and the stops below it as the rows Search Stop draws.
 *
 * - **The list.** What [IntroState.stopRows] says: the suggestions until the rider types, then the stops the local
 *   search finds, best first. No row is selected when the screen opens, so nothing reads as already chosen. A tap on
 *   a row selects it, which shows the single button in the bar, and a tap on another row moves the selection. The
 *   button confirms the stop and moves on.
 * - **The field.** A taj [TextField] that takes the keyboard when the rider taps it, so the screen opens with the list
 *   in view. What is typed is the view model's to search and goes
 *   nowhere else. It starts from the view model's text and not from a saved copy, so a turn of the screen keeps it
 *   and a restart of the app does not.
 * - **No match.** The list is replaced by one line, and nothing is selected.
 * - **Stops not ready.** Nothing differs on screen: the suggestions are bundled with the app, and the field takes
 *   text and finds nothing until the stops are ready, with no message.
 * - **The ground.** [ground] is the colour of the window behind the screen, which the sticky field is opaque in.
 * - **The frame.** One list that scrolls as a whole (`IntroStickyStopFrame`): the emoji and the headline, the field, which
 *   sticks at the top of the screen when the rider scrolls, and the rows, which slide under it. So with the keyboard up or
 *   large text every row can still be reached, and the field is always in reach.
 */
@Composable
internal fun IntroStopStepContent(
    state: IntroState,
    suggestedStops: SuggestedStops,
    onEvent: (IntroUiEvent) -> Unit,
    ground: Color,
    modifier: Modifier = Modifier,
) {
    val headline = introStopHeadline(state.step) ?: return
    val rows = state.stopRows(suggestedStops)
    IntroStickyStopFrame(
        headline = {
            IntroHeadline(
                lines = headline.lines,
                emphasisIndex = headline.emphasisIndex,
                emoji = introEmojiFor(state.step),
                spoken = headline.spoken,
                heroLayout = true,
            )
        },
        bar = {
            IntroStopField(
                step = state.step,
                startText = state.query,
                onEvent = onEvent,
            )
        },
        ground = ground,
        modifier = modifier,
    ) {
        if (state.search == IntroSearchPhase.NoMatch) {
            item(key = INTRO_STOP_NO_MATCH_KEY) { IntroStopNoMatch() }
        } else {
            introStopItems(
                rows = rows,
                modes = state.stopModes,
                selectedStopId = state.selectedStop?.stopId,
                onEvent = onEvent,
            )
        }
    }
}

/** The key of the one line that stands in for the rows when the search finds nothing. */
private const val INTRO_STOP_NO_MATCH_KEY = "intro-no-match"

@Composable
private fun IntroStopField(
    step: IntroStep,
    startText: String,
    onEvent: (IntroUiEvent) -> Unit,
) {
    // Not saved: the view model has the text through a turn of the screen, and nothing has it after a restart.
    val fieldState = remember { TextFieldState(initialText = startText) }
    // The field reports the text it starts with as well, which the view model already has, so that is not sent.
    val lastSent = remember { mutableStateOf(startText) }
    val fieldType = if (step == IntroStep.ORIGIN) SearchStopFieldType.FROM else SearchStopFieldType.TO
    val bar = if (step == IntroStep.ORIGIN) ToFromField.From else ToFromField.To
    // The page padding is on a plain box around the field, the same at the start and the end, and the field fills what is
    // left of it, so the field's own modifier chain has no say in where its edges are.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .padding(bottom = introRhythm(KrailTheme.dimensions).fieldToList),
    ) {
        IntroSearchField(fieldState, lastSent, bar, fieldType, onEvent)
    }
}

@Composable
private fun IntroSearchField(
    fieldState: TextFieldState,
    lastSent: MutableState<String>,
    bar: ToFromField,
    fieldType: SearchStopFieldType,
    onEvent: (IntroUiEvent) -> Unit,
) {
    TextField(
        state = fieldState,
        placeholder = barPlaceholderFor(bar, fieldType, editTripLeg = false),
        imeAction = ImeAction.Search,
        colors = introStopFieldColors(),
        // The same cap and the same filter as Search Stop's field, so a query works the same in both.
        maxLength = SearchQueryAnalyticsRedaction.MAX_QUERY_LENGTH,
        filter = ::stopSearchInputFilter,
        onTextChange = { text ->
            if (text.toString() != lastSent.value) {
                lastSent.value = text.toString()
                onEvent(IntroUiEvent.QueryChanged(text.toString()))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.INTRO_STOP_FIELD),
    )
}

/**
 * The colours of the stop field, in the one place: the soft pill colours the app's search fields share (`softFieldColors`), a
 * grey fill a touch darker than the page in light mode and a touch lighter in dark mode, with the page's text on it. Taj's
 * default colours were the page surface itself, so on the Intro's page the field had no fill and read as a heading, and the
 * inverted set was a harsh black bar.
 */
@Composable
internal fun introStopFieldColors(): TextFieldColors = softFieldColors()

// One item for each stop, keyed by the stop. A stop is listed once, because a list refuses a key it has seen.
private fun LazyListScope.introStopItems(
    rows: List<IntroStop>,
    modes: ImmutableMap<String, ImmutableList<TransportMode>>,
    selectedStopId: String?,
    onEvent: (IntroUiEvent) -> Unit,
) {
    itemsIndexed(items = rows.distinctBy { it.stopId }, key = { _, stop -> stop.stopId }) { index, stop ->
        IntroStopRow(
            stop = stop,
            modes = modes[stop.stopId].orEmpty(),
            selected = stop.stopId == selectedStopId,
            onClick = { onEvent(IntroUiEvent.SelectStop(stop)) },
            // A tag for whichever stop is first, which a flow that only needs to select one can tap.
            modifier = if (index == 0) Modifier.testTag(TripPlannerTestTags.INTRO_STOP_FIRST) else Modifier,
        )
    }
}

@Composable
private fun IntroStopNoMatch() {
    Text(
        text = STOP_NO_MATCH,
        style = KrailTheme.typography.titleLarge,
        color = KrailTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .padding(top = KrailTheme.dimensions.spacingXXL)
            // Read out when it appears, because the list it replaced was the thing the rider was watching.
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
