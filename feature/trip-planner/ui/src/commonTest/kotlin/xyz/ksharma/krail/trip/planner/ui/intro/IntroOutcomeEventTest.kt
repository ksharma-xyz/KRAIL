package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Holds the Intro outcome and the analytics event to the same names and values, so the two cannot
 * drift apart. The mapping here is the one the view model branch will write for real.
 */
class IntroOutcomeEventTest {

    @Test
    fun `the event carries exactly what the outcome reports for every value of every list`() {
        val outcomes = allOutcomes()

        outcomes.forEach { assertEquals(it.toParams(), it.asEvent().properties) }

        assertEquals(
            IntroStep.entries.size * ParkRideAnswer.entries.size *
                LocationResult.entries.size * StopsWait.entries.size,
            outcomes.size,
        )
    }

    @Test
    fun `the event carries exactly what the outcome reports for each flag`() {
        listOf(true, false).forEach { flag ->
            val outcome = outcome(
                IntroStep.FIRST_TRIP,
                ParkRideAnswer.YES,
                LocationResult.GRANTED,
                StopsWait.NONE,
            ).copy(addedParkRide = flag, savedTrip = !flag, usedSearch = flag, skipped = !flag)

            assertEquals(outcome.toParams(), outcome.asEvent().properties)
        }
    }

    private fun outcome(
        step: IntroStep,
        answer: ParkRideAnswer,
        location: LocationResult,
        wait: StopsWait,
    ) = IntroOutcome(
        introVersion = 2,
        finishedOn = step,
        parkRideAnswer = answer,
        locationResult = location,
        addedParkRide = false,
        savedTrip = false,
        usedSearch = false,
        skipped = true,
        stopsWait = wait,
    )

    private fun allOutcomes(): List<IntroOutcome> {
        val stepsAndAnswers = IntroStep.entries.flatMap { step ->
            ParkRideAnswer.entries.map { answer -> step to answer }
        }
        val locationsAndWaits = LocationResult.entries.flatMap { location ->
            StopsWait.entries.map { wait -> location to wait }
        }
        return stepsAndAnswers.flatMap { (step, answer) ->
            locationsAndWaits.map { (location, wait) -> outcome(step, answer, location, wait) }
        }
    }

    private fun IntroOutcome.asEvent() = IntroLetsKrailClickEvent(
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
}

private fun IntroStep.toEventValue() = when (this) {
    IntroStep.PARK_RIDE -> IntroLetsKrailClickEvent.FinishedOn.PARK_RIDE
    IntroStep.FIRST_TRIP -> IntroLetsKrailClickEvent.FinishedOn.FIRST_TRIP
}

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
