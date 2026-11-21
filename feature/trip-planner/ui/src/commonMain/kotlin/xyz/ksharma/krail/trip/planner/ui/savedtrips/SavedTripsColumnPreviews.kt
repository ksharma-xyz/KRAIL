package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

// The saved trips column (docs/MAP_HOME_UX.md rule 45) on three canvases. This is the phone. The
// 320 dp and wide previews sit in the `timetable.narrow` and `timetable.wide` packages, which the
// narrow and wide snapshot tests scan, and draw the same state.

/** Trips, a Park & Ride station and the search row, so every part that shares the column's edges is on screen. */
internal val previewSavedTripsColumnState = SavedTripsState(
    isSavedTripsLoading = false,
    isDiscoverAvailable = true,
    savedTrips = persistentListOf(
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
    ),
    parkRideUiState = persistentListOf(
        ParkRideUiState(
            stopId = "2153478",
            stopName = "Bella Vista",
            isLoading = false,
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
    ),
    fromStop = StopItem(stopId = "200060", stopName = "Central Station"),
    toStop = StopItem(stopId = "200070", stopName = "Town Hall Station"),
)

/** A phone: the column is the whole window, with the 16 dp page padding either side. */
@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewSavedTripsColumnPhone() {
    PreviewTheme { SavedTripsScreen(savedTripsState = previewSavedTripsColumnState) }
}
