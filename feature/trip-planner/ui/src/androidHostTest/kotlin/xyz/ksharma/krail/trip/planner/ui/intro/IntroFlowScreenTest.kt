package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The shell of the Intro: the bottom button, the back button and system back. The screens are left
 * empty here, because the shell is what is under test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFlowScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<IntroUiEvent>()
    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val stepTwo = IntroState(step = IntroStep.DESTINATION)
    private val tripPair = stepTwo.copy(
        startStopId = "200060",
        startName = "Central Station",
        endStopId = "200080",
        endName = "Wynyard Station",
    )

    private val shownState = mutableStateOf(IntroState())
    private val shownReadiness = mutableStateOf<StopsReadiness>(ready)
    private var contentSet = false

    /** Sets the content once, and from then on changes what it shows, as a test may only set it once. */
    private fun show(state: IntroState, readiness: StopsReadiness = ready) {
        shownState.value = state
        shownReadiness.value = readiness
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shownState.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        readiness = shownReadiness.value,
                        onEvent = { events += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun primary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    private fun secondary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)

    @Test
    fun `asking shows the two answers, no on the left and yes on the right under the test tags`() {
        show(IntroState())

        secondary().assertTextEquals(PARK_RIDE_NO).assertIsEnabled()
        primary().assertTextEquals(PARK_RIDE_YES).assertIsEnabled()
        assertTrue(
            secondary().getUnclippedBoundsInRoot().left <= primary().getUnclippedBoundsInRoot().left,
            "the negative answer is first, on the left or above",
        )
    }

    @Test
    fun `each answer sends its own event, and only the right one is the primary`() {
        show(IntroState())

        secondary().performClick()
        primary().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.AnswerParkRide(yes = true)),
            events,
        )
    }

    @Test
    fun `the card bar says Not now and Add to home, with Add off until the numbers have come`() {
        show(PreviewCardLoadingState)
        secondary().assertTextEquals(PARK_RIDE_NOT_NOW).assertIsEnabled()
        primary().assertTextEquals(PARK_RIDE_ADD).assertIsNotEnabled()

        show(PreviewCardContentState)
        primary().assertTextEquals(PARK_RIDE_ADD).assertIsEnabled()
        primary().performClick()
        secondary().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AddParkRideAndContinue, IntroUiEvent.DeclineParkRide),
            events,
        )
    }

    @Test
    fun `while locating the bar is still there, with no being a live answer and add off`() {
        show(PreviewLocatingState)

        secondary().assertTextEquals(PARK_RIDE_NOT_NOW).assertIsEnabled()
        primary().assertTextEquals(PARK_RIDE_ADD).assertIsNotEnabled()
    }

    @Test
    fun `a Park and Ride screen with nothing to decide shows no bar`() {
        listOf(
            IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.NoStationNear),
            IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.None),
        ).forEach { state ->
            show(state)

            primary().assertDoesNotExist()
            secondary().assertDoesNotExist()
        }
    }

    @Test
    fun `every screen that asks has exactly two clickable buttons and no third`() {
        listOf(IntroState(), PreviewLocatingState, PreviewCardContentState, PreviewAddedState).forEach { state ->
            show(state)

            composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        }
    }

    @Test
    fun `the trip screens ask Not now and Yes save it, and save waits only for stops that are still loading`() {
        show(stepTwo, StopsReadiness.Loading)
        secondary().assertTextEquals(TRIP_NOT_NOW).assertIsEnabled()
        primary().assertTextEquals(TRIP_SAVE).assertIsNotEnabled()

        show(stepTwo, StopsReadiness.Unknown)
        primary().assertIsNotEnabled()

        listOf(ready, StopsReadiness.Failed(attempt = 1, reason = "disk full")).forEach { readiness ->
            show(tripPair, readiness)
            primary().assertTextEquals(TRIP_SAVE).assertIsEnabled()
        }
        show(tripPair.copy(saved = true), StopsReadiness.Loading)
        primary().assertIsEnabled()
    }

    @Test
    fun `the trip answers send the finish events`() {
        show(tripPair)

        primary().performClick()
        secondary().performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.FinishPressed, IntroUiEvent.FinishWithoutTrip), events)
    }

    @Test
    fun `no screen has a button worded as a way past a question`() {
        val banned = listOf("Skip", "Later", "Next", "Continue", "Done")
        listOf(IntroState(), PreviewCardContentState, stepTwo, tripPair).forEach { state ->
            show(state)

            listOf(primary(), secondary()).forEach { button ->
                val words = button.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .joinToString { it.text }
                banned.forEach { word -> assertTrue(!words.contains(word, ignoreCase = true), "$words has $word") }
            }
        }
    }

    @Test
    fun `there are no progress dots on any screen`() {
        IntroStep.entries.forEach { step ->
            show(IntroState(step = step))

            composeRule.onNodeWithContentDescription("Step 1 of 2").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Step 2 of 2").assertDoesNotExist()
        }
    }

    @Test
    fun `step 1 has no back button`() {
        show(IntroState())

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun `the back button on step 2 sends Back`() {
        show(stepTwo)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.Back), events)
    }

    @Test
    fun `system back on step 2 sends Back`() {
        show(stepTwo)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.Back), events)
    }

    @Test
    fun `system back on step 1 is left to the platform`() {
        show(IntroState())

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(emptyList(), events)
    }
}
