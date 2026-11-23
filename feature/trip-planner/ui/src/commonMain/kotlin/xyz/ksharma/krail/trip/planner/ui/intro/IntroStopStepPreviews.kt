package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

// Real station names, ids and modes, never a rider's trip. The destination and origin screens are shared by the
// phone and the 320dp previews and by the layout tests, which draw the same states.

private val Central = IntroStop("200060", "Central Station")
private val Wynyard = IntroStop("200080", "Wynyard Station")
private val Redfern = IntroStop("201510", "Redfern Station")
private val Museum = IntroStop("200040", "Museum Station")
private val StJames = IntroStop("200050", "St James Station")
private val Chatswood = IntroStop("206710", "Chatswood Station")
private val ChatswoodStandA = IntroStop("206720", "Chatswood Station, Stand A")

private val PreviewModes = persistentMapOf(
    "200060" to persistentListOf(TransportMode.Train, TransportMode.Metro, TransportMode.LightRail),
    "200080" to persistentListOf(TransportMode.Train, TransportMode.Metro),
    "215020" to persistentListOf(TransportMode.Train, TransportMode.LightRail),
    "200070" to persistentListOf(TransportMode.Train),
    "200030" to persistentListOf(TransportMode.Train, TransportMode.Metro),
    "2155384" to persistentListOf(TransportMode.Metro),
    "276220" to persistentListOf(TransportMode.Train),
    "201510" to persistentListOf(TransportMode.Train),
    "200040" to persistentListOf(TransportMode.Train),
    "200050" to persistentListOf(TransportMode.Train),
    "206710" to persistentListOf(TransportMode.Train, TransportMode.Metro),
    "206720" to persistentListOf(TransportMode.Bus),
)

/** The destination screen as it opens: the curated ends with none selected, an empty field and no button yet. */
internal val PreviewDestinationSuggestions = IntroState(step = IntroStep.DESTINATION, stopModes = PreviewModes)

/** The destination screen after a row was tapped: that row is selected and the one button has appeared. */
internal val PreviewDestinationSelected = IntroState(
    step = IntroStep.DESTINATION,
    stopModes = PreviewModes,
    selectedStop = Wynyard,
)

/** The destination screen after the rider typed: the stops that match replace the suggestions. */
internal val PreviewDestinationTyped = IntroState(
    step = IntroStep.DESTINATION,
    stopModes = PreviewModes,
    query = "chats",
    search = IntroSearchPhase.Results(persistentListOf(Chatswood, ChatswoodStandA)),
)

/** The destination screen after the rider typed something nothing matches. */
internal val PreviewDestinationNoMatch = IntroState(
    step = IntroStep.DESTINATION,
    stopModes = PreviewModes,
    query = "zzzz",
    search = IntroSearchPhase.NoMatch,
)

/** The origin screen with the stations near the rider leading the curated starts. */
internal val PreviewOriginNearby = IntroState(
    step = IntroStep.ORIGIN,
    endStopId = Wynyard.stopId,
    endName = Wynyard.name,
    stopModes = PreviewModes,
    nearbyStops = persistentListOf(Redfern, Museum, StJames),
)

/** The origin screen with no fix, so only the curated starts are listed. */
internal val PreviewOriginCurated = IntroState(
    step = IntroStep.ORIGIN,
    endStopId = Central.stopId,
    endName = Central.name,
    stopModes = PreviewModes,
)

@Composable
internal fun PreviewIntroStopStep(state: IntroState) {
    PreviewTheme {
        IntroFlowScreen(
            state = state,
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            onEvent = {},
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationSuggestions() = PreviewIntroStopStep(PreviewDestinationSuggestions)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationSelected() = PreviewIntroStopStep(PreviewDestinationSelected)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationTyped() = PreviewIntroStopStep(PreviewDestinationTyped)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationNoMatch() = PreviewIntroStopStep(PreviewDestinationNoMatch)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroOriginNearby() = PreviewIntroStopStep(PreviewOriginNearby)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroOriginCurated() = PreviewIntroStopStep(PreviewOriginCurated)
