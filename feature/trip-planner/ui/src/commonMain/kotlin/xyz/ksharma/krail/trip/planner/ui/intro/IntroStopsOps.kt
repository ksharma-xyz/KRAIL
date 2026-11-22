package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

private fun IntroState.withModesOf(stops: List<ListedStop>): IntroState =
    withModes(stops.associate { it.stop.stopId to it.modes })

/** The modes of listed stops added to the ones known, for the row badges. Nothing is removed. */
internal fun IntroState.withModes(modes: Map<String, List<TransportMode>>): IntroState =
    if (modes.isEmpty()) {
        this
    } else {
        copy(stopModes = (stopModes + modes.mapValues { it.value.toImmutableList() }).toImmutableMap())
    }

/**
 * The stations near the rider, once they are found. They are the origin list's lead, so a rider who is already on the
 * origin screen, or past it, keeps the list they have, and the rows never change under a finger. After that screen
 * nothing reads them. The stops are kept as ids and names, with their modes known for the badges.
 */
internal fun IntroState.withNearby(found: List<ListedStop>): IntroState =
    if (step.ordinal >= IntroStep.ORIGIN.ordinal) {
        this
    } else {
        copy(nearbyStops = found.map { it.stop }.toImmutableList()).withModesOf(found)
    }

/** The text in the search field. Blank text is no search, so the suggestions show again at once. */
internal fun IntroState.withQuery(text: String): IntroState =
    copy(query = text, search = if (text.isBlank()) IntroSearchPhase.Idle else search)

/**
 * What the search found for [text], when [text] is still the query: an answer that arrives after the rider typed on
 * is dropped. No answer at all (the search failed) leaves the suggestions showing, and an answer with no stop is the
 * no match line.
 */
internal fun IntroState.withSearchResult(text: String, found: List<ListedStop>?): IntroState = when {
    text != query || query.isBlank() -> this
    found == null -> copy(search = IntroSearchPhase.Idle)
    found.isEmpty() -> copy(search = IntroSearchPhase.NoMatch)
    else -> copy(search = IntroSearchPhase.Results(found.map { it.stop }.toImmutableList())).withModesOf(found)
}

/** The state with the typed text and what was found for it dropped, as it is when the rider leaves a stop screen. */
internal fun IntroState.withoutSearch(): IntroState = copy(query = "", search = IntroSearchPhase.Idle)
