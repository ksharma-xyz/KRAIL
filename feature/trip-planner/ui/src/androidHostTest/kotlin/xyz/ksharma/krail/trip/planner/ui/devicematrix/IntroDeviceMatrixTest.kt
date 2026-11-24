package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceMatrixScope
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFlowScreen
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewCardContentState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision

/**
 * Every screen of the Intro on every device profile (`docs/testing/DEVICE_PROFILES.md`). The Intro pads the whole flow with the
 * safe drawing inset and lays its decision bar over the bottom, so each step's buttons, rows and badges must stand inside the
 * safe area on every device, the bar's two buttons must take a press, and a screen the rider scrolls must clear the bottom bar.
 * The steps are `IntroStep.entries`, so a screen added to the flow is held to this without anyone remembering to list it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class IntroDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private fun stateFor(step: IntroStep): IntroState = when (step) {
        IntroStep.PARK_RIDE -> PreviewCardContentState
        IntroStep.CONFIRM -> IntroState(
            step = IntroStep.CONFIRM,
            startStopId = "200060",
            startName = "Central Station",
            endStopId = "200080",
            endName = "Wynyard Station",
            tripDecision = TripDecision.SAVED,
        )
        else -> IntroState(step = step)
    }

    @Test
    fun `every Intro screen keeps its controls inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "IntroFlowScreen") {
            val shown = mutableStateOf(stateFor(IntroStep.entries.first()))
            setContent(theme = { BareKrailTheme(content = it) }) {
                IntroFlowScreen(
                    state = shown.value,
                    suggestedStops = DEFAULT_SUGGESTED_STOPS,
                    onEvent = { _, _ -> },
                )
            }
            for (step in IntroStep.entries) {
                shown.value = stateFor(step)
                rule.waitForIdle()

                assertNothingInteractiveUnderSystemBars()
                if (hasPrimaryAction()) assertTouchReaches(onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION))
            }
        }

    /** The bar's right button is there on most steps and hidden on the stop steps until a stop is picked. */
    private fun DeviceMatrixScope.hasPrimaryAction(): Boolean =
        onAllNodes(hasTestTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)).fetchSemanticsNodes().isNotEmpty()
}
