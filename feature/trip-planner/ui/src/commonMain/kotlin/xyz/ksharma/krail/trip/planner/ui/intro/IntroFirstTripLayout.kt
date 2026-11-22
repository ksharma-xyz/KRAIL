package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState

internal const val FIRST_TRIP_HEADING = "Save your first trip"
internal const val FIRST_TRIP_FROM = "From"
internal const val FIRST_TRIP_TO = "To"
internal const val SEARCH_ANOTHER_STOP = "Search another stop"

/** The end of the trip a stop is for. */
internal enum class IntroStopRole { START, END }

/** One block of step 2, top to bottom. */
internal sealed interface IntroTripSection {
    /** Both stops are chosen and neither is being changed: the saved trip tile, tap to change. */
    data class Tile(val startName: String, val startId: String, val endName: String, val endId: String) :
        IntroTripSection

    /** A stop already chosen, shown as one collapsed row, tap to change. */
    data class Chosen(val role: IntroStopRole, val name: String) : IntroTripSection

    /**
     * The suggestions for [role] and Search another stop. [selectedStopId] is the stop chosen for this
     * role, when the rider is changing it, and [otherStopId] is the stop chosen for the other role,
     * which is not offered again because a trip from a stop to itself is not a trip.
     */
    data class Picking(val role: IntroStopRole, val selectedStopId: String?, val otherStopId: String?) :
        IntroTripSection

    val key: String
        get() = when (this) {
            is Tile -> "tile"
            is Chosen -> "chosen-${role.name}"
            is Picking -> "picking-${role.name}"
        }
}

/**
 * What step 2 shows for [state], top to bottom.
 *
 * - Nothing chosen: the start suggestions only. The end list waits for a start, so the rider is asked
 *   one question at a time.
 * - A start and no end, which includes a start Add to home filled in: the start as a collapsed row,
 *   then the end suggestions.
 * - Both chosen: one tile.
 * - [changing], which a tap on the tile or on a collapsed row sets: both lists open with the current
 *   picks marked, so either end can be changed from one place.
 *
 * [changing] means nothing until something is chosen, so a stale flag never shows a list the rider
 * did not ask for.
 */
internal fun introTripSections(state: IntroState, changing: Boolean): List<IntroTripSection> {
    val trip = state.toTripOrNull()
    if (trip != null && !changing) {
        return listOf(
            IntroTripSection.Tile(
                startName = trip.fromStopName,
                startId = trip.fromStopId,
                endName = trip.toStopName,
                endId = trip.toStopId,
            ),
        )
    }
    val startId = state.startStopId
    val startName = state.startName
    val endId = state.endStopId
    val endName = state.endName
    val editing = changing && (startId != null || endId != null)

    val start = if (startId != null && startName != null && !editing) {
        IntroTripSection.Chosen(IntroStopRole.START, startName)
    } else {
        IntroTripSection.Picking(IntroStopRole.START, selectedStopId = startId, otherStopId = endId)
    }
    val end = when {
        endId != null && endName != null && !editing -> IntroTripSection.Chosen(IntroStopRole.END, endName)
        startId != null || endId != null -> {
            IntroTripSection.Picking(IntroStopRole.END, selectedStopId = endId, otherStopId = startId)
        }
        else -> null
    }
    return listOfNotNull(start, end)
}

/** The suggestions for [role], without the stop already chosen for the other role. */
internal fun SuggestedStops.offeredFor(role: IntroStopRole, otherStopId: String?): List<SuggestedStop> {
    val list = if (role == IntroStopRole.START) start else end
    return list.filterNot { it.stopId == otherStopId }
}

/** The event for a suggestion the rider chose. */
internal fun pickEvent(role: IntroStopRole, stop: SuggestedStop): IntroUiEvent =
    if (role == IntroStopRole.START) IntroUiEvent.PickStart(stop) else IntroUiEvent.PickEnd(stop)

internal fun IntroStopRole.sectionLabel(): String =
    if (this == IntroStopRole.START) FIRST_TRIP_FROM else FIRST_TRIP_TO

// What a screen reader says. Each one contains the words on screen, so what is heard matches what is read.

internal fun changeStopDescription(role: IntroStopRole, name: String): String =
    "Change ${role.roleWord()} stop, now $name"

internal fun changeTripDescription(startName: String, endName: String): String =
    "Change stops, now $startName to $endName"

internal fun pickStopDescription(role: IntroStopRole, name: String): String =
    "Pick $name as the ${role.roleWord()} stop"

internal fun searchStopDescription(role: IntroStopRole): String =
    "$SEARCH_ANOTHER_STOP for the ${role.roleWord()}"

private fun IntroStopRole.roleWord(): String = if (this == IntroStopRole.START) "start" else "end"
