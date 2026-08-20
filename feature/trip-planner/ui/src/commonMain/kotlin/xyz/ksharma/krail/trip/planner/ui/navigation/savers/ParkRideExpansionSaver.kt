package xyz.ksharma.krail.trip.planner.ui.navigation.savers

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * Saves which Park & Ride cards are open, as the list of their stop ids.
 *
 * Only the open ones are worth keeping: a card that is not in the list is closed, which is
 * also what an empty map means, so the two agree without storing a `false` per card.
 *
 * This matters for more than the rider's scroll position. The expanded set is also what the
 * ViewModel refreshes, and the two halves are held in different places — the map here, the
 * stop ids in `SavedTripsState.observeParkRideStopIdSet`, which survives recreation because
 * the ViewModel does. Losing only this half leaves every card drawn closed over a ViewModel
 * still refreshing them, and the next tap on one reads as *opening* it, so it can never be
 * taken back out of the refresh set.
 */
fun parkRideExpansionSaver(): Saver<SnapshotStateMap<String, Boolean>, List<String>> = Saver(
    save = { expansionByStopId ->
        expansionByStopId.filterValues { isExpanded -> isExpanded }.keys.toList()
    },
    restore = { openStopIds ->
        mutableStateMapOf<String, Boolean>().apply {
            openStopIds.forEach { stopId -> put(stopId, true) }
        }
    },
)
