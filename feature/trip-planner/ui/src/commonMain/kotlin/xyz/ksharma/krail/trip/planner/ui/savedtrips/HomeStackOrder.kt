package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession

/**
 * Where the Park & Ride row sits among the saved trips on home, `docs/MAP_HOME_UX.md` rule 29: two
 * trips, then the row, then the rest. With one trip the row follows it, and with none it comes
 * first. Until a rider drags it somewhere else the row is after the second trip.
 */
internal const val DEFAULT_PARK_RIDE_ROW_AFTER_TRIPS = 2

/**
 * How many trips come before the Park & Ride row, with [tripCount] trips saved.
 *
 * [storedIndex] is the place the rider dragged the row to, null while they have not. It is clamped
 * to the trips there are, so deleting trips never leaves the row past the end of them, and a
 * negative value from a bad write counts as first. The default is also clamped, so a rider with
 * one trip gets the row after it and a rider with none gets the row first.
 */
internal fun rowIndexFor(tripCount: Int, storedIndex: Int? = null): Int =
    (storedIndex ?: DEFAULT_PARK_RIDE_ROW_AFTER_TRIPS).coerceIn(0, tripCount.coerceAtLeast(0))

/**
 * The keys of the home stack in the order they are drawn: the saved trips' ids and, when a station
 * is saved, [PARK_RIDE_ROW_KEY] where the row sits (rule 29).
 *
 * Outside reorder mode that is the stored place clamped by [rowIndexFor]. During a reorder it is the
 * session's working order, so a drag shows at once and a cancel has nothing to undo: the stack goes
 * back to the saved order and the list animates the cards home. A trip the session has taken off is
 * not in its keys, and a key that is no longer on home (a station removed mid edit) is dropped.
 */
internal fun homeStackKeys(
    tripIds: List<String>,
    hasRow: Boolean,
    storedIndex: Int?,
    session: ReorderSession?,
): List<String> {
    if (session != null) {
        val known = tripIds.toSet()
        return session.keys.filter { key -> if (key == PARK_RIDE_ROW_KEY) hasRow else key in known }
    }
    val rowIndex = if (hasRow) rowIndexFor(tripIds.size, storedIndex) else null
    return ReorderSession.start(tripIds = tripIds, rowIndex = rowIndex).keys
}

/** The key of the one-time Park & Ride suggestion card among the trips' ids, in the row's place. */
internal const val PARK_RIDE_SUGGESTION_KEY = "park_ride_suggestion"

/**
 * The home stack's keys with the suggestion card in the Park & Ride row's place, or just [keys] when it is not shown.
 *
 * The card takes the place the row would, [storedIndex] if the rider dragged the row there and after the second trip
 * if not, clamped to the trips there are, so it follows the same rule 29 the row does. [keys] holds only trips, as
 * the suggestion is shown only while there is no row.
 */
internal fun withSuggestionKey(keys: List<String>, show: Boolean, storedIndex: Int?): List<String> {
    if (!show) return keys
    val at = rowIndexFor(tripCount = keys.size, storedIndex = storedIndex)
    return keys.take(at) + PARK_RIDE_SUGGESTION_KEY + keys.drop(at)
}
