package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult

/** Which field of the app's stop search the Intro opens, for the start or for the end. */
internal fun introSearchFieldType(isStart: Boolean): SearchStopFieldType =
    if (isStart) SearchStopFieldType.FROM else SearchStopFieldType.TO

/**
 * A stop the rider picked in the app's own stop search, as the event for the Intro. The search sends
 * the same result whichever screen opened it, so this is the one place that decides it is for a trip
 * end. A pick for a stop label is not for the Intro, and nothing the Intro opens asks for one.
 */
internal fun StopSelectedResult.toIntroEvent(): IntroUiEvent? = when (fieldType) {
    SearchStopFieldType.FROM -> IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = stopId, stopName = stopName)
    SearchStopFieldType.TO -> IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = stopId, stopName = stopName)
    SearchStopFieldType.LABEL -> null
}
