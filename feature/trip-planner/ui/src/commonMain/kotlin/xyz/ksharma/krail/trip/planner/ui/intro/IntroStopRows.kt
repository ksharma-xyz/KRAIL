package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

internal fun IntroState.startStop(): IntroStop? = startStopId?.let { id -> startName?.let { IntroStop(id, it) } }

internal fun IntroState.endStop(): IntroStop? = endStopId?.let { id -> endName?.let { IntroStop(id, it) } }

/** Whether this screen asks the rider to pick a stop from a list. */
internal fun IntroStep.isStopScreen(): Boolean = this == IntroStep.DESTINATION || this == IntroStep.ORIGIN

/**
 * The rows the destination or origin screen shows, top to bottom.
 *
 * - Typed results replace the suggestions, and no match is no rows. The stop already chosen for the other end is
 *   never listed, so a trip never starts and ends at one place.
 * - Otherwise it is the suggestions for the screen, without that stop. The rider's earlier choice for this
 *   screen, when they have come back to it, leads the list so it is the highlighted row, even when it was found
 *   by search and is not one of the suggestions.
 * - Other screens have no rows.
 */
internal fun IntroState.stopRows(suggested: SuggestedStops): List<IntroStop> {
    if (!step.isStopScreen()) return emptyList()
    val isOrigin = step == IntroStep.ORIGIN
    val other = if (isOrigin) endStop() else startStop()
    val own = if (isOrigin) startStop() else endStop()

    return when (val found = search) {
        is IntroSearchPhase.Results -> found.stops.without(other)
        IntroSearchPhase.NoMatch -> emptyList()
        IntroSearchPhase.Idle -> {
            val list = if (isOrigin) {
                fromSuggestions(nearbyStops.without(other), suggested.start.withoutStop(other))
            } else {
                destinationSuggestions(suggested.end.withoutStop(other))
            }
            if (own == null) list else (listOf(own) + list.without(own)).take(INTRO_STOP_LIST_SIZE)
        }
    }
}

/** The row the right button takes: the first one listed, or null when there is none. */
internal fun IntroState.highlightedStop(suggested: SuggestedStops): IntroStop? = stopRows(suggested).firstOrNull()

/**
 * The state after the rider picked [stop] on the destination or origin screen, which is a tap on a row and the
 * right button alike: it is the end or the start, the typed text and results are dropped, `usedSearch` is set
 * when [stop] is one of the typed results, and the Intro moves on to the next screen of the flow. The flow is
 * worked out after the pick, so picking the stop a Park & Ride start was filled in with brings the origin screen
 * back, and a pick that left no complete trip goes to the screen of the missing stop. On any other screen it changes
 * nothing.
 */
internal fun IntroState.withStopChosen(stop: IntroStop): IntroState {
    if (!step.isStopScreen()) return this
    val fromSearch = (search as? IntroSearchPhase.Results)?.stops?.any { it.stopId == stop.stopId } == true
    return withPick(isStart = step == IntroStep.ORIGIN, stopId = stop.stopId, stopName = stop.name)
        .copy(usedSearch = usedSearch || fromSearch, query = "", search = IntroSearchPhase.Idle)
        .movedOn()
        .backToMissingStop()
}

// The confirm screen needs both stops. A pick that cleared the other end, because it was that very stop, which the lists
// never offer, goes back to the screen that asks for the stop that is missing instead of on to a trip with one end.
private fun IntroState.backToMissingStop(): IntroState =
    if (step != IntroStep.CONFIRM || hasTrip) {
        this
    } else {
        copy(step = if (endStopId == null) IntroStep.DESTINATION else IntroStep.ORIGIN)
    }
