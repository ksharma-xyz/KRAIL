package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

data class SavedTripsState(
    val savedTrips: ImmutableList<Trip> = persistentListOf(),
    val isSavedTripsLoading: Boolean = true,
    val observeParkRideStopIdSet: ImmutableSet<String> = persistentSetOf(),
    val parkRideUiState: ParkRideStopsUiState? = null,
)
