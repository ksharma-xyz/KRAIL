package xyz.ksharma.krail.trip.planner.ui.parkride

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ErrorKind
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddParkRideViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val parkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private val facilityManager = FakeParkRideFacilityManager()
    private val sandook = FakeSandook()
    private val analytics: Analytics = FakeAnalytics()
    private val platformOps = FakePlatformOps()
    private lateinit var viewModel: AddParkRideViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddParkRideViewModel(
            catalogue = RealParkRideCatalogue(
                nswParkRideFacilityManager = facilityManager,
                stopResultsManager = FakeStopResultsManager(),
                sandook = sandook,
                festivalManager = FakeFestivalManager(),
            ),
            parkRideSandook = parkRideSandook,
            availabilityLoader = RealParkRideAvailabilityLoader(
                parkRideSandook = parkRideSandook,
                parkRideService = FakeParkRideService(),
                flag = FakeFlag(),
            ),
            platformOps = platformOps,
            analytics = analytics,
            ioDispatcher = testDispatcher,
            appReviewManager = FakeAppReviewManager(),
        )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `one stop with several car parks is a single station`() = runTest(testDispatcher) {
        // Tallawong: three car parks behind one stop ID.
        facilityManager.facilities = TALLAWONG

        viewModel.uiState.test {
            advanceUntilIdle()
            val stations = expectMostRecentItem().stations

            assertEquals(1, stations.size)
            assertEquals("Tallawong", stations.first().stationName)
            assertEquals(3, stations.first().carParkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `one car park behind several stops is a single station`() = runTest(testDispatcher) {
        // Sutherland: one facility reachable from three stop IDs.
        facilityManager.facilities = listOf(
            NswParkRideFacility("223210", "15", "Park&Ride - Sutherland"),
            NswParkRideFacility("2232126", "15", "Park&Ride - Sutherland"),
            NswParkRideFacility("2232254", "15", "Park&Ride - Sutherland"),
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val stations = expectMostRecentItem().stations

            assertEquals(1, stations.size)
            assertEquals("Sutherland", stations.first().stationName)
            assertEquals(1, stations.first().carParkCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding a station stores every one of its car parks`() = runTest(testDispatcher) {
        facilityManager.facilities = TALLAWONG

        viewModel.uiState.test {
            advanceUntilIdle()
            val station = expectMostRecentItem().stations.first()

            viewModel.onEvent(AddParkRideUiEvent.ToggleStation(station))
            advanceUntilIdle()

            val stored = parkRideSandook.observeSavedParkRidesBySource(UserAdded).first()
            assertEquals(setOf("26", "27", "28"), stored.map { it.facilityId }.toSet())
            assertTrue(expectMostRecentItem().stations.first().added)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a station held by a saved trip reads as added and cannot be removed here`() = runTest(testDispatcher) {
        facilityManager.facilities = TALLAWONG
        // The same station is also reached by a saved trip. Removing the rider's own entry
        // must not take the saved-trip row with it.
        parkRideSandook.insertOrReplaceSavedParkRides(
            parkRideInfoList = setOf(
                SavedParkRide(
                    stopId = "2155384",
                    facilityId = "26",
                    stopName = "Tallawong Station",
                    facilityName = "Tallawong P1",
                    source = SavedTrips.value,
                ),
            ),
            source = SavedTrips,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val station = expectMostRecentItem().stations.first()
            // Shown as added because its card IS on the home screen, but not the rider's to
            // remove from here.
            assertTrue(station.added)
            assertTrue(station.isLockedBySavedTrip)
            assertFalse(station.isUserAdded)

            // Tapping a saved-trip-held station is a no-op: the trip owns the card.
            advanceUntilIdle()
            assertTrue(parkRideSandook.observeSavedParkRidesBySource(UserAdded).first().isEmpty())
            assertEquals(1, parkRideSandook.observeSavedParkRidesBySource(SavedTrips).first().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty catalogue surfaces a retryable error`() = runTest(testDispatcher) {
        facilityManager.facilities = emptyList()

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(ErrorKind.NoFacilities, state.error)
            assertFalse(state.isLoading)

            // Retrying once Remote Config resolves clears the error.
            facilityManager.facilities = TALLAWONG
            viewModel.onEvent(AddParkRideUiEvent.Retry)
            advanceUntilIdle()

            val retried = expectMostRecentItem()
            assertEquals(null, retried.error)
            assertEquals(1, retried.stations.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stations carry local GTFS coordinates for the map pane`() = runTest(testDispatcher) {
        facilityManager.facilities = TALLAWONG
        sandook.insertNswStop(
            stopId = "2155384",
            stopName = "Tallawong Station",
            stopLat = TALLAWONG_LAT,
            stopLon = TALLAWONG_LON,
            isParent = null,
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val position = expectMostRecentItem().stations.first().position

            // Coordinates come from the local stops table, so the map can plot without any
            // availability fetch.
            assertEquals(TALLAWONG_LAT, position?.latitude)
            assertEquals(TALLAWONG_LON, position?.longitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a station missing from the local stops table simply has no position`() =
        runTest(testDispatcher) {
            facilityManager.facilities = TALLAWONG

            viewModel.uiState.test {
                advanceUntilIdle()
                val station = expectMostRecentItem().stations.first()

                // Not plotted, but still listed and addable.
                assertEquals(null, station.position)
                assertEquals("Tallawong", station.stationName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stations are grouped into alphabetical sections that follow the search`() =
        runTest(testDispatcher) {
            facilityManager.facilities = listOf(
                NswParkRideFacility("1", "1", "Park&Ride - Ashfield"),
                NswParkRideFacility("2", "2", "Park&Ride - Revesby"),
                NswParkRideFacility("3", "3", "Park&Ride - Riverwood"),
            ) + TALLAWONG

            viewModel.uiState.test {
                advanceUntilIdle()
                val sections = expectMostRecentItem().sections

                assertEquals(listOf("A", "R", "T"), sections.map { it.letter })
                // Both R stations land in one section, in name order.
                assertEquals(
                    listOf("Revesby", "Riverwood"),
                    sections.first { it.letter == "R" }.stations.map { it.stationName },
                )

                // Searching narrows the headers too, rather than leaving empty ones behind.
                viewModel.onEvent(AddParkRideUiEvent.SearchQueryChanged("river"))
                advanceUntilIdle()
                assertEquals(listOf("R"), expectMostRecentItem().sections.map { it.letter })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search matches station name and car park name`() = runTest(testDispatcher) {
        facilityManager.facilities = TALLAWONG + NswParkRideFacility("221210", "9", "Park&Ride - Revesby")

        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onEvent(AddParkRideUiEvent.SearchQueryChanged("tallawong"))
            advanceUntilIdle()
            assertEquals(listOf("Tallawong"), expectMostRecentItem().visibleStations.map { it.stationName })

            // "P2" only appears on a car park name, not the station name.
            viewModel.onEvent(AddParkRideUiEvent.SearchQueryChanged("P2"))
            advanceUntilIdle()
            assertEquals(listOf("Tallawong"), expectMostRecentItem().visibleStations.map { it.stationName })

            viewModel.onEvent(AddParkRideUiEvent.SearchQueryChanged("nothing here"))
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().visibleStations.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun cachedDetail(facilityId: String, spotsAvailable: Int) = NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = spotsAvailable.toLong(),
        totalSpots = 100,
        facilityName = "Tallawong P1",
        percentageFull = 58,
        stopId = "2155384",
        stopName = "Tallawong Station",
        timeText = "8:00 AM",
        suburb = "Rouse Hill",
        address = "",
        latitude = 0.0,
        longitude = 0.0,
        timestamp = Clock.System.now().epochSeconds,
    )

    private companion object {
        const val TALLAWONG_LAT = -33.6919
        const val TALLAWONG_LON = 150.9059

        val TALLAWONG = listOf(
            NswParkRideFacility("2155384", "26", "Park&Ride - Tallawong P1"),
            NswParkRideFacility("2155384", "27", "Park&Ride - Tallawong P2"),
            NswParkRideFacility("2155384", "28", "Park&Ride - Tallawong P3"),
        )
    }
}
