package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Step 1 on a phone: the question, the two answers, and every state of the card under them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroParkRideStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<IntroUiEvent>()
    private val shown = mutableStateOf(PreviewAskState)
    private var contentSet = false

    /** Sets the content once, then changes what it shows, because a test may only set content once. */
    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shown.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        readiness = StopsReadiness.Ready(stopCount = 100),
                        onEvent = { events += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun slotHeight() = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).getUnclippedBoundsInRoot().height

    @Test
    fun `asking shows the question and both answers and no card`() {
        show(PreviewAskState)

        composeRule.onNodeWithText(PARK_RIDE_QUESTION).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_YES).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_NO).assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertDoesNotExist()
    }

    @Test
    fun `tapping Yes sends the yes answer`() {
        show(PreviewAskState)

        composeRule.onNodeWithText(PARK_RIDE_YES).performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.AnswerParkRide(yes = true)), events)
    }

    @Test
    fun `tapping No sends the no answer`() {
        show(PreviewAskState)

        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.AnswerParkRide(yes = false)), events)
    }

    @Test
    fun `the answer the rider gave is the one shown as selected`() {
        show(PreviewLocatingState)
        composeRule.onNodeWithText(PARK_RIDE_YES).assertIsSelected()

        show(PreviewAskState.copy(parkRideAnswer = ParkRideAnswer.NO))
        composeRule.onNodeWithText(PARK_RIDE_NO).assertIsSelected()
    }

    @Test
    fun `locating shows a card with no names, no numbers and no row`() {
        show(PreviewLocatingState)

        composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertDoesNotExist()
        composeRule.onNodeWithText("Bella Vista").assertDoesNotExist()
        composeRule.onNodeWithText("spots available").assertDoesNotExist()
    }

    @Test
    fun `no station near shows one plain line and no card`() {
        show(PreviewNoStationNearState)

        composeRule.onNodeWithText(NO_STATION_NEAR_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertDoesNotExist()
    }

    @Test
    fun `a card still loading shows the car park name and the row but no numbers`() {
        show(PreviewCardLoadingState)

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("spots available").assertDoesNotExist()
        composeRule.onNodeWithText("full").assertDoesNotExist()
    }

    @Test
    fun `a card with numbers shows spots, percent full and the time of the reading`() {
        show(PreviewCardContentState)

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        composeRule.onNodeWithText("120").assertIsDisplayed()
        composeRule.onNodeWithText("spots available").assertIsDisplayed()
        composeRule.onNodeWithText("40%").assertIsDisplayed()
        composeRule.onNodeWithText("full").assertIsDisplayed()
        composeRule.onNodeWithText("Last updated at\u00A08:05\u00A0AM").assertIsDisplayed()
    }

    @Test
    fun `an unavailable card shows the name and Add with no numbers`() {
        show(PreviewCardUnavailableState)

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("spots available").assertDoesNotExist()
    }

    @Test
    fun `an added card is the same card with the row reading Added to home`() {
        show(PreviewAddedState)

        composeRule.onNodeWithText("120").assertIsDisplayed()
        composeRule.onNodeWithText(ADDED_TO_HOME_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).assertDoesNotExist()
    }

    @Test
    fun `tapping the Add to home row toggles it once`() {
        show(PreviewCardContentState)

        composeRule.onNodeWithText(ADD_TO_HOME_TEXT).performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ToggleParkRide), events)
    }

    @Test
    fun `tapping Added to home toggles it back`() {
        show(PreviewAddedState)

        composeRule.onNodeWithText(ADDED_TO_HOME_TEXT).performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ToggleParkRide), events)
    }

    @Test
    fun `the card keeps one height from the skeleton to the numbers and on to added`() {
        val heights = listOf(
            "locating" to PreviewLocatingState,
            "no station near" to PreviewNoStationNearState,
            "loading" to PreviewCardLoadingState,
            "content" to PreviewCardContentState,
            "unavailable" to PreviewCardUnavailableState,
            "added" to PreviewAddedState,
        ).map { (name, state) ->
            show(state)
            name to slotHeight()
        }

        val first = heights.first().second
        heights.forEach { (name, height) ->
            assertTrue(
                kotlin.math.abs(height.value - first.value) < HEIGHT_TOLERANCE_DP,
                "$name is $height high and locating is $first, so the card jumps: $heights",
            )
        }
    }

    private companion object {
        const val HEIGHT_TOLERANCE_DP = 0.5f
    }
}
