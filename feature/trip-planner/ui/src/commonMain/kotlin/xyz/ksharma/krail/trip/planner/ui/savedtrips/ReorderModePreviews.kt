package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

// Reorder mode on the real home screen, drawn in light and dark at normal and 200 percent text,
// the 320 dp frame included. The first trip is the card the long press lifted.

private const val PREVIEW_STATION_ID = "2153478"
private const val PREVIEW_STATION_NAME = "Bella Vista"
private const val PREVIEW_ROW_AFTER_TRIPS = 2

private val PREVIEW_TRIPS = listOf(
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
    Trip(fromStopId = "200080", fromStopName = "Wynyard Station", toStopId = "200090", toStopName = "Redfern Station"),
)

@PreviewScreen
@Composable
private fun PreviewReorderModeTripLifted() = ReorderPreview(liftedKey = PREVIEW_TRIPS.first().tripId)

@PreviewScreen
@Composable
private fun PreviewReorderModeRowLifted() = ReorderPreview(liftedKey = PARK_RIDE_ROW_KEY)

@PreviewScreen
@Composable
private fun PreviewReorderModeNoStation() = ReorderPreview(liftedKey = PREVIEW_TRIPS.first().tripId, hasStation = false)

@Composable
private fun ReorderPreview(liftedKey: String, hasStation: Boolean = true) {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                savedTrips = PREVIEW_TRIPS.toImmutableList(),
                parkRideUiState = if (hasStation) {
                    persistentListOf(
                        ParkRideUiState(
                            stopId = PREVIEW_STATION_ID,
                            stopName = PREVIEW_STATION_NAME,
                            facilities = persistentSetOf(
                                ParkRideUiState.ParkRideFacilityDetail(
                                    spotsAvailable = 120,
                                    totalSpots = 774,
                                    facilityName = "Bella Vista Car Park",
                                    percentageFull = 84,
                                    stopId = PREVIEW_STATION_ID,
                                    timeText = "10:00 AM",
                                    facilityId = "31",
                                ),
                            ),
                        ),
                    )
                } else {
                    persistentListOf()
                },
                reorder = ReorderSession.start(
                    tripIds = PREVIEW_TRIPS.map { it.tripId },
                    rowIndex = if (hasStation) PREVIEW_ROW_AFTER_TRIPS else null,
                    lifted = liftedKey,
                ),
            ),
            onEvent = {},
        )
    }
}
