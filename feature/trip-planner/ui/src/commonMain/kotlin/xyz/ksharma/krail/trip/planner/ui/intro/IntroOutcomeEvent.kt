package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.themeselection.themeIdParam

/**
 * The `intro_lets_krail` event for this outcome. The carousel params stay unset, so only the nine
 * outcome params are sent, with `themeId` when a colour was chosen. This is the only place the event is built,
 * and [IntroOutcome.introVersion] is not optional, so no firing is without it. `IntroOutcomeEventTest` holds the
 * event to [toParams] for every value.
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
    themeId = themeId?.let(::themeIdParam),
)

/**
 * The event has two finish places, the Park & Ride screen and the first trip, and the screens that
 * ask for a trip are all the first trip, so the rebuilt Intro reports the same two values. The theme
 * screen is the last, after the trip screens, so it is where a finished Intro ends and counts with the first trip.
 */
internal fun IntroStep.toEventValue() = when (this) {
    IntroStep.PARK_RIDE -> IntroLetsKrailClickEvent.FinishedOn.PARK_RIDE
    IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM, IntroStep.THEME ->
        IntroLetsKrailClickEvent.FinishedOn.FIRST_TRIP
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
