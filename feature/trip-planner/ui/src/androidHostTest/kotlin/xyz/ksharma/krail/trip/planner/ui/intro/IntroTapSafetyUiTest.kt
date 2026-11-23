package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.TimeSource

/**
 * A quick second tap never moves the Intro on a step. The real route and view model, with the clock held so a tap can land
 * in the middle of the slide between two screens: the tap on the bar a moment after a first tap, and a tap on a row of the
 * screen that is still arriving, are taken by nothing, so the Intro lands once on the next screen and nothing is selected
 * there, and a tap on a row of the screen that is leaving does nothing. After the slide everything works.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroTapSafetyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val analytics = FakeAnalytics()

    private val viewModel = IntroFlowViewModel(
        savedStateHandle = savedAt(ON_PARK_RIDE),
        appDataReadiness = FakeAppDataReadiness(initial = StopsReadiness.Ready(stopCount = 100)),
        completion = IntroCompletion(FakeSandook(), FakeSandookPreferences(), analytics, Dispatchers.Unconfined),
        timeSource = TimeSource.Monotonic,
        parkRide = IntroParkRideGateway(
            catalogue = FakeParkRideCatalogue(stations = listOf(BELLA_VISTA_STATION)),
            availabilityLoader = FakeParkRideAvailabilityLoader(),
            parkRideSandook = FakeNswParkRideSandook(),
            ioDispatcher = Dispatchers.Unconfined,
            stopNameLookup = { null },
            analytics = analytics,
        ),
        stops = fakeStopsGateway(),
    )

    private fun show() {
        composeRule.setContent {
            PreviewTheme {
                IntroFlowRoute(
                    viewModel = viewModel,
                    onNavigateToSavedTrips = {},
                    onApplyTheme = {},
                    userLocationManager = FakeUserLocationManager(),
                    finale = IntroFinaleSignal(),
                )
            }
        }
        composeRule.waitForIdle()
    }

    // A touch on the place where the bar's primary button is drawn, whether or not it is there now: a finger does not know.
    private fun primaryPlace(): Offset = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        .fetchSemanticsNode().boundsInRoot.center

    private fun primaryRightPlace(): Offset = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        .fetchSemanticsNode().boundsInRoot.let { Offset(it.right - RIGHT_INSET_PX, it.center.y) }

    private fun tapAt(place: Offset) = composeRule.onRoot().performTouchInput { click(place) }

    private fun noPlace(): Offset = composeRule.onNodeWithText(PARK_RIDE_NO).fetchSemanticsNode().boundsInRoot.center

    private fun tapFirstRow() =
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIRST).performTouchInput { click(center) }

    @Test
    fun `a double tap on No lands on the destination screen once, and the second tap selects nothing there`() {
        show()
        val no = noPlace()
        composeRule.mainClock.autoAdvance = false

        tapAt(no)
        composeRule.mainClock.advanceTimeBy(48)
        // The second tap of the double tap: on the bar's place, and on the first row of the screen that is arriving.
        tapAt(no)
        composeRule.mainClock.advanceTimeBy(32)
        tapFirstRow()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(IntroStep.DESTINATION, viewModel.state.value.step, "the double tap skipped a screen")
        assertEquals(ParkRideAnswer.NO, viewModel.state.value.parkRideAnswer)
        assertNull(viewModel.state.value.selectedStop, "the second tap selected a row on the screen that was still arriving")
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()
    }

    @Test
    fun `once the slide has finished the same tap on a row is taken`() {
        show()
        tapAt(noPlace())
        composeRule.waitForIdle()

        tapFirstRow()
        composeRule.waitForIdle()

        assertNotNull(viewModel.state.value.selectedStop)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertExists()
    }

    @Test
    fun `a tap on a row of the screen that is leaving, or of the one arriving, changes nothing while the slide runs`() {
        show()
        tapAt(noPlace())
        composeRule.waitForIdle()
        tapFirstRow()
        composeRule.waitForIdle()
        val confirm = primaryPlace()
        composeRule.mainClock.autoAdvance = false

        // Confirm the destination, which slides the origin screen in, and tap a row of either screen part way through.
        tapAt(confirm)
        composeRule.mainClock.advanceTimeBy(48)
        assertEquals(IntroStep.ORIGIN, viewModel.state.value.step)
        composeRule.onAllRowsOnScreen().forEach { row -> row.performTouchInput { click(center) } }
        tapAt(confirm)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(IntroStep.ORIGIN, viewModel.state.value.step, "a tap during the slide moved the Intro on")
        assertNull(viewModel.state.value.selectedStop, "a tap during the slide selected a stop")
        assertNull(viewModel.state.value.startStopId)
    }

    @Test
    fun `the bar that changes in place takes no tap while the next screen slides in, so a second tap never answers it`() {
        show()
        tapAt(noPlace())
        composeRule.waitForIdle()
        tapFirstRow()
        composeRule.waitForIdle()
        tapAt(primaryPlace())
        composeRule.waitForIdle()
        assertEquals(IntroStep.ORIGIN, viewModel.state.value.step)
        tapFirstRow()
        composeRule.waitForIdle()
        // The one button of the origin screen is the whole width, and the right half of it is where Yes save it will be.
        val confirm = primaryRightPlace()
        composeRule.mainClock.autoAdvance = false

        tapAt(confirm)
        composeRule.mainClock.advanceTimeBy(48)
        assertEquals(IntroStep.CONFIRM, viewModel.state.value.step)
        // The bar is already Not now and Yes save it, drawn live, and the second tap lands on it.
        tapAt(confirm)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(IntroStep.CONFIRM, viewModel.state.value.step, "the second tap answered the confirm screen")
        assertNull(viewModel.state.value.tripDecision, "the second tap answered the question before the rider saw it")
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertExists()
    }

    private fun ComposeContentTestRule.onAllRowsOnScreen() =
        onAllNodes(hasTestTag(TripPlannerTestTags.INTRO_STOP_FIRST), useUnmergedTree = true).let { rows ->
            (0 until rows.fetchSemanticsNodes().size).map { rows[it] }
        }

    private companion object {
        const val RIGHT_INSET_PX = 40f
    }
}
