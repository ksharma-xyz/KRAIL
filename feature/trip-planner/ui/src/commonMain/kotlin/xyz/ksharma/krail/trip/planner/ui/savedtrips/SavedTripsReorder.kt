package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderResult
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * The parts of reorder mode that are not the reducer, `docs/MAP_HOME_UX.md` rule 31. Kept out of
 * [SavedTripsViewModel] so that class stays under detekt's large class limit.
 */

/**
 * Begins a session over what home shows now: the saved trips in their order and the Park & Ride
 * row where it sits, or no row when no station is saved. Null when there is nothing to reorder.
 */
internal fun SavedTripsState.beginReorder(liftedKey: String): ReorderSession? {
    val hasRow = parkRideUiState.any { it.facilities.isNotEmpty() }
    if (savedTrips.isEmpty() && !hasRow) return null
    return ReorderSession.start(
        tripIds = savedTrips.map { it.tripId },
        rowIndex = if (hasRow) rowIndexFor(savedTrips.size, parkRideRowIndex) else null,
        lifted = liftedKey,
    )
}

/**
 * The state the moment Done is tapped: the session gone, the order and the removals applied and the
 * row's place kept, so home draws the finished order at once and not the old one until the database
 * answers. The database's own answer, which is the same order, follows.
 */
internal fun SavedTripsState.withReorderApplied(result: ReorderResult): SavedTripsState {
    val byId = savedTrips.associateBy { it.tripId }
    return copy(
        reorder = null,
        savedTrips = result.tripOrder.mapNotNull { byId[it] }.toImmutableList(),
        parkRideRowIndex = result.rowIndex ?: parkRideRowIndex,
    )
}

/**
 * Done's write: the new order and the trips taken off in one database transaction, and the row's
 * place in one preference. Nothing is written for a part that did not change, so a Done with
 * nothing moved writes nothing at all. Analytics reuse the two events the old edit mode sent: a
 * delete per trip taken off, and a reorder per trip whose place changed.
 */
internal fun persistReorder(
    sandook: Sandook,
    preferences: SandookPreferences,
    analytics: Analytics,
    session: ReorderSession,
    before: List<Trip>,
) {
    val result = session.result()
    val order = result.tripOrder
    if (session.removed.isNotEmpty() || order != before.map { it.tripId }.filter { it in order }) {
        sandook.reorderSavedTrips(orderedTripIds = order, removedTripIds = result.removed)
    }
    val rowIndex = result.rowIndex
    val rowIndexAtStart = session.startKeys.indexOf(PARK_RIDE_ROW_KEY).takeIf { it >= 0 }
    if (rowIndex != null && rowIndex != rowIndexAtStart) {
        preferences.setLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX, rowIndex.toLong())
    }
    analytics.trackReorderResult(before = before, result = result)
}

private fun Analytics.trackReorderResult(before: List<Trip>, result: ReorderResult) {
    val byId = before.associateBy { it.tripId }
    before.filter { it.tripId in result.removed }.forEach { trackDeleteSavedTrip(it.fromStopId, it.toStopId) }
    val kept = before.filter { it.tripId !in result.removed }
    result.tripOrder.forEachIndexed { newIndex, tripId ->
        val trip = byId[tripId] ?: return@forEachIndexed
        val previousIndex = kept.indexOfFirst { it.tripId == tripId }
        if (previousIndex != newIndex) {
            track(
                AnalyticsEvent.SavedTripCardReorderedEvent(
                    fromStopId = trip.fromStopId,
                    toStopId = trip.toStopId,
                    previousIndex = previousIndex,
                    newIndex = newIndex,
                    totalCount = result.tripOrder.size,
                ),
            )
        }
    }
}
