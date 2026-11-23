package xyz.ksharma.krail.trip.planner.ui.components

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture

// The decisions of the departure board that need no layout, so each is pinned by a plain test.

/** The departures of [line] alone, or all of [departures] when no line is chosen. */
internal fun departuresOnLine(departures: ImmutableList<StopDeparture>, line: String?): ImmutableList<StopDeparture> =
    if (line == null) departures else departures.filter { it.lineNumber == line }.toImmutableList()

/**
 * Whether opening the previous departures asks for them: only when the panel was just opened ([showPrevious]), none have been
 * fetched ([hasPrevious] false) and a fetch is not already running ([previousLoading]).
 */
internal fun previousDeparturesWanted(showPrevious: Boolean, hasPrevious: Boolean, previousLoading: Boolean): Boolean =
    showPrevious && !hasPrevious && !previousLoading
