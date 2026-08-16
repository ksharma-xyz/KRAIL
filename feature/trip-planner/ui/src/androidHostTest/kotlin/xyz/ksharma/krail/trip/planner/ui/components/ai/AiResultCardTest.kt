package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.ResolvedTripIntent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The card replaces the input bar, and only for a trip that has both ends.
 *
 * The gate is the point of these tests. A half-resolved trip reaching this card would offer a
 * rider a timetable that cannot be loaded, and the surface has one job at that moment: fill the
 * field it found and get out of the way.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class AiResultCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bothStopsResolved_showsTheCardInsteadOfTheBar() {
        setContent(state(from = SEVEN_HILLS, to = WYNYARD))

        composeRule.onNodeWithText(SEVEN_HILLS.stopName).assertExists()
        composeRule.onNodeWithText(WYNYARD.stopName).assertExists()
        composeRule.onNodeWithText(SEE_TIMES_LABEL).assertExists()
    }

    @Test
    fun oneStopMissed_stillShowsTheCardWithThatFieldEmpty() {
        setContent(state(from = null, to = WYNYARD))

        // The miss is the case where seeing the answer matters most. Closing here dropped the
        // rider on the home row to work out for themselves which field had been filled.
        composeRule.onNodeWithText(WYNYARD.stopName).assertExists()
        composeRule.onNodeWithText(STARTING_FROM_PLACEHOLDER).assertExists()
    }

    @Test
    fun oneStopMissed_seeTimesDoesNothing() {
        var seeTimes = 0
        setContent(
            state(from = null, to = WYNYARD),
            actions = AiResultActions(onSeeTimes = { seeTimes++ }),
        )

        composeRule.onNodeWithText(SEE_TIMES_LABEL).performClick()

        // No timetable exists without both ends, so the button is inert rather than absent: a
        // vanished button leaves the rider guessing what the card is for.
        assertEquals(0, seeTimes)
    }

    @Test
    fun nothingResolved_keepsTheInputBar() {
        setContent(state(from = null, to = null))

        composeRule.onNodeWithText(SEE_TIMES_LABEL).assertDoesNotExist()
    }

    @Test
    fun stillWorking_keepsTheInputBarEvenWithStopsAlreadyOnState() {
        setContent(
            state(from = SEVEN_HILLS, to = WYNYARD, phase = AiSearchInputPhase.EXTRACTING),
        )

        composeRule.onNodeWithText(SEE_TIMES_LABEL).assertDoesNotExist()
    }

    @Test
    fun noTimeUnderstood_showsNoTimeChip() {
        setContent(state(from = SEVEN_HILLS, to = WYNYARD))

        // Nothing is invented to fill the gap. A rider cannot tell a departure time the app
        // made up from one it misheard them say, so it shows neither.
        composeRule.onNodeWithText(ARRIVE_TEXT).assertDoesNotExist()
    }

    @Test
    fun seeTimes_reportsExactlyOnce() {
        var seeTimes = 0
        setContent(
            state(from = SEVEN_HILLS, to = WYNYARD),
            actions = AiResultActions(onSeeTimes = { seeTimes++ }),
        )

        composeRule.onNodeWithText(SEE_TIMES_LABEL).performClick()

        assertEquals(1, seeTimes)
    }

    @Test
    fun tappingAStop_reportsThatFieldAndNotTheOther() {
        var editFrom = 0
        var editTo = 0
        setContent(
            state(from = SEVEN_HILLS, to = WYNYARD),
            actions = AiResultActions(onEditFrom = { editFrom++ }, onEditTo = { editTo++ }),
        )

        composeRule.onNodeWithText(SEVEN_HILLS.stopName).performClick()

        assertEquals(1, editFrom)
        assertEquals(0, editTo)
    }

    private fun setContent(
        state: AiSearchInputUiState,
        actions: AiResultActions = AiResultActions(),
    ) {
        composeRule.setContent {
            PreviewTheme {
                Column(modifier = Modifier.fillMaxWidth().height(HOST_HEIGHT)) {
                    AiInputContent(
                        state = state,
                        textFieldState = rememberTextFieldState(),
                        suggestion = SUGGESTION,
                        onEvent = {},
                        modifier = Modifier.weight(1f),
                        resultActions = actions,
                    )
                }
            }
        }
    }

    private fun state(
        from: StopItem?,
        to: StopItem?,
        phase: AiSearchInputPhase = AiSearchInputPhase.RESOLVED,
    ) = AiSearchInputUiState(
        isInputOpen = true,
        phase = phase,
        resolved = ResolvedTripIntent(
            fromText = from?.stopName,
            fromStopItem = from,
            toText = to?.stopName,
            toStopItem = to,
            dateTimeSelectionItem = null,
            modeHints = emptyList(),
        ),
    )

    private companion object {
        val HOST_HEIGHT = 800.dp
        const val SUGGESTION = "Home to Work by 9am"
        const val ARRIVE_TEXT = "Arrive"
        const val STARTING_FROM_PLACEHOLDER = "Starting from"
        val SEVEN_HILLS = StopItem(stopId = "2147", stopName = "Seven Hills Station")
        val WYNYARD = StopItem(stopId = "200070", stopName = "Wynyard Station")
    }
}
