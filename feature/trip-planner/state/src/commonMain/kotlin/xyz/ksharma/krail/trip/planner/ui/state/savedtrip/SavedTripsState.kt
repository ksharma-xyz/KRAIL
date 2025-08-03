package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Stable
data class SavedTripsState(
    val savedTrips: ImmutableList<Trip> = persistentListOf(),
    val isSavedTripsLoading: Boolean = true,
    val observeParkRideStopIdSet: ImmutableSet<String> = persistentSetOf(),
    val parkRideUiState: ImmutableList<ParkRideUiState> = persistentListOf(),
    val isDiscoverAvailable: Boolean = false,
    val displayDiscoverBadge: Boolean = false,
)
