package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState

@Composable
internal fun PreviewIntroStepOne(state: IntroState) {
    PreviewTheme {
        IntroFlowScreen(
            state = state,
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            readiness = StopsReadiness.Ready(stopCount = 100),
            onEvent = {},
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideAsk() = PreviewIntroStepOne(PreviewAskState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideLocating() = PreviewIntroStepOne(PreviewLocatingState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideNoStationNear() = PreviewIntroStepOne(PreviewNoStationNearState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideCardLoading() = PreviewIntroStepOne(PreviewCardLoadingState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideCardContent() = PreviewIntroStepOne(PreviewCardContentState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideCardUnavailable() = PreviewIntroStepOne(PreviewCardUnavailableState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideAdded() = PreviewIntroStepOne(PreviewAddedState)
