package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceMatrixScope
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapActions
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapScreenForTest
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * The saved trips home, the classic one and the map home, on every device profile (`docs/testing/DEVICE_PROFILES.md`), with the
 * cards being reordered as well as at rest. The gear, Discover, the trip cards, the Done button and the bottom search sheet
 * must stand inside the safe area on every device, and the list must scroll clear of the bottom bar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SavedTripsDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private fun savedTripsState(reordering: Boolean = false) = SavedTripsState(
        isSavedTripsLoading = false,
        savedTrips = TRIPS.toImmutableList(),
        parkRideUiState = persistentListOf(STATION),
        stopLabels = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station"),
            StopLabel(emoji = "W", label = "Work"),
        ),
        reorder = if (reordering) ReorderSession.start(TRIPS.map { it.tripId }, rowIndex = 2) else null,
    )

    private fun DeviceMatrixScope.showSavedTrips() =
        setContent(theme = { BareKrailTheme(content = it) }) {
            SavedTripsScreen(savedTripsState = savedTripsState())
        }

    private fun DeviceMatrixScope.showMapHome(reordering: Boolean) =
        setContent(theme = { BareKrailTheme(content = it) }) {
            HomeMapScreenForTest(
                savedTripsState = savedTripsState(reordering),
                actions = HomeMapActions(),
            )
        }

    @Test
    fun `Saved Trips keeps the gear, the cards and the search sheet inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips()

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS.first().tripId)))
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST))
        }

    @Test
    fun `Saved Trips keeps Done and the cards inside the safe area while the cards are reordered`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips()
            onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS.first().tripId)).performTouchInput { longClick() }
            rule.waitForIdle()

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE))
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST))
        }

    @Test
    fun `the map home keeps its chrome, its cards and its search sheet inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "HomeMapScreen") {
            showMapHome(reordering = false)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS))
        }

    @Test
    fun `the map home keeps Done, the grips and the delete circles inside the safe area while reordering`() =
        matrix.forEachDeviceProfile(screen = "HomeMapScreen") {
            showMapHome(reordering = true)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE))
        }

    private companion object {
        val TRIPS = (1..SAVED_TRIP_COUNT).map { index ->
            Trip(
                fromStopId = "F$index",
                fromStopName = "From $index Station",
                toStopId = "T$index",
                toStopName = "To $index Station",
            )
        }

        val STATION = ParkRideUiState(
            stopId = "2153478",
            stopName = "Bella Vista",
            facilities = persistentSetOf(
                ParkRideUiState.ParkRideFacilityDetail(
                    spotsAvailable = 120,
                    totalSpots = 774,
                    facilityName = "Bella Vista Car Park",
                    percentageFull = 84,
                    stopId = "2153478",
                    timeText = "10:00 AM",
                    facilityId = "31",
                ),
            ),
        )
    }
}

private const val SAVED_TRIP_COUNT = 8
