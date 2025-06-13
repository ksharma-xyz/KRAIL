package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.data.buildCarParkFacilityDetailResponse
import xyz.ksharma.core.test.data.buildOccupancy
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeNswParkRideSandook
import xyz.ksharma.core.test.fakes.FakeParkRideFacilityManager
import xyz.ksharma.core.test.fakes.FakeParkRideService
import xyz.ksharma.core.test.fakes.FakeParkRideService.Companion.facilityResponses
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsViewModelTest {

    private val sandook: Sandook = FakeSandook()
    private val fakeNswParkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private val fakeAnalytics: Analytics = FakeAnalytics()
    private lateinit var viewModel: SavedTripsViewModel
    private val fakeParkRideManager: NswParkRideFacilityManager = FakeParkRideFacilityManager()

    private val fakeParkRideService: ParkRideService = FakeParkRideService()

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SavedTripsViewModel(
            sandook = sandook,
            analytics = fakeAnalytics,
            ioDispatcher = testDispatcher,
            nswParkRideFacilityManager = fakeParkRideManager,
            parkRideService = fakeParkRideService,
            parkRideSandook = fakeNswParkRideSandook,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN initial state WHEN observer is active THEN analytics event should be tracked`() =
        runTest {
            // Ensure analytics events have not been tracked before observation
            assertFalse((fakeAnalytics as FakeAnalytics).isEventTracked("view_screen"))

            viewModel.uiState.test {
                val item = awaitItem()
                assertEquals(item, SavedTripsState())

                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.SavedTrips.name,
                )

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN no observer is active WHEN checking analytics THEN event should not be tracked`() =
        runTest {
            // GIVEN no observer is active

            // WHEN checking analytics
            val eventTracked = (fakeAnalytics as FakeAnalytics).isEventTracked("view_screen")

            // THEN event should not be tracked
            assertFalse(eventTracked)
        }

    @Test
    fun `GIVEN a saved trip WHEN LoadSavedTrips event is triggered THEN UiState should update savedTrips`() =
        runTest {

            // GIVEN a saved trip
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_ID_1",
                toStopName = "STOP_NAME_2",
            )

            // Ensure initial state
            viewModel.uiState.test {
                skipItems(1)

                // WHEN the LoadSavedTrips event is triggered
                viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)

                val item = awaitItem()

                // THEN Verify that the state is updated after loading trips
                assertFalse(item.isSavedTripsLoading)
                assertTrue(item.savedTrips.isNotEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a saved trip WHEN DeleteSavedTrip event is triggered THEN the trip should be deleted and UiState should update`() =
        runTest {
            // GIVEN a saved trip
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_ID_1",
                toStopName = "STOP_NAME_2",
            )
            sandook.insertOrReplaceTrip(
                tripId = trip.tripId,
                fromStopId = trip.fromStopId,
                fromStopName = trip.fromStopName,
                toStopId = trip.toStopId,
                toStopName = trip.toStopName,
            )

            // Ensure initial state
            viewModel.uiState.test {
                skipItems(1)

                // WHEN the DeleteSavedTrip event is triggered
                viewModel.onEvent(
                    SavedTripUiEvent.DeleteSavedTrip(trip = trip)
                )

                // THEN verify that the trip is deleted and the state is updated
                val item = awaitItem()
                assertFalse(item.isSavedTripsLoading)
                assertTrue(item.savedTrips.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsSavedTripCardClick event is triggered THEN trackSavedTripCardClick should be called`() =
        runTest {
            // GIVEN a trip
            val fromStopId = "FROM_STOP_ID_1"
            val toStopId = "TO_STOP_ID_1"

            // WHEN the AnalyticsSavedTripCardClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsSavedTripCardClick(fromStopId, toStopId))

            // THEN verify that [SavedTripCardClickEvent] is called with correct parameters
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.SavedTripCardClickEvent(
                fromStopId = fromStopId,
                toStopId = toStopId
            ).name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsReverseSavedTrip event is triggered THEN track ReverseStopClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsReverseSavedTrip event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsReverseSavedTrip)

            // THEN verify that track is called with ReverseStopClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.ReverseStopClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsLoadTimeTableClick event is triggered THEN trackLoadTimeTableClick should be called`() =
        runTest {
            // GIVEN a trip
            val fromStopId = "FROM_STOP_ID_1"
            val toStopId = "TO_STOP_ID_1"

            // WHEN the AnalyticsLoadTimeTableClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsLoadTimeTableClick(fromStopId, toStopId))

            // THEN verify that trackLoadTimeTableClick is called with correct parameters
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.LoadTimeTableClickEvent(
                fromStopId = fromStopId,
                toStopId = toStopId
            ).name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsSettingsButtonClick event is triggered THEN track SettingsClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsSettingsButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)

            // THEN verify that track is called with SettingsClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.SettingsClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsFromButtonClick event is triggered THEN track FromFieldClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsFromButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)

            // THEN verify that track is called with FromFieldClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.FromFieldClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsToButtonClick event is triggered THEN track ToFieldClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsToButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)

            // THEN verify that track is called with ToFieldClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.ToFieldClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    // region Park&Ride Facilities Tests

    @Ignore // todo - fix when states added
    @Test
    fun `GIVEN a saved trip with ParkRide stop WHEN LoadParkRideFacilities event is triggered THEN ParkRideUiState should update to Loaded`() =
        runTest {
            // Arrange: Insert a saved trip with a stopId matching a Park&Ride facility
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "2153478", // matches mockFacilityResponses key
                fromStopName = "Bella Vista",
                toStopId = "2153471",
                toStopName = "XYZ station",
            )

            val fakeParkRideService = FakeParkRideService()
            val viewModel = SavedTripsViewModel(
                sandook = sandook,
                analytics = fakeAnalytics,
                ioDispatcher = testDispatcher,
                nswParkRideFacilityManager = fakeParkRideManager,
                parkRideService = fakeParkRideService,
                parkRideSandook = fakeNswParkRideSandook,
            )

            // Act: Load saved trips and trigger LoadParkRideFacilities event
            viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)
            advanceUntilIdle()
            viewModel.onEvent(
                SavedTripUiEvent.DisplayParkRideFacilitiesClick(
                    fromStopId = "2153478",
                    toStopId = "2153471"
                )
            )
            advanceUntilIdle()

            // Assert: The trip's parkRideUiState should be Loaded with correct ParkRideState
            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                val state = awaitItem()
                val trip = state.savedTrips.first()
                println( "Trip ParkRideUiState: ${trip.parkRideUiState}")
                assertTrue(trip.parkRideUiState is ParkRideUiState.Loaded)

                val loaded = trip.parkRideUiState as ParkRideUiState.Loaded
                assertEquals(1, loaded.parkRideList.size)

                val parkRideState = loaded.parkRideList.first()
                assertEquals(774, parkRideState.totalSpots)
                assertEquals(674, parkRideState.spotsAvailable)
                assertEquals("Park&Ride - Bella Vista", parkRideState.facilityName)
                assertEquals(12, parkRideState.percentageFull) // 100/774 ≈ 12%
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Ignore // todo - fix when error states added
    @Test
    fun `GIVEN ParkRideService throws WHEN LoadParkRideFacilities event is triggered THEN ParkRideUiState should update to Error`() =
        runTest {
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "2153478",
                fromStopName = "Bella Vista",
                toStopId = "2153471",
                toStopName = "XYZ station",
            )

            val errorService = object : ParkRideService {
                override suspend fun getCarParkFacilities(facilityId: String): CarParkFacilityDetailResponse {
                    throw RuntimeException("Network error")
                }

                override suspend fun getCarParkFacilities(): Map<String, String> {
                    throw RuntimeException("Network error")
                }
            }
            val viewModel = SavedTripsViewModel(
                sandook = sandook,
                analytics = fakeAnalytics,
                ioDispatcher = testDispatcher,
                nswParkRideFacilityManager = fakeParkRideManager,
                parkRideService = errorService,
                parkRideSandook = fakeNswParkRideSandook,
            )

            viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)
            advanceUntilIdle()
            viewModel.onEvent(
                SavedTripUiEvent.DisplayParkRideFacilitiesClick(
                    fromStopId = "2153478",
                    toStopId = "2153471"
                )
            )
            advanceUntilIdle()

            viewModel.uiState.test {
                skipItems(1)
                val state = awaitItem()
                val trip = state.savedTrips.first()
                assertTrue(trip.parkRideUiState is ParkRideUiState.Available)
                val error = trip.parkRideUiState as ParkRideUiState.Error
                assertTrue(error.message.contains("Network error"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a saved trip WHEN LoadParkRideFacilities event is triggered THEN ParkRideUiState should be Loading before result`() =
        runTest {
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "2153478",
                fromStopName = "Bella Vista",
                toStopId = "2153471",
                toStopName = "XYZ station",
            )

            val slowService = object : ParkRideService {
                override suspend fun getCarParkFacilities(facilityId: String): CarParkFacilityDetailResponse {
                    kotlinx.coroutines.delay(100)
                    return facilityResponses.values.first()
                }

                override suspend fun getCarParkFacilities(): Map<String, String> {
                    return mapOf()
                }
            }
            val viewModel = SavedTripsViewModel(
                sandook = sandook,
                analytics = fakeAnalytics,
                ioDispatcher = testDispatcher,
                nswParkRideFacilityManager = fakeParkRideManager,
                parkRideService = slowService,
                parkRideSandook = fakeNswParkRideSandook,
            )

            viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)
            advanceUntilIdle()
            viewModel.onEvent(
                SavedTripUiEvent.DisplayParkRideFacilitiesClick(
                    fromStopId = "2153478",
                    toStopId = "2153471"
                )
            )

            viewModel.uiState.test {
                skipItems(1) // Initial state

                val state = awaitItem()
                val trip = state.savedTrips.first()
                assertTrue(trip.parkRideUiState is ParkRideUiState.Available)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN CarParkFacilityDetailResponse with valid data WHEN toParkRideState is called THEN ParkRideState is correct`() {
        val facilityResponse = buildCarParkFacilityDetailResponse(
            facilityName = "Park&Ride - Bella Vista",
            spots = "774",
            occupancy = buildOccupancy(transients = "100")
        )

        val parkRideState = facilityResponse.toParkRideState()

        assertEquals(774, parkRideState.totalSpots)
        assertEquals(574, parkRideState.spotsAvailable)
        assertEquals(25, parkRideState.percentageFull) // 100/774 ≈ 12%
        assertEquals("Bella Vista", parkRideState.facilityName)
    }

    @Test
    fun `GIVEN saved trips with and without ParkRide stops WHEN loadSavedTrips is called THEN ParkRideUiState is set correctly`() =
        runTest {
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "2153478", // Park&Ride stop
                fromStopName = "Bella Vista",
                toStopId = "TO_ID_1",
                toStopName = "STOP_NAME_2",
            )
            sandook.insertOrReplaceTrip(
                tripId = "2",
                fromStopId = "NON_PARKRIDE_STOP",
                fromStopName = "No ParkRide",
                toStopId = "TO_ID_2",
                toStopName = "STOP_NAME_3",
            )

            viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)
            advanceUntilIdle()

            viewModel.uiState.test {
                skipItems(1)

                val state = awaitItem()
                val tripWithParkRide = state.savedTrips.first { it.fromStopId == "2153478" }
                val tripWithoutParkRide =
                    state.savedTrips.first { it.fromStopId == "NON_PARKRIDE_STOP" }

                assertTrue(tripWithParkRide.parkRideUiState is ParkRideUiState.Available)
                assertTrue(tripWithoutParkRide.parkRideUiState is ParkRideUiState.NotAvailable)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion
}
