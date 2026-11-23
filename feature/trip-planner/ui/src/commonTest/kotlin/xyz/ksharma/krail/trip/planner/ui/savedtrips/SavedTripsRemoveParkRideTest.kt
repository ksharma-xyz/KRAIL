package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeInfoTileManager
import xyz.ksharma.krail.core.testing.fakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Remove from home on a saved station's card on the map (`docs/MAP_HOME_UX.md` rule 38): it removes the car parks
 * the rider added at that station, and only those. A row a saved trip holds stays, as on the picker.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsRemoveParkRideTest {

    private val parkRideSandook = FakeNswParkRideSandook()

    private suspend fun save(stopId: String, facilityId: String, source: SavedParkRideSource) {
        parkRideSandook.insertOrReplaceSavedParkRides(
            parkRideInfoList = setOf(
                SavedParkRide(
                    stopId = stopId,
                    facilityId = facilityId,
                    stopName = "Station $stopId",
                    facilityName = "Car park $facilityId",
                    source = source.value,
                ),
            ),
            source = source,
        )
    }

    private fun station(stopId: String, vararg facilityIds: String) = ParkRideUiState(
        stopId = stopId,
        stopName = "Station $stopId",
        facilities = persistentSetOf(
            *facilityIds.map { facilityId ->
                ParkRideFacilityDetail(
                    spotsAvailable = -1,
                    totalSpots = -1,
                    facilityName = "Car park $facilityId",
                    percentageFull = -1,
                    stopId = stopId,
                    timeText = "",
                    facilityId = facilityId,
                )
            }.toTypedArray(),
        ),
    )

    private suspend fun remaining(): List<Pair<String, String>> =
        parkRideSandook.observeSavedParkRides().first().map { it.stopId to it.facilityId }

    @Test
    fun `removing a station removes every car park the rider added there`() = krailRunTest {
        save(STATION, P1, UserAdded)
        save(STATION, P2, UserAdded)
        val viewModel = buildViewModel()
        val screen = openHome(viewModel)

        viewModel.onEvent(SavedTripUiEvent.RemoveParkRideStation(station(STATION, P1, P2)))
        runCurrent()

        assertEquals(emptyList(), remaining())
        leaveHome(screen)
    }

    @Test
    fun `another station is left alone`() = krailRunTest {
        save(STATION, P1, UserAdded)
        save(OTHER_STATION, OTHER, UserAdded)
        val viewModel = buildViewModel()
        val screen = openHome(viewModel)

        viewModel.onEvent(SavedTripUiEvent.RemoveParkRideStation(station(STATION, P1)))
        runCurrent()

        assertEquals(listOf(OTHER_STATION to OTHER), remaining())
        leaveHome(screen)
    }

    @Test
    fun `a car park a saved trip holds is not removed`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = openHome(viewModel)
        // After open, which drops saved trip rows that no saved trip backs.
        save(STATION, P1, SavedTrips)

        viewModel.onEvent(SavedTripUiEvent.RemoveParkRideStation(station(STATION, P1)))
        runCurrent()

        assertEquals(listOf(STATION to P1), remaining())
        leaveHome(screen)
    }

    @Test
    fun `removing a station with no rows is not an error`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = openHome(viewModel)

        viewModel.onEvent(SavedTripUiEvent.RemoveParkRideStation(station(STATION, P1)))
        runCurrent()

        assertEquals(emptyList(), remaining())
        leaveHome(screen)
    }

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.openHome(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    private fun KrailTestScope.leaveHome(screen: Job) {
        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
    }

    private fun KrailTestScope.buildViewModel() = SavedTripsViewModel(
        sandook = FakeSandook(),
        analytics = FakeAnalytics(),
        ioDispatcher = ioDispatcher,
        nswParkRideFacilityManager = FakeParkRideFacilityManager(),
        parkRideService = FakeParkRideService(),
        parkRideSandook = parkRideSandook,
        stopResultsManager = FakeStopResultsManager(),
        searchSessionStore = RealSearchSessionStore(),
        flag = FakeFlag(),
        preferences = FakeSandookPreferences(),
        infoTileManager = FakeInfoTileManager(),
        platformOps = FakePlatformOps(),
        inviteFriendsTileManager = FakeInviteFriendsTileManager(),
        trackingManager = TrackingManager(),
        appReviewManager = FakeAppReviewManager(),
        nearbyStopsRepository = FakeNearbyStopsRepository(),
    )

    private companion object {
        const val STATION = "2155384"
        const val OTHER_STATION = "2153478"
        const val P1 = "26"
        const val P2 = "27"
        const val OTHER = "31"
        const val SCREEN_SETTLE_MS = 1_000L
        const val BEAT_MS = 500L
        const val SUBSCRIPTION_GRACE_MS = 5_000L
    }
}
