package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.saveable.Saver
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The trip the To and From form holds while the rider is still on Search Stop, rules 35, 36 and 39 of
// `docs/MAP_HOME_UX.md`. A pick fills its bar and the screen stays; only the go circle leaves, with both
// stops. Every move is a pure function, so one table pins the focus move and the go circle.

/** The stops picked so far. A bar the rider has not picked for is null, and an auto filled start is not here. */
internal data class SearchStopTrip(val from: StopItem? = null, val to: StopItem? = null) {

    /** The bars holding a stop, counting a start that filled itself in ([autoFrom]) as a filled From. */
    fun filled(autoFrom: StopItem? = null): Set<ToFromField> = buildSet {
        if (to != null) add(ToFromField.To)
        if (from != null || autoFrom != null) add(ToFromField.From)
    }

    /** The stop in [field], or null. */
    fun stopIn(field: ToFromField): StopItem? = if (field == ToFromField.To) to else from

    fun with(field: ToFromField, stop: StopItem): SearchStopTrip =
        if (field == ToFromField.To) copy(to = stop) else copy(from = stop)
}

/** The bar a pick fills when nothing is being edited: the destination. */
internal fun pickedBarFor(editing: ToFromField?): ToFromField = editing ?: ToFromField.To

internal fun ToFromField.other(): ToFromField =
    if (this == ToFromField.To) ToFromField.From else ToFromField.To

/** The next trip and flow after a pick. */
internal data class TripPick(val trip: SearchStopTrip, val flow: SearchStopFlow)

/**
 * A pick on the To and From form. The stop fills the bar that was being edited ([field], the
 * destination when none was), and the screen stays. Editing then moves to the other bar if it is
 * empty, with the keyboard following when it was up, and to nothing when both bars hold a stop. With
 * nothing to edit there is no keyboard, so the screen goes to the map state and shows the go circle.
 *
 * A start that filled itself in ([autoFrom]) counts as a filled From, so a destination pick with a
 * stop nearby leaves both bars set and the rider only has to go.
 */
internal fun reduceTripPick(
    trip: SearchStopTrip,
    flow: SearchStopFlow,
    field: ToFromField,
    stop: StopItem,
    autoFrom: StopItem?,
): TripPick {
    val next = trip.with(field, stop)
    val editing = nextEditingAfter(field, next.filled(autoFrom))
    val mode = if (editing == null) SearchStopMode.Map else flow.mode
    return TripPick(trip = next, flow = flow.copy(mode = mode, editing = editing))
}

/**
 * The swap: From becomes To and To becomes From. A start that filled itself in ([autoFrom]) counts as the From the rider
 * sees, so it moves to To like any other. Whatever bar is left empty by it is the one edited next (To first), the typed text
 * that searched for the old bar is the screen's to clear, and with both bars held there is nothing to edit, so the screen is
 * the map state with no keyboard, as after any pick that fills the last bar.
 *
 * The swap is offered only once the rider has picked a stop ([swapShown]), so with only an auto start there is nothing to move.
 */
internal fun reduceTripSwap(trip: SearchStopTrip, flow: SearchStopFlow, autoFrom: StopItem?): TripPick {
    val swapped = SearchStopTrip(from = trip.to, to = trip.from ?: autoFrom)
    val filled = swapped.filled(autoFrom)
    val editing = listOf(ToFromField.To, ToFromField.From).firstOrNull { it !in filled }
    val mode = if (editing == null) SearchStopMode.Map else flow.mode
    return TripPick(trip = swapped, flow = flow.copy(mode = mode, editing = editing))
}

/** Whether the swap is drawn: the rider has picked at least one stop. An auto filled start alone is nothing to swap. */
internal fun swapShown(trip: SearchStopTrip): Boolean = trip.from != null || trip.to != null

/** The bar editing moves to once [picked] is filled: the other bar if it is empty, else nothing. */
internal fun nextEditingAfter(picked: ToFromField, filled: Set<ToFromField>): ToFromField? =
    picked.other().takeIf { it !in filled }

/**
 * A start that arrived after the screen opened. While the map state shows the From bar as the one
 * being edited with nothing typed, and a destination is already held, the nearest stop filling in
 * means both bars are set, so editing drops to nothing and the go circle shows.
 */
internal fun settleLateAutoFrom(
    flow: SearchStopFlow,
    trip: SearchStopTrip,
    autoFrom: StopItem?,
): SearchStopFlow = if (
    autoFrom != null &&
    trip.from == null &&
    trip.to != null &&
    flow.editing == ToFromField.From &&
    flow.mode == SearchStopMode.Map
) {
    flow.copy(editing = null)
} else {
    flow
}

/**
 * The trip the go circle leaves with, or null while it cannot start: both stops are needed, and a
 * trip from a stop to itself is not a trip. The start is the one the rider picked, else the one that
 * filled itself in.
 */
internal fun tripToGo(trip: SearchStopTrip, autoFrom: StopItem?): Pair<StopItem, StopItem>? {
    val from = trip.from ?: autoFrom
    val to = trip.to
    return if (from != null && to != null && from.stopId != to.stopId) from to to else null
}

private const val STOP_FIELD_COUNT = 4

/**
 * Saves the picked stops across rotation, theme and font changes as eight strings, four per stop (id,
 * name, kind and address type), with an empty id for a bar that holds none.
 */
internal val SearchStopTripSaver: Saver<SearchStopTrip, List<String>> = Saver(
    save = { trip -> encodeStop(trip.from) + encodeStop(trip.to) },
    restore = { saved ->
        SearchStopTrip(
            from = decodeStop(saved.take(STOP_FIELD_COUNT)),
            to = decodeStop(saved.drop(STOP_FIELD_COUNT)),
        )
    },
)

private fun encodeStop(stop: StopItem?): List<String> =
    if (stop == null) {
        listOf("", "", "", "")
    } else {
        listOf(stop.stopId, stop.stopName, stop.locationKind.name, stop.addressType.orEmpty())
    }

private fun decodeStop(parts: List<String>): StopItem? {
    val id = parts.getOrNull(0).orEmpty()
    if (id.isEmpty()) return null
    return StopItem(
        stopId = id,
        stopName = parts.getOrElse(1) { "" },
        locationKind = LocationKind.entries.firstOrNull { it.name == parts.getOrNull(2) }
            ?: LocationKind.TRANSIT_STOP,
        addressType = parts.getOrNull(3)?.takeIf { it.isNotEmpty() },
    )
}
