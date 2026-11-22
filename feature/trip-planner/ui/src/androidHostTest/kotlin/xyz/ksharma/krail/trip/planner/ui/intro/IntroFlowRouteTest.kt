package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.krail.core.navigation.ResultEventBus
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.TimeSource

/**
 * The route with the real view model, the fakes at the edges, and a fake location manager: a yes
 * ends in a card, a denial ends on step 2, a turn of the screen before the fix still ends in one
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
    private val searchesOpened = mutableListOf<SearchStopFieldType>()

    private val viewModel = IntroFlowViewModel(
        savedStateHandle = SavedStateHandle(),
        appDataReadiness = FakeAppDataReadiness(initial = StopsReadiness.Ready(stopCount = 100)),
        completion = IntroCompletion(FakeSandook(), FakeSandookPreferences(), analytics, Dispatchers.Unconfined),
        flag = FakeFlag(),
        timeSource = TimeSource.Monotonic,
        parkRide = IntroParkRideGateway(
            catalogue = FakeParkRideCatalogue(stations = listOf(BELLA_VISTA_STATION)),
            availabilityLoader = FakeParkRideAvailabilityLoader(),
            parkRideSandook = FakeNswParkRideSandook(),
            ioDispatcher = Dispatchers.Unconfined,
            stopNameLookup = { null },
            analytics = analytics,
        ),
    )

    private val nearBellaVista = Location(latitude = -33.771961, longitude = 150.944024, timestamp = 0L)

    private fun show(manager: FakeUserLocationManager) {
        restorationTester.setContent {
            PreviewTheme {
                IntroFlowRoute(
                    viewModel = viewModel,
                    onNavigateToSavedTrips = { savedTripsOpened++ },
                    onNavigateToSearchStop = { searchesOpened += it },
                    userLocationManager = manager,
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
        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertIsDisplayed()
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

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(1, savedTripsOpened)
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

    @After
    fun leaveTheBusClean() {
        ResultEventBus.getInstance().removeResult<StopSelectedResult>()
    }

    private fun sendPick(fieldType: SearchStopFieldType, id: String, name: String) {
        ResultEventBus.getInstance()
            .sendResult(result = StopSelectedResult(fieldType = fieldType, stopId = id, stopName = name))
        composeRule.waitForIdle()
    }

    @Test
    fun `Search another stop opens the search for the start, then for the end`() {
        show(FakeUserLocationManager())
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START)).performClick()
        assertEquals(listOf(SearchStopFieldType.FROM), searchesOpened)

        sendPick(SearchStopFieldType.FROM, id = "215020", name = "Parramatta Station")
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.END)).performClick()
        assertEquals(listOf(SearchStopFieldType.FROM, SearchStopFieldType.TO), searchesOpened)
    }

    @Test
    fun `stops picked in the search become the trip on step 2, and the search counts as used`() {
        show(FakeUserLocationManager())
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()

        sendPick(SearchStopFieldType.FROM, id = "215020", name = "Parramatta Station")
        sendPick(SearchStopFieldType.TO, id = "200080", name = "Wynyard Station")

        val state = viewModel.state.value
        assertEquals("215020", state.startStopId)
        assertEquals("Parramatta Station", state.startName)
        assertEquals("200080", state.endStopId)
        assertEquals("Wynyard Station", state.endName)
        assertEquals(true, state.usedSearch)
        composeRule.onNodeWithContentDescription(
            changeTripDescription("Parramatta Station", "Wynyard Station"),
        ).assertIsDisplayed()
    }

    @Test
    fun `a stop picked in the search survives a turn of the screen and is not applied twice`() {
        show(FakeUserLocationManager())
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()
        sendPick(SearchStopFieldType.FROM, id = "215020", name = "Parramatta Station")

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals("215020", viewModel.state.value.startStopId)
        composeRule.onNodeWithContentDescription(
            changeStopDescription(IntroStopRole.START, "Parramatta Station"),
        ).assertIsDisplayed()
    }
}
