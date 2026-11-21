package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Holds the Intro outcome and the analytics event to the same names and values, so the two cannot
 * drift apart. The mapping under test is the real one, `IntroOutcome.toEvent()`, the one the view
 * model reports through.
 */
class IntroOutcomeEventTest {

    @Test
    fun `the event carries exactly what the outcome reports for every value of every list`() {
        val outcomes = allOutcomes()

        outcomes.forEach { assertEquals(it.toParams(), it.toEvent().properties) }

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

            assertEquals(outcome.toParams(), outcome.toEvent().properties)
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
}
