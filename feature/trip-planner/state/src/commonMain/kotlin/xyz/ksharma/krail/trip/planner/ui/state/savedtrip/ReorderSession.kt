package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/**
 * The list key of the Park & Ride row among the trips' ids, in a reorder session and in the home
 * list. A trip id is a stop id pair, so it can never equal this.
 */
const val PARK_RIDE_ROW_KEY = "park_ride_row"

/**
 * One reorder of the home stack, `docs/MAP_HOME_UX.md` rule 31: a working copy of the order the
 * rider is moving cards in. Nothing is stored while it is open. [result] is what Done writes, and
 * dropping the session is what cancel does, so cancel writes nothing by construction.
 *
 * Every function is pure and returns a new session, so the reducer is driven in a table test with no
 * ViewModel, database or layout.
 *
 * @property keys the working order: the saved trips' ids, and [PARK_RIDE_ROW_KEY] where the Park &
 * Ride row sits, when there is a row. The row is one entry and moves as a block.
 * @property startKeys the order when the session began.
 * @property removed trips the rider has taken off with their delete circle. They are staged here and
 * only deleted by Done, so cancel brings them back.
 * @property lifted the card the long press lifted, until the rider moves or removes something.
 */
@Stable
data class ReorderSession(
    val keys: ImmutableList<String>,
    val startKeys: ImmutableList<String>,
    val removed: ImmutableSet<String> = persistentSetOf(),
    val lifted: String? = null,
) {

    /** The trips in the working order, without the row. */
    val tripOrder: List<String> get() = keys.filter { it != PARK_RIDE_ROW_KEY }

    /** How many trips come before the row, or null when there is no row. */
    val rowIndex: Int?
        get() = keys.indexOf(PARK_RIDE_ROW_KEY).takeIf { it >= 0 }
            ?.let { at -> keys.take(at).size }

    /** Whether Done would change anything: an order that moved, or a trip taken off. */
    val hasChanges: Boolean get() = removed.isNotEmpty() || keys != startKeys

    /**
     * Moves the card with [fromKey] to where the card with [toKey] is, the way a drag past a
     * neighbour does. A key that is not in the session, or the same key twice, changes nothing.
     * Moving anything clears the lift.
     */
    fun move(fromKey: String, toKey: String): ReorderSession {
        val from = keys.indexOf(fromKey)
        val to = keys.indexOf(toKey)
        if (from < 0 || to < 0 || from == to) return this
        val moved = keys.toMutableList().apply { add(to, removeAt(from)) }
        return copy(keys = moved.toImmutableList(), lifted = null)
    }

    /** Takes a trip off. The row can never be removed, and a trip not in the session changes nothing. */
    fun remove(tripId: String): ReorderSession {
        if (tripId == PARK_RIDE_ROW_KEY || tripId !in keys) return this
        return copy(
            keys = keys.filter { it != tripId }.toImmutableList(),
            removed = (removed + tripId).toImmutableSet(),
            lifted = null,
        )
    }

    /** What Done writes: the trips' order, the trips taken off, and where the row sits. */
    fun result(): ReorderResult = ReorderResult(tripOrder = tripOrder, removed = removed, rowIndex = rowIndex)

    companion object {

        /**
         * Begins a session over [tripIds] in their saved order, with the row after [rowIndex] trips
         * when there is one (null for no row), and [lifted] as the card the long press picked up.
         * [rowIndex] is clamped to the trips there are.
         */
        fun start(tripIds: List<String>, rowIndex: Int?, lifted: String? = null): ReorderSession {
            val keys = if (rowIndex == null) {
                tripIds
            } else {
                val at = rowIndex.coerceIn(0, tripIds.size)
                tripIds.take(at) + PARK_RIDE_ROW_KEY + tripIds.drop(at)
            }
            return ReorderSession(
                keys = keys.toImmutableList(),
                startKeys = keys.toImmutableList(),
                lifted = lifted?.takeIf { it in keys },
            )
        }
    }
}

/** What Done writes: [tripOrder] first to last, the [removed] trips, and the [rowIndex] if there is a row. */
data class ReorderResult(
    val tripOrder: List<String>,
    val removed: Set<String>,
    val rowIndex: Int?,
)
