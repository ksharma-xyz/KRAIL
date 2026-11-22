package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * What the floating cards offer for a stop, decided with no layout (`docs/MAP_HOME_UX.md`, Stop card,
 * and `docs/features/map-home/ERD.md`, the Go here cases). Every rule is a plain function so a table
 * test drives it at its boundary, and none reads a layout, a clock or a database.
 */

/**
 * What picking the stop on the card does to the trip. It is the field the rider is filling in: a
 * destination reads Go here, a start reads Start here, and a stop chosen for a label, which is not a
 * trip at all, keeps the plain Select stop.
 */
enum class PickRole {
    /** The stop becomes the end of the trip. */
    Destination,

    /** The stop becomes the start of the trip. */
    Start,

    /** The stop is chosen for a label. */
    Choose,
}

internal fun pickRoleFor(field: SearchStopFieldType): PickRole = when (field) {
    SearchStopFieldType.TO -> PickRole.Destination
    SearchStopFieldType.FROM -> PickRole.Start
    SearchStopFieldType.LABEL -> PickRole.Choose
}

/** The words of the one primary action, a verb or a place (`docs/MAP_HOME_UX.md`, Language). */
internal fun pickActionText(role: PickRole): String = when (role) {
    PickRole.Destination -> "Go here"
    PickRole.Start -> "Start here"
    PickRole.Choose -> "Select stop"
}

/**
 * A stop is offered whenever it has an id. Nothing here asks the stops table whether it knows the
 * stop, so a stop with no modes or no row of its own still offers its action: the trip planner takes
 * a stop id, and a card that hid the action for a stop it knew less about would promise less than it
 * can do.
 */
internal fun offersPick(stopId: String): Boolean = stopId.isNotBlank()

/** A label has no Save as while the rider is choosing a stop for a label, which is what Save as does. */
internal fun offersSaveAs(role: PickRole): Boolean = role != PickRole.Choose

/** One pill of the Save as row. */
internal sealed interface SaveAsPill {
    val label: StopLabel

    /** The label this stop already carries, filled and not tappable: solid means set here. */
    data class Saved(override val label: StopLabel) : SaveAsPill

    /** A label no stop carries yet, outlined: a tap gives it to this stop. */
    data class Offered(override val label: StopLabel, val text: String) : SaveAsPill
}

internal fun saveAsText(label: StopLabel): String = "Save as ${label.label}"

/**
 * The Save as pills for the stop [stopId], in the order the labels are stored (Home, Work, then the
 * rider's own, as Manage stop labels arranges them).
 *
 * A stop is on one label and a label is on one stop, the same rule Search Stop's assign row keeps. So a
 * stop that already carries a label shows that one pill filled and nothing else, the first in stored
 * order if the data ever holds two. A stop with none is offered every label that no stop carries, and a
 * label that sits on another stop is left out: a filled pill there would read as this stop's own, and
 * tapping it would be a reassignment the assign path never makes.
 */
internal fun saveAsPillsFor(stopId: String, role: PickRole, labels: List<StopLabel>): List<SaveAsPill> {
    if (!offersSaveAs(role) || stopId.isBlank()) return emptyList()
    val carried = labels.firstOrNull { it.isSet && it.stopId == stopId }
    return if (carried != null) {
        listOf(SaveAsPill.Saved(carried))
    } else {
        labels.filterNot { it.isSet }.map { SaveAsPill.Offered(label = it, text = saveAsText(it)) }
    }
}

/**
 * The room the Save as row holds, the tallest it has been. Tapping a pill turns a row of outlined pills
 * into one filled pill, which can be a line shorter, and the card must not shrink under the rider's
 * finger (rule 22), so the row never gives back what it has used.
 */
internal fun reservedRoom(reserved: Dp, measured: Dp): Dp = maxOf(reserved, measured)

/**
 * The stop Go here sets for a Park & Ride station: the smallest of the station's stop ids that the
 * stops table holds, compared as text exactly as the station's own id is (`groupIntoStations`), so the
 * answer never depends on the order the catalogue lists them in. Null when the table holds none of them:
 * a station with no stop of its own has nothing a trip could start from, so no Go here.
 */
internal fun goHereStopId(stopIds: Collection<String>, inStopsTable: Set<String>): String? =
    stopIds.filter { it in inStopsTable }.minOrNull()

/** The action that sets a stop on the trip. */
internal data class PickAction(val text: String, val stop: StopItem)

/**
 * Go here on the Park & Ride pin card (Start here in a start search), or null when the station has no stop
 * to start from. The card always offers Add to home or Remove from home besides. The stop is named as the
 * station, which is what the rider tapped, with the id the trip planner needs.
 */
internal fun parkRidePickActionFor(stationName: String, goHereStopId: String?, role: PickRole): PickAction? =
    goHereStopId?.takeIf(::offersPick)?.let {
        PickAction(text = pickActionText(role), stop = StopItem(stopId = it, stopName = stationName))
    }
