package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep

// The shell with both steps empty: the dots, the back button and the one bottom button.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroShellStepOne() {
    PreviewTheme {
        IntroFlowScreen(
            state = IntroState(),
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            readiness = StopsReadiness.Ready(stopCount = 100),
            onEvent = {},
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroShellStepTwoGettingReady() {
    PreviewTheme {
        IntroFlowScreen(
            state = IntroState(step = IntroStep.FIRST_TRIP),
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            readiness = StopsReadiness.Loading,
            onEvent = {},
        )
    }
}
