package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
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
 *   search finds, best first. The first row is the highlighted one, the stop the right button takes. A tap on any
 *   row chooses it and moves on.
 * - **The field.** A taj [TextField] that takes the keyboard when the rider asks for it with the left button and
 *   not before, so the screen opens with the list in view. What is typed is the view model's to search and goes
 *   nowhere else. It starts from the view model's text and not from a saved copy, so a turn of the screen keeps it
 *   and a restart of the app does not.
 * - **No match.** The list is replaced by one line, and nothing is highlighted.
 * - **Stops not ready.** Nothing differs on screen: the suggestions are bundled with the app, and the field takes
 *   text and finds nothing until the stops are ready, with no message.
 *
 * The middle starts straight under the headline and does not centre, so the field and the rows never move when
 * their own number changes. The whole screen scrolls above the pinned bar, so with the keyboard up or large text
 * every row can still be reached.
 */
@Composable
internal fun IntroStopStepContent(
    state: IntroState,
    suggestedStops: SuggestedStops,
    focusRequester: FocusRequester,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headline = introStopHeadline(state.step) ?: return
    val rows = state.stopRows(suggestedStops)
    IntroStepFrame(
        headline = {
            IntroHeadline(
                lines = headline.lines,
                emphasisIndex = headline.emphasisIndex,
                emoji = introEmojiFor(state.step),
            )
        },
        modifier = modifier,
        centreMiddle = false,
    ) {
        Column {
            IntroStopField(
                step = state.step,
                startText = state.query,
                focusRequester = focusRequester,
                onEvent = onEvent,
            )
            if (state.search == IntroSearchPhase.NoMatch) {
                IntroStopNoMatch()
            } else {
                IntroStopList(rows = rows, modes = state.stopModes, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun IntroStopField(
    step: IntroStep,
    startText: String,
    focusRequester: FocusRequester,
    onEvent: (IntroUiEvent) -> Unit,
) {
    // Not saved: the view model has the text through a turn of the screen, and nothing has it after a restart.
    val fieldState = remember { TextFieldState(initialText = startText) }
    // The field reports the text it starts with as well, which the view model already has, so that is not sent.
    val lastSent = remember { mutableStateOf(startText) }
    val fieldType = if (step == IntroStep.ORIGIN) SearchStopFieldType.FROM else SearchStopFieldType.TO
    val bar = if (step == IntroStep.ORIGIN) ToFromField.From else ToFromField.To
    TextField(
        state = fieldState,
        placeholder = barPlaceholderFor(bar, fieldType, editTripLeg = false),
        imeAction = ImeAction.Search,
        // The search field of the page it is on, dark on a light page and light on a dark one, as Add Park & Ride has.
        colors = TextFieldDefaults.invertedColors(),
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
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .padding(bottom = KrailTheme.dimensions.spacingM)
            .focusRequester(focusRequester)
            .testTag(TripPlannerTestTags.INTRO_STOP_FIELD),
    )
}

@Composable
private fun IntroStopList(
    rows: List<IntroStop>,
    modes: ImmutableMap<String, ImmutableList<TransportMode>>,
    onEvent: (IntroUiEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, stop ->
            key(stop.stopId) {
                IntroStopRow(
                    stop = stop,
                    modes = modes[stop.stopId].orEmpty(),
                    highlighted = index == 0,
                    onClick = { onEvent(IntroUiEvent.ChooseStop(stop)) },
                )
            }
        }
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
