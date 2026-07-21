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
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What a selected station shows, what it costs, and what survives a configuration
 * change. Split from [AddParkRideViewModelTest] so neither file becomes a wall of
 * unrelated setup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddParkRideSelectionTest {

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
        )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a station shows its cached availability without an API call`() =
        runTest(testDispatcher) {
            facilityManager.facilities = TALLAWONG
            // Cached row already stored, and its timestamp is now — so the facility is on
            // cooldown and must be served from cache rather than refetched.
            parkRideSandook.insertOrReplaceAll(
                listOf(cachedDetail(facilityId = "26", spotsAvailable = 42)),
            )
            parkRideSandook.updateApiCallTimestamp("26", Clock.System.now().epochSeconds)

            viewModel.uiState.test {
                advanceUntilIdle()
                val station = expectMostRecentItem().stations.first()

                viewModel.onEvent(AddParkRideUiEvent.StationSelected(station))
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(station.stationId, state.selectedStation?.stationId)
                assertEquals(42, state.selectedStationDetails.first().spotsAvailable)
                assertFalse(state.isLoadingSelectedStation)

                // Dismissing clears the sheet so a stale station is never shown next time.
                viewModel.onEvent(AddParkRideUiEvent.StationDismissed)
                advanceUntilIdle()
                val dismissed = expectMostRecentItem()
                assertEquals(null, dismissed.selectedStation)
                assertTrue(dismissed.selectedStationDetails.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `directions hand off the station's coordinates to the platform`() =
        runTest(testDispatcher) {
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
                val station = expectMostRecentItem().stations.first()

                viewModel.onEvent(
                    AddParkRideUiEvent.DirectionsClicked(
                        position = station.position!!,
                        stationName = station.stationName,
                    ),
                )
                advanceUntilIdle()

                // Coordinates go to the platform's own launcher, not a hardcoded provider URL,
                // so the device's default maps app handles it.
                assertEquals(
                    FakePlatformOps.MapDirections(TALLAWONG_LAT, TALLAWONG_LON, "Tallawong"),
                    platformOps.lastMapDirections,
                )
                assertEquals(null, platformOps.lastOpenedUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a typed query and its filtered results survive a config change`() =
        runTest(testDispatcher) {
            facilityManager.facilities = TALLAWONG +
                NswParkRideFacility("221210", "9", "Park&Ride - Revesby")

            // First collection: the composition before rotation.
            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onEvent(AddParkRideUiEvent.SearchQueryChanged("tal"))
                advanceUntilIdle()

                val before = expectMostRecentItem()
                assertEquals("tal", before.query)
                assertEquals(listOf("Tallawong"), before.visibleStations.map { it.stationName })
                cancelAndIgnoreRemainingEvents()
            }

            // Rotation tears the composition down and rebuilds it, so uiState is dropped and
            // re-collected. The rider must come back to exactly what they were looking at,
            // not an unfiltered list.
            viewModel.uiState.test {
                advanceUntilIdle()

                val after = expectMostRecentItem()
                assertEquals("tal", after.query)
                assertEquals(listOf("Tallawong"), after.visibleStations.map { it.stationName })
                assertEquals(listOf("T"), after.sections.map { it.letter })
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
