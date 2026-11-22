package xyz.ksharma.krail.trip.planner.ui.savedtrips

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
 * The trips that come before the Park & Ride row and the trips that come after it, in the order
 * the rider keeps them.
 */
internal fun <T> List<T>.splitAroundParkRideRow(rowIndex: Int): Pair<List<T>, List<T>> {
    val at = rowIndex.coerceIn(0, size)
    return take(at) to drop(at)
}
