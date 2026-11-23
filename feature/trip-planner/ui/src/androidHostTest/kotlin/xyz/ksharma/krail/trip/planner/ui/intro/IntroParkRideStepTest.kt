package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Park & Ride screen on a phone: the headline, every state of the card, and the bar that holds the
 * answers.
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
                        onEvent = { event, _ -> events += event },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun slotHeight() = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).getUnclippedBoundsInRoot().height

    @Test
    fun `asking shows the headline and both answers in the bar and no card`() {
        show(PreviewAskState)

        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_YES).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_NO).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_ADD).assertDoesNotExist()
    }

    @Test
    fun `the headline is one heading with the whole phrase, and the emoji is hidden from a screen reader`() {
        show(PreviewAskState)

        composeRule.onAllNodes(isHeading()).assertCountEquals(1)
        composeRule.onNode(isHeading()).assertTextEquals(PARK_RIDE_HEADING)
        val emoji = introEmojiFor(IntroStep.PARK_RIDE)
        composeRule.onNodeWithText(emoji).assertDoesNotExist()
        composeRule.onNodeWithText(emoji, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the headline sits above the card, the card above the bar, and the bar is inside the window`() {
        show(PreviewCardContentState)

        val headline = composeRule.onNode(isHeading()).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot()
        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertTrue(
            headline.bottom <= card.top,
            "the headline ends at ${headline.bottom}, below the card at ${card.top}",
        )
        assertTrue(card.bottom <= bar.top, "the card ends at ${card.bottom}, below the bar at ${bar.top}")
        assertTrue(bar.bottom <= window.bottom, "the bar ends at ${bar.bottom}, outside the ${window.bottom} window")
    }

    @Test
    fun `the answers are not in the screen, only in the bar, so the screen holds no button of its own`() {
        show(PreviewAskState)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
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
    fun `locating shows a card with no names and no numbers`() {
        show(PreviewLocatingState)

        composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Bella Vista").assertDoesNotExist()
        composeRule.onNodeWithText("spots available").assertDoesNotExist()
    }

    @Test
    fun `a card still loading shows the car park name but no numbers`() {
        show(PreviewCardLoadingState)

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
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
    fun `an unavailable card shows the name with no numbers`() {
        show(PreviewCardUnavailableState)

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        composeRule.onNodeWithText("spots available").assertDoesNotExist()
    }

    @Test
    fun `an added card is the same card, and the bar still reads Not now and Add to home`() {
        show(PreviewAddedState)

        composeRule.onNodeWithText("120").assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_ADD).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_NOT_NOW).assertIsDisplayed()
    }

    @Test
    fun `the card has no action of its own, so tapping it sends nothing`() {
        show(PreviewCardContentState)

        composeRule.onNodeWithText("Bella Vista").performClick()

        assertEquals<List<IntroUiEvent>>(emptyList(), events)
    }

    @Test
    fun `the bar keeps its place and size from locating to the card with numbers, with Add to home appearing in its room`() {
        val bars = listOf(
            "locating" to PreviewLocatingState,
            "loading" to PreviewCardLoadingState,
            "content" to PreviewCardContentState,
        ).map { (name, state) ->
            show(state)
            // The left button is there in every one of them, and Add to home only once a card shows.
            name to composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).getUnclippedBoundsInRoot()
        }

        val first = bars.first().second
        bars.forEach { (name, bounds) ->
            assertTrue(
                kotlin.math.abs(bounds.top.value - first.top.value) < HEIGHT_TOLERANCE_DP &&
                    kotlin.math.abs(bounds.height.value - first.height.value) < HEIGHT_TOLERANCE_DP,
                "the bar moves or changes height at $name: $bars",
            )
        }
    }

    @Test
    fun `the card keeps one height from the skeleton to the numbers and on to added`() {
        val heights = listOf(
            "locating" to PreviewLocatingState,
            "loading" to PreviewCardLoadingState,
            "content" to PreviewCardContentState,
            "unavailable" to PreviewCardUnavailableState,
            "added" to PreviewAddedState,
            "no station near, the message" to PreviewNoStationState,
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

    @Test
    fun `no station near is the message in the card's room, the one OK button, and nothing else`() {
        show(PreviewNoStationState)

        composeRule.onNodeWithText(PARK_RIDE_NO_STATION).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertTextEquals(PARK_RIDE_NO_STATION_OK)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()
        composeRule.onNodeWithText("Bella Vista").assertDoesNotExist()
        composeRule.onNodeWithText(PARK_RIDE_NOT_NOW).assertDoesNotExist()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.AcknowledgeNoStation), events)
    }

    private companion object {
        const val HEIGHT_TOLERANCE_DP = 0.5f
    }
}
