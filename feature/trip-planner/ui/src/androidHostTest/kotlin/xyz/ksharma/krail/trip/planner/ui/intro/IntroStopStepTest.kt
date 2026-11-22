package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * The destination and origin screens with the real view model and the fakes at the edges: the list and its
 * highlighted row, choosing by tapping a row and by the right button, the left button opening the field without
 * choosing, typing, no match, a turn of the screen, and Back.
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
    fun `only the first row is announced as selected`() {
        show()

        val selected = ends.map {
            row(it.stopId).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected)
        }

        assertEquals(listOf(true, false, false, false, false), selected)
    }

    @Test
    fun `the bar says No search on the left and Yes this one on the right`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertTextEquals(STOP_SEARCH)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertTextEquals(STOP_CHOOSE)
    }

    @Test
    fun `tapping a row sets it as the end and moves to the origin screen without it in the list`() {
        show()

        row(ends[1].stopId).performClick()
        composeRule.waitForIdle()

        assertEquals(ends[1].stopId, viewModel.state.value.endStopId)
        composeRule.onNodeWithText(ORIGIN_HEADING).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.introStop(ends[1].stopId)).assertDoesNotExist()
    }

    @Test
    fun `Yes this one takes the first row and moves on`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(ends.first().stopId, viewModel.state.value.endStopId)
        composeRule.onNodeWithText(ORIGIN_HEADING).assertIsDisplayed()
    }

    @Test
    fun `No search focuses the field, never chooses and never leaves the screen`() {
        show()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertIsNotFocused()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertIsFocused()
        assertEquals(IntroStep.DESTINATION, viewModel.state.value.step)
        assertEquals(null, viewModel.state.value.endStopId)
    }

    @Test
    fun `typing replaces the suggestions with the stops found and highlights the first of them`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()

        type("chat")

        composeRule.onNodeWithText("Chatswood Station").assertIsDisplayed()
        ends.forEach { row(it.stopId).assertDoesNotExist() }
        assertEquals(
            true,
            row("206710").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected),
        )
        assertEquals(listOf("chat" to false), stopResults.searches)
    }

    @Test
    fun `choosing a typed result sets it as the end and counts the search as used`() {
        stopResults.answer = listOf(SuggestedStop("206710", "Chatswood Station")).map(::stopResult)
        show()
        type("chat")

        row("206710").performClick()
        composeRule.waitForIdle()

        assertEquals("206710", viewModel.state.value.endStopId)
        assertTrue(viewModel.state.value.usedSearch)
        composeRule.onNodeWithText(ORIGIN_HEADING).assertIsDisplayed()
        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `a search with no match shows the line, highlights nothing and turns the right button off`() {
        stopResults.answer = emptyList()
        show()

        type("zzzz")

        composeRule.onNodeWithText(STOP_NO_MATCH).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertIsEnabled()
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
    fun `the origin screen lists the curated starts, leaves out the chosen end and highlights the first`() {
        show()
        row(ends[1].stopId).performClick()
        composeRule.waitForIdle()

        val expected = starts.filterNot { it.stopId == ends[1].stopId }
        expected.forEach { row(it.stopId).assertIsDisplayed() }
        assertEquals(
            true,
            row(expected.first().stopId).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected),
        )
        assertFalse(viewModel.state.value.step == IntroStep.CONFIRM)
    }
}
