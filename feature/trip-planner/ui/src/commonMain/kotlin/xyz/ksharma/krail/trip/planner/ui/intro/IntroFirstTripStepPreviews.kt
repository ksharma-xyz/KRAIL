package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// Real station names, never a rider's trip. Step 2 is shared by the phone, the 320dp and the tablet previews
// and by the layout tests, which draw the same states.

internal val PreviewTripNothingChosen = IntroState(step = IntroStep.DESTINATION)

/** The start is the station Add to home filled in, so the end list follows below it. */
internal val PreviewTripPrefilled = IntroState(
    step = IntroStep.DESTINATION,
    startStopId = "2155384",
    startName = "Tallawong Station",
    startFromParkRide = true,
    addedParkRide = true,
)

internal val PreviewTripBothChosen = IntroState(
    step = IntroStep.DESTINATION,
    startStopId = "2155384",
    startName = "Tallawong Station",
    endStopId = "200060",
    endName = "Central Station",
)

@Composable
internal fun PreviewIntroStepTwo(
    state: IntroState,
    readiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
) {
    PreviewTheme {
        IntroFlowScreen(
            state = state,
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            readiness = readiness,
            onEvent = {},
            firstTripStep = introFirstTripStep(onSearchStop = {}),
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripGettingReady() =
    PreviewIntroStepTwo(PreviewTripNothingChosen, readiness = StopsReadiness.Loading)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripStartList() = PreviewIntroStepTwo(PreviewTripNothingChosen)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripPrefilled() = PreviewIntroStepTwo(PreviewTripPrefilled)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripBothChosen() = PreviewIntroStepTwo(PreviewTripBothChosen)
