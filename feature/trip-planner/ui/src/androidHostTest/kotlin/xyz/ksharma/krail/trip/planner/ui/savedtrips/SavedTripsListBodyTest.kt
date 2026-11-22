package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.assertTrue

/**
 * Compose UI tests for [savedTripsListBody], `docs/MAP_HOME_UX.md` rule 29: two trips, then the one
 * Park & Ride row, then the rest. The row follows the only trip, comes first with no trips, and is
 * absent with no saved station. There are no headings and no card in the row's place.
 *
 * Card contents, reordering and Park & Ride expansion and polling are out of scope here and covered
 * by [ParkRideRowLayoutTest], [SavedTripsParkRideRestoreTest] and the ViewModel tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SavedTripsListBodyTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region where the row sits

    @Test
    fun `with no trips the row comes first`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 0, stations = 1)) }

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW).assertIsDisplayed()
    }

    @Test
    fun `with one trip the row follows it`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 1, stations = 1)) }

        assertBelow(row(), card(0))
    }

    @Test
    fun `with two trips the row follows both`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 2, stations = 1)) }

        assertBelow(row(), card(1))
    }

    @Test
    fun `with four trips the row sits after the second and before the third`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 4, stations = 1)) }

        assertBelow(row(), card(1))
        assertBelow(card(2), row())
    }

    // endregion

    // region nothing in its place

    @Test
    fun `with no saved station there is no row and no card in its place`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 2, stations = 0)) }

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW).assertDoesNotExist()
        composeRule.onNodeWithText("Add Park & Ride").assertDoesNotExist()
        composeRule.onNodeWithText("Add another").assertDoesNotExist()
        composeRule.onNodeWithText(OLD_EMPTY_TIP).assertDoesNotExist()
    }

    @Test
    fun `no trips and no station is an empty stack with no heading`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 0, stations = 0)) }

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW).assertDoesNotExist()
        composeRule.onNodeWithText("Saved Trips").assertDoesNotExist()
        composeRule.onNodeWithText("Park & Ride").assertDoesNotExist()
    }

    @Test
    fun `the stack has no Saved Trips or Park and Ride heading`() {
        composeRule.setContent { ListBody(state = stateWith(trips = 2, stations = 1)) }

        composeRule.onNodeWithText("Saved Trips").assertDoesNotExist()
        composeRule.onNodeWithText("Park & Ride").assertDoesNotExist()
    }

    @Test
    fun `loading renders nothing`() {
        composeRule.setContent { ListBody(state = SavedTripsState(isSavedTripsLoading = true)) }

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW).assertDoesNotExist()
    }

    // endregion

    private fun row() = composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW)
        .getUnclippedBoundsInRoot()

    private fun card(index: Int) = composeRule
        .onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS[index].tripId))
        .getUnclippedBoundsInRoot()

    private fun assertBelow(lower: androidx.compose.ui.unit.DpRect, upper: androidx.compose.ui.unit.DpRect) {
        assertTrue(lower.top >= upper.bottom, "$lower should sit below $upper")
    }

    @Composable
    private fun ListBody(state: SavedTripsState) {
        PreviewTheme {
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { _, _ -> }
            val expandedMap: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }

            LazyColumn(state = lazyListState) {
                savedTripsListBody(
                    savedTripsState = state,
                    trackedJourney = null,
                    onEvent = {},
                    onSavedTripCardClick = { _, _ -> },
                    onTrackingCardClick = {},
                    onStopTracking = {},
                    expandedMap = expandedMap,
                    editing = false,
                    reorderState = reorderState,
                    onEnterEditing = {},
                )
            }
        }
    }

    private fun stateWith(trips: Int, stations: Int) = SavedTripsState(
        isSavedTripsLoading = false,
        hasSeenReorderTip = true,
        savedTrips = TRIPS.take(trips).let { persistentListOf(*it.toTypedArray()) },
        parkRideUiState = STATIONS.take(stations).let { persistentListOf(*it.toTypedArray()) },
    )

    private companion object {
        const val OLD_EMPTY_TIP = "Save a trip and its station's parking shows here automatically."

        val TRIPS = listOf(
            Trip(
                fromStopId = "200060",
                fromStopName = "Central Station",
                toStopId = "200070",
                toStopName = "Town Hall Station",
            ),
            Trip(
                fromStopId = "207710",
                fromStopName = "Parramatta Station",
                toStopId = "206710",
                toStopName = "Chatswood Station",
            ),
            Trip(
                fromStopId = "200080",
                fromStopName = "Wynyard Station",
                toStopId = "200090",
                toStopName = "Redfern Station",
            ),
            Trip(
                fromStopId = "200100",
                fromStopName = "Strathfield Station",
                toStopId = "200110",
                toStopName = "Lidcombe Station",
            ),
        )

        val STATIONS = listOf(
            ParkRideUiState(
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
            ),
        )
    }
}
