package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

data class SavedTripsState(val savedTrips: ImmutableList<Trip> = persistentListOf())
