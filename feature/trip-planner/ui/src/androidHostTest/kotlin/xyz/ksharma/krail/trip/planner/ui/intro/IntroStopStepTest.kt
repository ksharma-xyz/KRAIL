package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The destination and origin screens with the real view model and the fakes at the edges: the list with no row
 * selected, selecting by tapping a row, confirming with the one button, typing, no match and a turn of the screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroStopStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }
    private val analytics = FakeAnalytics()
    private val stopResults = FakeStopResultsManager()

    private val viewModel by lazy {
        IntroFlowViewModel(
            savedStateHandle = savedAt(IntroState(step = IntroStep.DESTINATION)),
            appDataReadiness = FakeAppDataReadiness(initial = StopsReadiness.Ready(stopCount = 100)),
            completion = IntroCompletion(FakeSandook(), FakeSandookPreferences(), analytics, Dispatchers.Unconfined),
            timeSource = TimeSource.Monotonic,
            parkRide = IntroParkRideGateway(
                catalogue = FakeParkRideCatalogue(stations = emptyList()),
                availabilityLoader = FakeParkRideAvailabilityLoader(),
                parkRideSandook = FakeNswParkRideSandook(),
                ioDispatcher = Dispatchers.Unconfined,
                stopNameLookup = { null },
                analytics = analytics,
            ),
            stops = fakeStopsGateway(stopResults = stopResults),
        )
    }

    private val ends get() = DEFAULT_SUGGESTED_STOPS.end
    private val starts get() = DEFAULT_SUGGESTED_STOPS.start

    private fun show() {
        restorationTester.setContent {
            PreviewTheme {
                IntroFlowRoute(
                    viewModel = viewModel,
                    onNavigateToSavedTrips = {},
                    onApplyTheme = {},
                    userLocationManager = FakeUserLocationManager(),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun row(stopId: String) = composeRule.onNodeWithTag(TripPlannerTestTags.introStop(stopId))

    private fun stopResult(stop: SuggestedStop) = SearchStopState.SearchResult.Stop(
        stopName = stop.name,
        stopId = stop.stopId,
        transportModeType = persistentListOf(TransportMode.Train),
    )

    private fun type(text: String) {
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).performTextInput(text)
        composeRule.waitForIdle()
        // The pause before the search is on the main looper's clock, which a paused test looper does not move.
        composeRule.runOnUiThread {
            ShadowLooper.idleMainLooper(INTRO_SEARCH_DEBOUNCE_MS * 3, TimeUnit.MILLISECONDS)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the destination screen shows the headline, the field and the five curated ends in order`() {
        show()

        composeRule.onNodeWithText(DESTINATION_HEADING).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertIsDisplayed()
        ends.forEach { row(it.stopId).assertIsDisplayed() }
        val tops = ends.map { row(it.stopId).fetchSemanticsNode().positionInRoot.y }
        assertEquals(tops.sorted(), tops, "the rows are in the order of the list")
    }

    @Test
    fun `the rows are Search Stop's rows without the label chevron, so nothing on them can expand`() {
        show()

        composeRule.onAllNodesWithContentDescription("Assign to a label").assertCountEquals(0)
        ends.forEach { row(it.stopId).assertHasClickAction() }
    }

    @Test
    fun `the first row carries a tag that does not depend on the stop, and it selects that row`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIRST).performClick()
        composeRule.waitForIdle()

        row(ends.first().stopId).assertIsSelected()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertExists()
    }

    @Test
    fun `no row is announced as selected when the screen opens, and there is no button`() {
        show()

        val selected = ends.map {
            row(it.stopId).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected)
        }

        assertEquals(listOf(false, false, false, false, false), selected)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()
        composeRule.onNodeWithText(STOP_CHOOSE).assertDoesNotExist()
    }

    @Test
    fun `tapping a row selects it, announces it as selected and shows the one button, and the screen stays`() {
        show()

        row(ends[1].stopId).performClick()
        composeRule.waitForIdle()

        assertEquals(ends[1].stopId, viewModel.state.value.selectedStop?.stopId)
        assertEquals(IntroStep.DESTINATION, viewModel.state.value.step)
        assertEquals(null, viewModel.state.value.endStopId)
        row(ends[1].stopId).assertIsSelected()
        ends.filterNot { it == ends[1] }.forEach { row(it.stopId).assertIsNotSelected() }
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertTextEquals(STOP_CHOOSE).assertIsEnabled()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()
    }

    @Test
    fun `tapping another row moves the selection`() {
        show()

        row(ends[1].stopId).performClick()
        row(ends[3].stopId).performClick()
        composeRule.waitForIdle()

        row(ends[1].stopId).assertIsNotSelected()
        row(ends[3].stopId).assertIsSelected()
        assertEquals(ends[3].stopId, viewModel.state.value.selectedStop?.stopId)
    }

    @Test
    fun `Yes this one sets the selected row as the end and moves to the origin screen without it in the list`() {
        show()
        row(ends[1].stopId).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(ends[1].stopId, viewModel.state.value.endStopId)
        composeRule.onNodeWithText(ORIGIN_HEADING).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.introStop(ends[1].stopId)).assertDoesNotExist()
        starts.filterNot { it.stopId == ends[1].stopId }.forEach { row(it.stopId).assertIsNotSelected() }
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
    }

    @Test
    fun `the search field is the only thing that opens the keyboard, and there is no No search button`() {
        show()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertIsNotFocused()
        composeRule.onNodeWithText("No, search").assertDoesNotExist()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertIsFocused()
        assertEquals(IntroStep.DESTINATION, viewModel.state.value.step)
        assertEquals(null, viewModel.state.value.selectedStop)
    }

    @Test
    fun `typing replaces the suggestions with the stops found, with none of them selected`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()

        type("chat")

        composeRule.onNodeWithText("Chatswood Station").assertIsDisplayed()
        ends.forEach { row(it.stopId).assertDoesNotExist() }
        row("206710").assertIsNotSelected()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
        assertEquals(listOf("chat" to false), stopResults.searches)
    }

    @Test
    fun `selecting a typed result and confirming sets it as the end and counts the search as used`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()
        type("chat")

        row("206710").performClick()
        composeRule.waitForIdle()
        row("206710").assertIsSelected()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals("206710", viewModel.state.value.endStopId)
        assertTrue(viewModel.state.value.usedSearch)
        composeRule.onNodeWithText(ORIGIN_HEADING).assertIsDisplayed()
        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `typing after a row was selected drops the selection and the button goes`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()
        row(ends[0].stopId).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertExists()

        type("chat")

        assertEquals(null, viewModel.state.value.selectedStop)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
    }

    @Test
    fun `a search with no match shows the line, selects nothing and shows no button`() {
        stopResults.answer = emptyList()
        show()

        type("zzzz")

        composeRule.onNodeWithText(STOP_NO_MATCH).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()
        ends.forEach { row(it.stopId).assertDoesNotExist() }
    }

    @Test
    fun `a turn of the screen keeps the typed text and the stops found, and a chosen end`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()
        type("chat")

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertTextContains("chat")
        composeRule.onNodeWithText("Chatswood Station").assertIsDisplayed()
    }

    @Test
    fun `the origin screen lists the curated starts, leaves out the chosen end and selects none`() {
        show()
        row(ends[1].stopId).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        val expected = starts.filterNot { it.stopId == ends[1].stopId }
        expected.forEach {
            row(it.stopId).assertIsDisplayed()
            row(it.stopId).assertIsNotSelected()
        }
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
        assertFalse(viewModel.state.value.step == IntroStep.CONFIRM)
    }

    @Test
    fun `the origin screen works the same way, a tap selects and the one button confirms and reaches the confirm screen`() {
        show()
        row(ends[1].stopId).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()
        val start = starts.first { it.stopId != ends[1].stopId }

        row(start.stopId).performClick()
        composeRule.waitForIdle()
        row(start.stopId).assertIsSelected()
        assertEquals(IntroStep.ORIGIN, viewModel.state.value.step)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(start.stopId, viewModel.state.value.startStopId)
        assertEquals(IntroStep.CONFIRM, viewModel.state.value.step)
    }
}
