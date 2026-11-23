package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
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
    private val stepTwo = IntroState(step = IntroStep.DESTINATION)
    private val tripPair = IntroState(
        step = IntroStep.CONFIRM,
        startStopId = "200060",
        startName = "Central Station",
        endStopId = "200080",
        endName = "Wynyard Station",
    )

    private val shownState = mutableStateOf(ON_PARK_RIDE)
    private var contentSet = false

    /** Sets the content once, and from then on changes what it shows, as a test may only set it once. */
    private fun show(state: IntroState) {
        shownState.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shownState.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
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
        show(ON_PARK_RIDE)

        secondary().assertTextEquals(PARK_RIDE_NO).assertIsEnabled()
        primary().assertTextEquals(PARK_RIDE_YES).assertIsEnabled()
        assertTrue(
            secondary().getUnclippedBoundsInRoot().left <= primary().getUnclippedBoundsInRoot().left,
            "the negative answer is first, on the left or above",
        )
    }

    @Test
    fun `each answer sends its own event, and only the right one is the primary`() {
        show(ON_PARK_RIDE)

        secondary().performClick()
        primary().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.AnswerParkRide(yes = true)),
            events,
        )
    }

    @Test
    fun `the card bar says Not now and Add to home, and Add is live from the moment the card shows, with or without numbers`() {
        listOf(PreviewCardLoadingState, PreviewCardContentState, PreviewCardUnavailableState).forEach { state ->
            events.clear()
            show(state)

            secondary().assertTextEquals(PARK_RIDE_NOT_NOW).assertIsEnabled()
            primary().assertTextEquals(PARK_RIDE_ADD).assertIsEnabled()
            primary().performClick()
            secondary().performClick()

            assertEquals<List<IntroUiEvent>>(
                listOf(IntroUiEvent.AddParkRideAndContinue, IntroUiEvent.DeclineParkRide),
                events,
                "${state.parkRideCard}",
            )
        }
    }

    @Test
    fun `while locating the left button is live and Add to home is not shown, and its room is kept`() {
        show(PreviewCardContentState)
        val room = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).getUnclippedBoundsInRoot()

        show(PreviewLocatingState)

        secondary().assertTextEquals(PARK_RIDE_NOT_NOW).assertIsEnabled()
        primary().assertDoesNotExist()
        assertEquals(room, secondary().getUnclippedBoundsInRoot(), "the left button moved when Add to home went")
        secondary().performClick()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.DeclineParkRide), events)
    }

    @Test
    fun `a Park and Ride screen with nothing to decide shows no bar`() {
        listOf(
            ON_PARK_RIDE.copy(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.NoStationNear),
            ON_PARK_RIDE.copy(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.None),
        ).forEach { state ->
            show(state)

            primary().assertDoesNotExist()
            secondary().assertDoesNotExist()
        }
    }

    @Test
    fun `every Park and Ride screen that asks has two clickable buttons and no third, but Add to home is not there while locating`() {
        listOf(ON_PARK_RIDE, PreviewCardContentState, PreviewAddedState).forEach { state ->
            show(state)

            composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        }
        show(PreviewLocatingState)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun `the destination and origin screens have no button until a stop is selected, then the one button sends the confirm`() {
        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN).forEach { step ->
            events.clear()
            show(IntroState(step = step))

            primary().assertDoesNotExist()
            secondary().assertDoesNotExist()

            show(IntroState(step = step, selectedStop = CENTRAL_STOP))

            primary().assertTextEquals(STOP_CHOOSE).assertIsEnabled()
            secondary().assertDoesNotExist()
            primary().performClick()

            assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ConfirmStop), events, "$step")
        }
    }

    @Test
    fun `the confirm screen asks Not now and Yes save it, and neither is disabled`() {
        show(tripPair)

        secondary().assertTextEquals(CONFIRM_NOT_NOW).assertIsEnabled()
        primary().assertTextEquals(CONFIRM_SAVE).assertIsEnabled()
    }

    @Test
    fun `the trip answers send the answer events, which only record it`() {
        show(tripPair)

        primary().performClick()
        secondary().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AnswerSaveTrip(yes = true), IntroUiEvent.AnswerSaveTrip(yes = false)),
            events,
        )
    }

    @Test
    fun `the theme screen's one button is live whether the rider said yes to saving or not, and finishes the Intro`() {
        val saving = tripPair.copy(
            step = IntroStep.THEME,
            selectedThemeId = KrailThemeStyle.Train.id,
            tripDecision = TripDecision.SAVED,
        )
        show(saving)
        primary().assertTextEquals(THEME_CONFIRM).assertIsEnabled()

        show(saving.copy(tripDecision = TripDecision.DECLINED))
        primary().assertIsEnabled()

        primary().performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ConfirmTheme), events)
    }

    @Test
    fun `no screen has a button worded as a way past a question`() {
        val banned = listOf("Skip", "Later", "Next", "Continue", "Done")
        val selectedStop = stepTwo.copy(selectedStop = CENTRAL_STOP)
        listOf(ON_PARK_RIDE, PreviewCardContentState, selectedStop, tripPair).forEach { state ->
            show(state)

            // A screen with one button has no secondary one, so only the buttons that are there are read.
            val buttons = listOf(
                TripPlannerTestTags.INTRO_PRIMARY_ACTION,
                TripPlannerTestTags.INTRO_SECONDARY_ACTION,
            ).flatMap { composeRule.onAllNodes(hasTestTag(it)).fetchSemanticsNodes() }
            assertTrue(buttons.isNotEmpty(), "no button was found on $state")
            buttons.forEach { button ->
                val words = button.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
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
    fun `every screen starts its headline, the emoji first, 16 dp below the top, the same on all five`() {
        val headings = mapOf(
            IntroStep.PARK_RIDE to PARK_RIDE_HEADING,
            IntroStep.DESTINATION to DESTINATION_HEADING,
            IntroStep.ORIGIN to ORIGIN_HEADING,
            IntroStep.CONFIRM to CONFIRM_HEADING,
            IntroStep.THEME to PICK_THEME_HEADING,
        )
        val rhythm = introRhythm(KrailDimensions())

        headings.forEach { (step, heading) ->
            show(tripPair.copy(step = step))

            val bounds = composeRule.onNodeWithText(heading).getUnclippedBoundsInRoot()
            assertEquals(16.dp, rhythm.top)
            assertTrue(
                bounds.top >= rhythm.top - 1.dp && bounds.top <= rhythm.top + 1.dp,
                "$step: the emoji starts at ${bounds.top}, not ${rhythm.top} below the top",
            )
        }
    }

    @Test
    fun `no screen has a back button`() {
        IntroStep.entries.forEach { step ->
            show(IntroState(step = step))

            composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        }
    }

    @Test
    fun `system back on the Park and Ride screen, the first, is left to the platform and leaves the app`() {
        show(IntroState(step = IntroStep.PARK_RIDE))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(emptyList(), events)
        assertTrue(composeRule.activity.isFinishing, "the platform handled it")
    }

    @Test
    fun `system back on every screen after the first is consumed, so nothing is sent and the app stays`() {
        IntroStep.entries.filter { it != IntroStep.PARK_RIDE }.forEach { step ->
            show(IntroState(step = step))

            composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitForIdle()

            assertEquals<List<IntroUiEvent>>(emptyList(), events, "$step")
            assertTrue(!composeRule.activity.isFinishing, "$step left the app")
        }
    }
}
