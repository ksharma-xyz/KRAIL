package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val INTRO_VERSION = 2
private val SHORT_WAIT_LIMIT = 2.seconds
private val LONG_WAIT_LIMIT = 5.seconds

/** How long the rider waited on the stops data at the last button. */
internal enum class StopsWait { NONE, UNDER_2S, TWO_TO_FIVE_S, OVER_5S, FAILED }

/**
 * What the Intro reports once, at the finish. The property names are the parameter names of the
 * existing `intro_lets_krail` event. It carries no coordinate, stop name or text.
 */
internal data class IntroOutcome(
    val introVersion: Int,
    val finishedOn: IntroStep,
    val parkRideAnswer: ParkRideAnswer,
    val locationResult: LocationResult,
    val addedParkRide: Boolean,
    val savedTrip: Boolean,
    val usedSearch: Boolean,
    val skipped: Boolean,
    val stopsWait: StopsWait,
)

/**
 * [IntroState.tripDecision] being [TripDecision.DECLINED], the rider answering Not now on the confirm screen, is the
 * only thing that makes [IntroOutcome.skipped] true: nothing is saved then. A trip that was saved is not skipped, and
 * neither is a finish with no answer recorded, so no combination of picks decides it on its own.
 */
internal fun IntroState.toOutcome(stopsWait: StopsWait): IntroOutcome = IntroOutcome(
    introVersion = INTRO_VERSION,
    finishedOn = step,
    parkRideAnswer = parkRideAnswer,
    locationResult = locationResult,
    addedParkRide = addedParkRide,
    savedTrip = saved,
    usedSearch = usedSearch,
    skipped = tripDecision == TripDecision.DECLINED && !saved,
    stopsWait = stopsWait,
)

/**
 * The bucket for [waited], the time spent on Getting ready. Failed stops are [StopsWait.FAILED]
 * whatever the wait. No wait is [StopsWait.NONE]. Under two seconds is below 2 s, the middle bucket
 * runs from 2 s to 5 s inclusive, and over five seconds is above 5 s.
 */
internal fun stopsWaitBucket(waited: Duration, readiness: StopsReadiness): StopsWait = when {
    readiness is StopsReadiness.Failed -> StopsWait.FAILED
    waited <= Duration.ZERO -> StopsWait.NONE
    waited < SHORT_WAIT_LIMIT -> StopsWait.UNDER_2S
    waited <= LONG_WAIT_LIMIT -> StopsWait.TWO_TO_FIVE_S
    else -> StopsWait.OVER_5S
}

/**
 * The nine parameters with the names and values of the measurement plan. An unanswered question is
 * reported as `skipped`, because that is what tapping Next past it means.
 */
internal fun IntroOutcome.toParams(): Map<String, Any> = mapOf(
    "introVersion" to introVersion,
    "finishedOn" to finishedOn.toEventValue().value,
    "parkRideAnswer" to when (parkRideAnswer) {
        ParkRideAnswer.ASK -> "skipped"
        ParkRideAnswer.YES -> "yes"
        ParkRideAnswer.NO -> "no"
    },
    "locationResult" to locationResult.name.lowercase(),
    "addedParkRide" to addedParkRide,
    "savedTrip" to savedTrip,
    "usedSearch" to usedSearch,
    "skipped" to skipped,
    "stopsWait" to when (stopsWait) {
        StopsWait.NONE -> "none"
        StopsWait.UNDER_2S -> "under_2s"
        StopsWait.TWO_TO_FIVE_S -> "2_to_5s"
        StopsWait.OVER_5S -> "over_5s"
        StopsWait.FAILED -> "failed"
    },
)
