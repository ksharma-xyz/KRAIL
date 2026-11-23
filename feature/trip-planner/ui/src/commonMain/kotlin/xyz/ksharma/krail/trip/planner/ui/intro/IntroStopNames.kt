package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

internal fun SuggestedStop.toIntroStop() = IntroStop(stopId = stopId, name = name)

/**
 * The station a stop belongs to: its name up to the first comma, so `Chatswood Station, Stand A` and
 * `Chatswood Station` are one station. Collapsing by name and not by the stops table's parent flag, which the
 * data does not set reliably, is an operator ruling.
 */
internal fun stationNameOf(stopName: String): String = stopName.substringBefore(',').trim()

// A name compared the way a rider reads it: no case, no doubled or trailing spaces.
private fun String.nameKey(): String = split(' ').filter { it.isNotEmpty() }.joinToString(" ").lowercase()

private fun IntroStop.stationKey(): String = stationNameOf(name).nameKey()

/**
 * Whether two stops are the same place for the lists: the same id, or the same station name. A trip never starts
 * and ends at one place, and a list never shows one station twice.
 */
internal fun IntroStop.isSameStopAs(other: IntroStop): Boolean =
    stopId == other.stopId || stationKey() == other.stationKey()

/**
 * One row for each station, in the order the stops arrive (nearest first). The row of a station is the stop that
 * is named exactly as the station (the station level stop), and when none is, the nearest stop of that station.
 * The rows keep the id and the name they came with.
 */
internal fun List<IntroStop>.collapsedByStationName(): List<IntroStop> =
    groupBy { it.stationKey() }.values.map { group ->
        group.firstOrNull { it.name.nameKey() == it.stationKey() } ?: group.first()
    }

/** The rows without any stop that is the same place as [other]. [other] null leaves the rows as they are. */
internal fun List<IntroStop>.without(other: IntroStop?): List<IntroStop> =
    if (other == null) this else filterNot { it.isSameStopAs(other) }

internal fun List<SuggestedStop>.withoutStop(other: IntroStop?): List<SuggestedStop> =
    if (other == null) this else filterNot { it.toIntroStop().isSameStopAs(other) }
