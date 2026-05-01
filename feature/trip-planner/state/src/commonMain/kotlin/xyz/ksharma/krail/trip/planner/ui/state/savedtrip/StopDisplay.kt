package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Display model for "a stop, possibly labelled".
 *
 * Crosses the VM ↔ UI boundary so the UI never needs to see the raw labels list
 * for display purposes — the VM resolves once per emission and passes a
 * [StopDisplay] per stop. Each surface composes [name] and [label] as its design
 * dictates (label-primary on timetable, name-primary on recents, etc.).
 *
 * Pure data. No `primary`/`secondary` getters; encoding either UX policy on the
 * model breaks the surfaces that want the opposite ordering.
 */
@Stable
data class StopDisplay(
    val stopId: String,
    val name: String,
    val label: String? = null,
)

/**
 * Returns the user's label for [stopId] when an `isSet` [StopLabel] entry maps
 * to it, otherwise null. First match wins on duplicates (data-corruption case).
 */
internal fun List<StopLabel>.findLabelFor(stopId: String): String? =
    firstOrNull { it.isSet && it.stopId == stopId }?.label

/** Convert a [StopItem] to a [StopDisplay], picking up its label if any. */
fun StopItem.toDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(
        stopId = stopId,
        name = stopName,
        label = labels.findLabelFor(stopId),
    )

/** A [Trip]'s "from" stop as a [StopDisplay]. */
fun Trip.fromStopDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(
        stopId = fromStopId,
        name = fromStopName,
        label = labels.findLabelFor(fromStopId),
    )

/** A [Trip]'s "to" stop as a [StopDisplay]. */
fun Trip.toStopDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(
        stopId = toStopId,
        name = toStopName,
        label = labels.findLabelFor(toStopId),
    )
