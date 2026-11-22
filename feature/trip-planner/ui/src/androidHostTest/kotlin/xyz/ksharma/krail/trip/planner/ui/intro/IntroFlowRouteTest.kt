package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The route with the real view model, the fakes at the edges, and a fake location manager: a yes
 * ends in a card, a denial ends on the destination screen, a turn of the screen before the fix still ends in one
 * card, the finish effect reaches the callback, and each step reports its screen once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFlowRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }
    private val analytics = FakeAnalytics()
    private var savedTripsOpened = 0
    private val finale = IntroFinaleSignal()
    private val appliedThemes = mutableListOf<String>()

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

    private val nearBellaVista = Location(latitude = -33.771961, longitude = 150.944024, timestamp = 0L)

    private fun show(manager: FakeUserLocationManager) {
        restorationTester.setContent {
            PreviewTheme {
                IntroFlowRoute(
                    viewModel = viewModel,
                    onNavigateToSavedTrips = { savedTripsOpened++ },
                    onApplyTheme = { appliedThemes += it },
                    userLocationManager = manager,
                    finale = finale,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun screenViews() = analytics.getTrackedEvents("view_screen").map { it.properties?.get("name") }

    @Test
    fun `yes with a fix near a station ends in its card, and one fix was taken`() {
        val manager = FakeUserLocationManager(currentLocation = { Result.success(nearBellaVista) })
        show(manager)

        composeRule.onNodeWithText(PARK_RIDE_YES).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_ADD).assertIsDisplayed()
        assertEquals(1, manager.currentLocationCalls)
        assertEquals(LocationResult.GRANTED, viewModel.state.value.locationResult)
        assertIs<ParkRideCardPhase.Card>(viewModel.state.value.parkRideCard)
    }

    @Test
    fun `a denied permission moves on to the trip screen and the card is not left locating`() {
        val manager = FakeUserLocationManager(currentLocation = { Result.failure(LocationError.PermissionDenied()) })
        show(manager)

        composeRule.onNodeWithText(PARK_RIDE_YES).performClick()
        composeRule.waitForIdle()

        assertEquals(LocationResult.DENIED, viewModel.state.value.locationResult)
        assertEquals(IntroStep.DESTINATION, viewModel.state.value.step)
        assertEquals(ParkRideCardPhase.None, viewModel.state.value.parkRideCard)
        composeRule.onNodeWithText(DESTINATION_HEADING).assertIsDisplayed()
    }

    @Test
    fun `a turn of the screen while the fix is awaited still ends in one card`() {
        val gate = CompletableDeferred<Result<Location>>()
        val manager = FakeUserLocationManager(currentLocation = { gate.await() })
        show(manager)
        composeRule.onNodeWithText(PARK_RIDE_YES).performClick()
        composeRule.waitForIdle()
        assertEquals(ParkRideCardPhase.Locating(), viewModel.state.value.parkRideCard)

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        composeRule.runOnIdle { gate.complete(Result.success(nearBellaVista)) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bella Vista").assertIsDisplayed()
        assertIs<ParkRideCardPhase.Card>(viewModel.state.value.parkRideCard)
    }

    @Test
    fun `the finish effect reaches the navigation callback once`() {
        show(FakeUserLocationManager())
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()

        // The destination and the origin take the highlighted row each, and the confirm screen saves.
        repeat(3) {
            composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
            composeRule.waitForIdle()
        }

        assertEquals(1, savedTripsOpened)
    }

    @Test
    fun `with a finale host the finish waits for the finale to ask, then opens Saved Trips once and says so`() {
        finale.attachHost()
        show(FakeUserLocationManager())
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()
        repeat(3) {
            composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
            composeRule.waitForIdle()
        }

        assertEquals(0, savedTripsOpened, "Saved Trips was opened before the finale covered the screen")
        val run = assertNotNull(finale.request.value)
        assertEquals(DEFAULT_THEME_STYLE.hexColorCode, run.themeHexColorCode, "no theme was chosen on this route")
        assertTrue(!finale.navigated.value)

        composeRule.runOnIdle { finale.requestNavigate() }
        composeRule.waitForIdle()

        assertEquals(1, savedTripsOpened)
        assertTrue(finale.navigated.value, "the finale was not told the Intro had navigated")
    }

    @Test
    fun `each step reports its screen once, and a turn of the screen reports nothing`() {
        show(FakeUserLocationManager())
        assertEquals(listOf<Any?>("Intro"), screenViews())

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertEquals(listOf<Any?>("Intro"), screenViews())

        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf<Any?>("Intro", "IntroFirstTrip"), screenViews())

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertEquals(listOf<Any?>("Intro", "IntroFirstTrip"), screenViews())
    }
}
