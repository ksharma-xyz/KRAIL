package xyz.ksharma.krail.trip.planner.ui.intro

import app.cash.turbine.test
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SaveTripClickEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val SEEN_INTRO = "pref:KEY_HAS_SEEN_INTRO=true"
private const val SUGGESTION_SHOWN = "pref:KEY_PARK_RIDE_SUGGESTION_SHOWN=true"
private const val SAVED_TRIP = "trip:2155384->200060"

class IntroFlowViewModelFinishTest {

    private val tripPicked = IntroState(
        step = IntroStep.DESTINATION,
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = TALLAWONG.stopId,
        startName = TALLAWONG.name,
        endStopId = CENTRAL.stopId,
        endName = CENTRAL.name,
    )
    private val nothingPicked = IntroState(step = IntroStep.DESTINATION, parkRideAnswer = ParkRideAnswer.NO)

    private fun KrailTestScope.rigAt(
        state: IntroState,
        readiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    ) = IntroRig(this, readiness, savedAt(state))

    @Test
    fun `save and start writes exactly one trip then marks the intro seen and leaves`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.withEffects {
            rig.send(IntroUiEvent.FinishPressed)

            assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
        }

        val trip = rig.sandook.selectAllTrips().single()
        assertEquals(
            listOf(TALLAWONG.stopId, TALLAWONG.name, CENTRAL.stopId, CENTRAL.name),
            listOf(trip.fromStopId, trip.fromStopName, trip.toStopId, trip.toStopName),
        )
        assertEquals(1, rig.log.count(SAVED_TRIP))
        assertEquals(1, rig.log.count(SEEN_INTRO))
        assertTrue(rig.state.saved)
    }

    @Test
    fun `not now writes no trip even when a pair is picked, marks both flags once and leaves`() = krailRunTest {
        listOf(nothingPicked, tripPicked).forEach { picks ->
            val rig = rigAt(picks)

            rig.withEffects {
                rig.send(IntroUiEvent.FinishWithoutTrip)

                assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
            }

            assertTrue(rig.sandook.selectAllTrips().isEmpty(), "picks $picks")
            assertEquals(1, rig.log.count(SEEN_INTRO))
            assertEquals(1, rig.log.count(SUGGESTION_SHOWN))
            assertFalse(rig.state.saved)
        }
    }

    // Temporary until the confirm screen: the positive answer with no complete pair saves nothing and still ends the Intro.
    @Test
    fun `a yes with no complete pair saves nothing and still leaves`() = krailRunTest {
        val startOnly = nothingPicked.copy(startStopId = TALLAWONG.stopId, startName = TALLAWONG.name)
        val endOnly = nothingPicked.copy(endStopId = CENTRAL.stopId, endName = CENTRAL.name)

        listOf(nothingPicked, startOnly, endOnly).forEach { picks ->
            val rig = rigAt(picks)

            rig.withEffects {
                rig.send(IntroUiEvent.FinishPressed)

                assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
            }

            assertTrue(rig.sandook.selectAllTrips().isEmpty(), "picks $picks")
            assertEquals(1, rig.log.count(SEEN_INTRO), "picks $picks")
            assertNull(rig.analytics.getTrackedEvent("save_trip_click"), "picks $picks")
        }
    }

    @Test
    fun `a pair completed while the finish waits is the pair that is saved`() = krailRunTest {
        val rig = rigAt(nothingPicked, readiness = StopsReadiness.Loading)

        rig.send(IntroUiEvent.FinishPressed, IntroUiEvent.PickStart(TALLAWONG), IntroUiEvent.PickEnd(CENTRAL))
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        rig.send()

        assertEquals(1, rig.log.count(SAVED_TRIP))
    }

    @Test
    fun `the suggestion flag answered earlier is not written again at the finish`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.FinishPressed)

        assertEquals(1, rig.log.count(SUGGESTION_SHOWN))
        assertEquals(1, rig.log.count(SEEN_INTRO))
    }

    @Test
    fun `finishing before the stops are ready waits and then writes`() = krailRunTest {
        val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

        rig.send(IntroUiEvent.FinishPressed)

        assertEquals(listOf("awaitReady"), rig.log.entries)
        assertTrue(rig.sandook.selectAllTrips().isEmpty())

        rig.testTime += 3.seconds
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        rig.send()

        assertEquals(1, rig.log.count(SAVED_TRIP))
        assertEquals("2_to_5s", rig.outcomeEvent().properties?.get("stopsWait"))
    }

    @Test
    fun `a trip is saved while the stops failed, the stops load goes on in the background and the wait is failed`() =
        krailRunTest {
            val rig = rigAt(tripPicked, readiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))

            rig.withEffects {
                rig.send(IntroUiEvent.FinishPressed)

                assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
            }

            assertEquals(1, rig.sandook.selectAllTrips().size)
            assertEquals(1, rig.log.count(SAVED_TRIP))
            assertEquals(1, rig.readiness.retryCallCount)
            assertEquals("failed", rig.outcomeEvent().properties?.get("stopsWait"))
            assertEquals(1, rig.log.count(SEEN_INTRO))
            assertTrue(rig.state.saved)
        }

    @Test
    fun `not now while the stops failed writes no trip and still leaves`() = krailRunTest {
        val rig = rigAt(nothingPicked, readiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))

        rig.withEffects {
            rig.send(IntroUiEvent.FinishWithoutTrip)

            assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
        }

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals("failed", rig.outcomeEvent().properties?.get("stopsWait"))
    }

    @Test
    fun `stops that fail while the finish waits do not stop it, it saves the trip and reports failed`() =
        krailRunTest {
            val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

            rig.send(IntroUiEvent.FinishPressed)
            assertEquals(listOf("awaitReady"), rig.log.entries)
            rig.readiness.emit(StopsReadiness.Failed(attempt = 4, reason = "disk"))
            rig.send()

            assertEquals(1, rig.log.count(SAVED_TRIP))
            assertEquals(1, rig.readiness.retryCallCount)
            assertEquals("failed", rig.outcomeEvent().properties?.get("stopsWait"))
        }

    @Test
    fun `a wait that runs out after fifteen seconds still saves the trip and reports over five seconds`() =
        krailRunTest {
            listOf(StopsReadiness.Loading, StopsReadiness.Unknown).forEach { stillNotReady ->
                val rig = rigAt(tripPicked, readiness = stillNotReady)

                rig.send(IntroUiEvent.FinishPressed)
                assertEquals(listOf("awaitReady"), rig.log.entries, "readiness $stillNotReady")
                rig.testTime += FINISH_WAIT_TIMEOUT_MS.milliseconds
                pumpOnce(FINISH_WAIT_TIMEOUT_MS)

                assertEquals(1, rig.log.count(SAVED_TRIP), "readiness $stillNotReady")
                assertEquals(1, rig.readiness.retryCallCount, "readiness $stillNotReady")
                assertEquals("over_5s", rig.outcomeEvent().properties?.get("stopsWait"))
            }
        }

    @Test
    fun `the wait for the stops is fifteen seconds`() {
        assertEquals(15_000L, FINISH_WAIT_TIMEOUT_MS)
    }

    @Test
    fun `going back while the finish waits abandons it`() = krailRunTest {
        val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

        rig.send(IntroUiEvent.FinishPressed, IntroUiEvent.Back)
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        rig.send()

        assertEquals(listOf("awaitReady"), rig.log.entries)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
    }

    @Test
    fun `the finish happens in order readiness then trip then both flags then tracking then the effect`() =
        krailRunTest {
            val rig = rigAt(tripPicked)

            rig.withEffects {
                rig.send(IntroUiEvent.FinishPressed)
                awaitItem()
            }

            assertEquals(
                listOf(
                    "awaitReady",
                    SAVED_TRIP,
                    SUGGESTION_SHOWN,
                    SEEN_INTRO,
                    "track:save_trip_click",
                    "track:intro_lets_krail",
                    "effect",
                ),
                rig.log.entries,
            )
        }

    @Test
    fun `a saved trip reports the exact outcome and a save click from the intro`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.send(IntroUiEvent.FinishPressed)

        assertEquals(
            mapOf(
                "introVersion" to 2,
                "finishedOn" to "first_trip",
                "parkRideAnswer" to "no",
                "locationResult" to "not_asked",
                "addedParkRide" to false,
                "savedTrip" to true,
                "usedSearch" to false,
                "skipped" to false,
                "stopsWait" to "none",
            ),
            rig.outcomeEvent().properties,
        )
        val save = assertIs<SaveTripClickEvent>(rig.analytics.getTrackedEvent("save_trip_click"))
        assertEquals(
            mapOf("fromStopId" to TALLAWONG.stopId, "toStopId" to CENTRAL.stopId, "source" to "intro"),
            save.properties,
        )
    }

    @Test
    fun `a not now with the question never answered reports skipped and no save click`() = krailRunTest {
        val rig = rigAt(nothingPicked.copy(parkRideAnswer = ParkRideAnswer.ASK))

        rig.send(IntroUiEvent.FinishWithoutTrip)

        assertEquals(
            mapOf(
                "introVersion" to 2,
                "finishedOn" to "first_trip",
                "parkRideAnswer" to "skipped",
                "locationResult" to "not_asked",
                "addedParkRide" to false,
                "savedTrip" to false,
                "usedSearch" to false,
                "skipped" to true,
                "stopsWait" to "none",
            ),
            rig.outcomeEvent().properties,
        )
        assertNull(rig.analytics.getTrackedEvent("save_trip_click"))
    }

    @Test
    fun `no tracked property carries a stop name, a station name or a coordinate`() = krailRunTest {
        val rig = IntroRig(this)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )
        rig.send(
            IntroUiEvent.DeclineParkRide,
            IntroUiEvent.PickStart(TALLAWONG),
            IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = CENTRAL.stopId, stopName = CENTRAL.name),
            IntroUiEvent.FinishPressed,
        )
        assertIs<ParkRideCardPhase.Card>(rig.state.parkRideCard, "the station card was found, so its name is in play")

        val values = listOf("intro_lets_krail", "save_trip_click")
            .flatMap { rig.analytics.getTrackedEvents(it) }
            .flatMap { it.properties.orEmpty().values }
            .map { it.toString() }

        assertTrue(values.isNotEmpty())
        listOf("Station", "Bella Vista", TALLAWONG.name, CENTRAL.name, "33.77", "150.94").forEach { forbidden ->
            assertTrue(values.none { it.contains(forbidden) }, "a property carries $forbidden: $values")
        }
    }

    @Test
    fun `pressing finish repeatedly writes one trip tracks once and leaves once`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.withEffects {
            rig.send(IntroUiEvent.FinishPressed, IntroUiEvent.FinishPressed)
            awaitItem()
            rig.send(IntroUiEvent.FinishPressed)
            expectNoEvents()
        }

        assertEquals(1, rig.log.count(SAVED_TRIP))
        assertEquals(1, rig.log.count("track:intro_lets_krail"))
        assertEquals(1, rig.log.count("effect"))
    }

    @Test
    fun `lets krail after the trip was saved does not write the trip again`() = krailRunTest {
        val rig = rigAt(tripPicked.copy(saved = true))

        rig.send(IntroUiEvent.FinishPressed)

        assertEquals(0, rig.log.count(SAVED_TRIP))
        assertNull(rig.analytics.getTrackedEvent("save_trip_click"))
        assertEquals(true, rig.outcomeEvent().properties?.get("savedTrip"))
        assertEquals(1, rig.log.count(SEEN_INTRO))
    }

    @Test
    fun `the stops wait is none when the stops were already ready however long the call took`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(tripPicked), timeSource = TickingTimeSource(1.seconds))

            rig.send(IntroUiEvent.FinishPressed)

            assertEquals("none", rig.outcomeEvent().properties?.get("stopsWait"))
        }

    @Test
    fun `an answer to the last screens on a screen that is not one of them does nothing`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.FinishPressed, IntroUiEvent.FinishWithoutTrip)

        assertEquals(ON_PARK_RIDE, rig.state)
        assertEquals(emptyList(), rig.log.entries)
    }

    @Test
    fun `the finish effect is not replayed to a collector that starts later`() = krailRunTest {
        val rig = rigAt(nothingPicked)
        rig.send(IntroUiEvent.FinishPressed)

        rig.viewModel.effects.test {
            assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        rig.viewModel.effects.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
