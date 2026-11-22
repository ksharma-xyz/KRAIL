package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import kotlin.test.assertEquals

/**
 * Step 2 on a phone: the suggestions, the collapsed row, the tile, changing a trip and the hand-off to
 * the stop search. The state is handed in, so each test sets exactly the state it is about, and a pick
 * is the event it sends.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFirstTripStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val events = mutableListOf<IntroUiEvent>()
    private val searches = mutableListOf<Boolean>()
    private val shown = mutableStateOf(PreviewTripNothingChosen)
    private val readiness = mutableStateOf<StopsReadiness>(StopsReadiness.Ready(stopCount = 100))

    @Composable
    private fun Screen() {
        PreviewTheme {
            IntroFlowScreen(
                state = shown.value,
                suggestedStops = DEFAULT_SUGGESTED_STOPS,
                readiness = readiness.value,
                onEvent = { events += it },
                firstTripStep = introFirstTripStep(onSearchStop = { searches += it }),
            )
        }
    }

    private fun show(state: IntroState) {
        shown.value = state
        composeRule.setContent { Screen() }
        composeRule.waitForIdle()
    }

    private fun pick(role: IntroStopRole, name: String) =
        composeRule.onNodeWithContentDescription(pickStopDescription(role, name))

    @Test
    fun `nothing chosen shows the heading, the start suggestions and Search another stop`() {
        show(PreviewTripNothingChosen)

        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertIsDisplayed()
        DEFAULT_SUGGESTED_STOPS.start.forEach { pick(IntroStopRole.START, it.name).assertIsDisplayed() }
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START)).assertIsDisplayed()
    }

    @Test
    fun `nothing chosen does not show the end list yet`() {
        show(PreviewTripNothingChosen)

        composeRule.onNodeWithText(FIRST_TRIP_TO).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.END)).assertDoesNotExist()
    }

    @Test
    fun `choosing a start suggestion sends PickStart for that stop`() {
        show(PreviewTripNothingChosen)

        pick(IntroStopRole.START, "Parramatta Station").performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.PickStart(SuggestedStop(stopId = "215020", name = "Parramatta Station"))),
            events,
        )
    }

    @Test
    fun `Search another stop on the start list asks to search for the start`() {
        show(PreviewTripNothingChosen)

        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START)).performClick()

        assertEquals(listOf(true), searches)
    }

    @Test
    fun `a pre-filled start is a collapsed row and the end list is below it`() {
        show(PreviewTripPrefilled)

        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(changeStopDescription(IntroStopRole.START, "Tallawong Station"))
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(FIRST_TRIP_TO).assertIsDisplayed()
        // The start list is not open, so none of its other suggestions is.
        pick(IntroStopRole.START, "Central Station").assertDoesNotExist()
        pick(IntroStopRole.END, "Wynyard Station").assertExists()
    }

    @Test
    fun `the end list leaves out the stop already chosen as the start`() {
        show(PreviewTripPrefilled)

        pick(IntroStopRole.END, "Tallawong Station").assertDoesNotExist()
        DEFAULT_SUGGESTED_STOPS.end.forEach { pick(IntroStopRole.END, it.name).assertExists() }
    }

    @Test
    fun `choosing an end suggestion sends PickEnd for that stop`() {
        show(PreviewTripPrefilled)

        pick(IntroStopRole.END, "Wynyard Station").performScrollTo().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.PickEnd(SuggestedStop(stopId = "200080", name = "Wynyard Station"))),
            events,
        )
    }

    @Test
    fun `Search another stop on the end list asks to search for the end`() {
        show(PreviewTripPrefilled)

        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.END))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(false), searches)
    }

    @Test
    fun `tapping the collapsed start opens both lists with the current start marked`() {
        show(PreviewTripPrefilled)

        composeRule.onNodeWithContentDescription(changeStopDescription(IntroStopRole.START, "Tallawong Station"))
            .performClick()

        // Changing opens both lists, so there is one place to change either end.
        pick(IntroStopRole.START, "Tallawong Station").assertIsSelected()
        pick(IntroStopRole.START, "Central Station").assertExists()
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.END)).assertExists()
    }

    @Test
    fun `both chosen is one tile with both names, and nothing from the lists`() {
        show(PreviewTripBothChosen)

        composeRule.onNodeWithContentDescription(
            changeTripDescription("Tallawong Station", "Central Station"),
        ).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Tallawong Station").assertIsDisplayed()
        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
        composeRule.onNodeWithText(SEARCH_ANOTHER_STOP).assertDoesNotExist()
        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertDoesNotExist()
    }

    @Test
    fun `tapping the tile opens both lists with the current picks marked`() {
        show(PreviewTripBothChosen)

        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .performClick()

        pick(IntroStopRole.START, "Tallawong Station").assertIsSelected()
        pick(IntroStopRole.END, "Central Station").performScrollTo().assertIsSelected()
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START)).assertExists()
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.END)).assertExists()
    }

    @Test
    fun `choosing in the open lists sends the pick and shows the tile again`() {
        show(PreviewTripBothChosen)
        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .performClick()

        pick(IntroStopRole.END, "Wynyard Station").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.PickEnd(SuggestedStop(stopId = "200080", name = "Wynyard Station"))),
            events,
        )
        // The state handed in has not changed, so this is the tile coming back because the rider picked.
        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .assertIsDisplayed()
    }

    @Test
    fun `a stop that came back from the search closes the open lists`() {
        show(PreviewTripBothChosen)
        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .performClick()
        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertIsDisplayed()

        shown.value = PreviewTripBothChosen.copy(endStopId = "10104", endName = "Hornsby Station")
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Hornsby Station"))
            .assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertDoesNotExist()
    }

    @Test
    fun `a turn of the screen keeps the open lists open`() {
        restorationTester.setContent { Screen() }
        shown.value = PreviewTripBothChosen
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .performClick()
        pick(IntroStopRole.START, "Tallawong Station").assertIsSelected()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        pick(IntroStopRole.START, "Tallawong Station").assertIsSelected()
        composeRule.onNodeWithText(FIRST_TRIP_FROM).assertIsDisplayed()
    }

    @Test
    fun `stops still loading shows the suggestions and a disabled save with no label of its own`() {
        readiness.value = StopsReadiness.Loading
        show(PreviewTripNothingChosen)

        DEFAULT_SUGGESTED_STOPS.start.forEach { pick(IntroStopRole.START, it.name).assertIsDisplayed() }
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithText(TRIP_SAVE).assertIsDisplayed()
        composeRule.onNodeWithText("Getting ready").assertDoesNotExist()
    }

    @Test
    fun `stops that failed show no error and a button that works`() {
        readiness.value = StopsReadiness.Failed(attempt = 1, reason = "stops")
        show(PreviewTripNothingChosen)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsEnabled()
        composeRule.onNodeWithText(TRIP_SAVE).assertIsDisplayed()
        composeRule.onNodeWithText(TRIP_NOT_NOW).assertIsDisplayed()
        DEFAULT_SUGGESTED_STOPS.start.forEach { pick(IntroStopRole.START, it.name).assertIsDisplayed() }
    }
}
