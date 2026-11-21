package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer

/**
 * The `intro_lets_krail` event for this outcome. The carousel params stay unset, so only the nine
 * outcome params are sent. `IntroOutcomeEventTest` holds the event to [toParams] for every value.
 */
internal fun IntroOutcome.toEvent() = IntroLetsKrailClickEvent(
    introVersion = introVersion,
    finishedOn = finishedOn.toEventValue(),
    parkRideAnswer = parkRideAnswer.toEventValue(),
    locationResult = locationResult.toEventValue(),
    addedParkRide = addedParkRide,
    savedTrip = savedTrip,
    usedSearch = usedSearch,
    skipped = skipped,
    stopsWait = stopsWait.toEventValue(),
)

private fun IntroStep.toEventValue() = when (this) {
    IntroStep.PARK_RIDE -> IntroLetsKrailClickEvent.FinishedOn.PARK_RIDE
    IntroStep.FIRST_TRIP -> IntroLetsKrailClickEvent.FinishedOn.FIRST_TRIP
}

// An unanswered question is what tapping Next past it means, so it is reported as skipped.
private fun ParkRideAnswer.toEventValue() = when (this) {
    ParkRideAnswer.ASK -> IntroLetsKrailClickEvent.ParkRideAnswer.SKIPPED
    ParkRideAnswer.YES -> IntroLetsKrailClickEvent.ParkRideAnswer.YES
    ParkRideAnswer.NO -> IntroLetsKrailClickEvent.ParkRideAnswer.NO
}

private fun LocationResult.toEventValue() = when (this) {
    LocationResult.NOT_ASKED -> IntroLetsKrailClickEvent.LocationResult.NOT_ASKED
    LocationResult.GRANTED -> IntroLetsKrailClickEvent.LocationResult.GRANTED
    LocationResult.DENIED -> IntroLetsKrailClickEvent.LocationResult.DENIED
    LocationResult.UNAVAILABLE -> IntroLetsKrailClickEvent.LocationResult.UNAVAILABLE
}

private fun StopsWait.toEventValue() = when (this) {
    StopsWait.NONE -> IntroLetsKrailClickEvent.StopsWait.NONE
    StopsWait.UNDER_2S -> IntroLetsKrailClickEvent.StopsWait.UNDER_2S
    StopsWait.TWO_TO_FIVE_S -> IntroLetsKrailClickEvent.StopsWait.TWO_TO_FIVE_S
    StopsWait.OVER_5S -> IntroLetsKrailClickEvent.StopsWait.OVER_5S
    StopsWait.FAILED -> IntroLetsKrailClickEvent.StopsWait.FAILED
}
