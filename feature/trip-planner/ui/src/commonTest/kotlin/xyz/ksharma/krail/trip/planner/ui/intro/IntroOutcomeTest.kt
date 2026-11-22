package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class IntroOutcomeTest {

    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val failed = StopsReadiness.Failed(attempt = 1, reason = "disk full")

    // A rider on the theme screen, where the Intro ends, with the confirm screen not answered, so every flag is in its
    // resting position.
    private val resting = IntroState(step = IntroStep.THEME)
    private val restingOutcome = IntroOutcome(
        introVersion = 2,
        finishedOn = IntroStep.THEME,
        parkRideAnswer = ParkRideAnswer.ASK,
        locationResult = LocationResult.NOT_ASKED,
        addedParkRide = false,
        savedTrip = false,
        usedSearch = false,
        skipped = false,
        stopsWait = StopsWait.NONE,
    )

    @Test
    fun `a finish with no answer to the confirm screen recorded is neither saved nor skipped`() {
        assertEquals(restingOutcome, resting.toOutcome(StopsWait.NONE))
    }

    @Test
    fun `each flag is read from its own field`() {
        assertEquals(
            restingOutcome.copy(addedParkRide = true),
            resting.copy(addedParkRide = true).toOutcome(StopsWait.NONE),
        )
        assertEquals(
            restingOutcome.copy(usedSearch = true),
            resting.copy(usedSearch = true).toOutcome(StopsWait.NONE),
        )
        assertEquals(
            restingOutcome.copy(finishedOn = IntroStep.PARK_RIDE),
            resting.copy(step = IntroStep.PARK_RIDE).toOutcome(StopsWait.NONE),
        )
    }

    @Test
    fun `the answer and the location result are carried as they are`() {
        ParkRideAnswer.entries.forEach { answer ->
            assertEquals(
                restingOutcome.copy(parkRideAnswer = answer),
                resting.copy(parkRideAnswer = answer).toOutcome(StopsWait.NONE),
            )
        }
        LocationResult.entries.forEach { result ->
            assertEquals(
                restingOutcome.copy(locationResult = result),
                resting.copy(locationResult = result).toOutcome(StopsWait.NONE),
            )
        }
    }

    @Test
    fun `a saved trip is saved and not skipped`() {
        val saved = resting.copy(startStopId = "1", endStopId = "2", saved = true)

        assertEquals(
            restingOutcome.copy(savedTrip = true, skipped = false),
            saved.toOutcome(StopsWait.NONE),
        )
    }

    @Test
    fun `Not now on the confirm screen is skipped with nothing saved, whatever was picked`() {
        val picked = resting.copy(startStopId = "1", endStopId = "2")

        val declined = TripDecision.DECLINED
        assertEquals(restingOutcome.copy(skipped = true), resting.copy(tripDecision = declined).toOutcome(StopsWait.NONE))
        assertEquals(restingOutcome.copy(skipped = true), picked.copy(tripDecision = declined).toOutcome(StopsWait.NONE))
        assertEquals(restingOutcome.copy(skipped = false), picked.toOutcome(StopsWait.NONE))
        assertEquals(
            restingOutcome.copy(skipped = false),
            picked.copy(tripDecision = TripDecision.SAVED).toOutcome(StopsWait.NONE),
            "a yes that has not been written yet is not skipped either",
        )
    }

    @Test
    fun `a trip that was saved is never skipped, even if the answer says Not now`() {
        val saved = resting.copy(startStopId = "1", endStopId = "2", saved = true)

        assertEquals(false, saved.copy(tripDecision = TripDecision.DECLINED).toOutcome(StopsWait.NONE).skipped)
    }

    @Test
    fun `the wait bucket is passed through`() {
        StopsWait.entries.forEach { wait ->
            assertEquals(restingOutcome.copy(stopsWait = wait), resting.toOutcome(wait))
        }
    }

    @Test
    fun `the parameters carry the names and values the registry expects`() {
        val outcome = IntroOutcome(
            introVersion = 2,
            finishedOn = IntroStep.CONFIRM,
            parkRideAnswer = ParkRideAnswer.YES,
            locationResult = LocationResult.GRANTED,
            addedParkRide = true,
            savedTrip = true,
            usedSearch = false,
            skipped = false,
            stopsWait = StopsWait.TWO_TO_FIVE_S,
        )

        assertEquals(
            mapOf(
                "introVersion" to 2,
                "finishedOn" to "first_trip",
                "parkRideAnswer" to "yes",
                "locationResult" to "granted",
                "addedParkRide" to true,
                "savedTrip" to true,
                "usedSearch" to false,
                "skipped" to false,
                "stopsWait" to "2_to_5s",
            ),
            outcome.toParams(),
        )
    }

    @Test
    fun `an unanswered question is reported as skipped and every other value keeps its word`() {
        val params = restingOutcome.copy(finishedOn = IntroStep.PARK_RIDE).toParams()

        assertEquals("skipped", params["parkRideAnswer"])
        assertEquals("park_ride", params["finishedOn"])
        assertEquals("no", restingOutcome.copy(parkRideAnswer = ParkRideAnswer.NO).toParams()["parkRideAnswer"])
        assertEquals(
            listOf("granted", "denied", "unavailable", "not_asked"),
            listOf(
                LocationResult.GRANTED,
                LocationResult.DENIED,
                LocationResult.UNAVAILABLE,
                LocationResult.NOT_ASKED,
            ).map { restingOutcome.copy(locationResult = it).toParams()["locationResult"] },
        )
        assertEquals(
            listOf("none", "under_2s", "2_to_5s", "over_5s", "failed"),
            StopsWait.entries.map { restingOutcome.copy(stopsWait = it).toParams()["stopsWait"] },
        )
    }

    @Test
    fun `the finish place is the first trip for every screen but Park and Ride, so a finish on the theme screen is the first trip`() {
        assertEquals(
            mapOf(
                IntroStep.PARK_RIDE to "park_ride",
                IntroStep.DESTINATION to "first_trip",
                IntroStep.ORIGIN to "first_trip",
                IntroStep.CONFIRM to "first_trip",
                IntroStep.THEME to "first_trip",
            ),
            IntroStep.entries.associateWith { restingOutcome.copy(finishedOn = it).toParams()["finishedOn"] },
        )
    }

    @Test
    fun `no wait is none, whatever the stops state`() {
        assertEquals(StopsWait.NONE, stopsWaitBucket(Duration.ZERO, ready))
    }

    @Test
    fun `the bucket edges sit at two and five seconds`() {
        assertEquals(StopsWait.UNDER_2S, stopsWaitBucket(1.milliseconds, ready))
        assertEquals(StopsWait.UNDER_2S, stopsWaitBucket(1999.milliseconds, ready))
        assertEquals(StopsWait.TWO_TO_FIVE_S, stopsWaitBucket(2.seconds, ready))
        assertEquals(StopsWait.TWO_TO_FIVE_S, stopsWaitBucket(5.seconds, ready))
        assertEquals(StopsWait.OVER_5S, stopsWaitBucket(5001.milliseconds, ready))
    }

    @Test
    fun `failed stops are failed whatever the rider waited`() {
        assertEquals(StopsWait.FAILED, stopsWaitBucket(Duration.ZERO, failed))
        assertEquals(StopsWait.FAILED, stopsWaitBucket(3.seconds, failed))
    }

    @Test
    fun `a wait that ended before the stops were ready is still timed`() {
        assertEquals(StopsWait.OVER_5S, stopsWaitBucket(10.seconds, StopsReadiness.Loading))
        assertEquals(StopsWait.UNDER_2S, stopsWaitBucket(500.milliseconds, StopsReadiness.Unknown))
    }
}
