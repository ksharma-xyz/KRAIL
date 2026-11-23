package xyz.ksharma.krail.trip.planner.ui.intro

import app.cash.turbine.test
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SaveTripClickEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
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
private const val THEME_REPORTED = "track:theme_selected"

class IntroFlowViewModelFinishTest {

    private val tripPicked = IntroState(
        step = IntroStep.CONFIRM,
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = TALLAWONG.stopId,
        startName = TALLAWONG.name,
        endStopId = CENTRAL.stopId,
        endName = CENTRAL.name,
    )
    private val nothingPicked = IntroState(step = IntroStep.CONFIRM, parkRideAnswer = ParkRideAnswer.NO)

    private fun KrailTestScope.rigAt(
        state: IntroState,
        readiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    ) = IntroRig(this, readiness, savedAt(state))

    @Test
    fun `save and start writes exactly one trip then marks the intro seen and leaves`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.withEffects {
            rig.answerAndFinish(yes = true)

            awaitFinish()
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
    fun `not now writes no trip even though both stops are chosen, marks both flags once and leaves`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.withEffects {
            rig.answerAndFinish(yes = false)

            awaitFinish()
        }

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals(1, rig.log.count(SEEN_INTRO))
        assertEquals(1, rig.log.count(SUGGESTION_SHOWN))
        assertFalse(rig.state.saved)
    }

    // No flow reaches the confirm screen without both stops, so this is a guard and not a path: the positive answer
    // with no complete pair saves nothing and still ends the Intro.
    @Test
    fun `a yes with no complete pair saves nothing and still leaves`() = krailRunTest {
        val startOnly = nothingPicked.copy(startStopId = TALLAWONG.stopId, startName = TALLAWONG.name)
        val endOnly = nothingPicked.copy(endStopId = CENTRAL.stopId, endName = CENTRAL.name)

        listOf(nothingPicked, startOnly, endOnly).forEach { picks ->
            val rig = rigAt(picks)

            rig.withEffects {
                rig.answerAndFinish(yes = true)

                awaitFinish()
            }

            assertTrue(rig.sandook.selectAllTrips().isEmpty(), "picks $picks")
            assertEquals(1, rig.log.count(SEEN_INTRO), "picks $picks")
            assertNull(rig.analytics.getTrackedEvent("save_trip_click"), "picks $picks")
        }
    }

    @Test
    fun `the suggestion flag answered earlier is not written again at the finish`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = false),
            IntroUiEvent.SelectStop(CENTRAL.toIntroStop()),
            IntroUiEvent.ConfirmStop,
            IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()),
            IntroUiEvent.ConfirmStop,
        )
        rig.answerAndFinish(yes = true)

        assertEquals(1, rig.log.count(SUGGESTION_SHOWN))
        assertEquals(1, rig.log.count(SEEN_INTRO))
    }

    @Test
    fun `finishing before the stops are ready starts the finale and writes at once, then waits for the stops and reveals`() =
        krailRunTest {
            val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

            rig.withEffects {
                rig.answerAndFinish(yes = true)

                assertEquals(IntroEffect.StartFinale, awaitItem())
                expectNoEvents()
                // The trip and both flags are written before the wait, from the rider's own picks.
                assertEquals(
                    listOf(THEME_REPORTED, "effect", SAVED_TRIP, SUGGESTION_SHOWN, SEEN_INTRO, "awaitReady"),
                    rig.log.entries,
                )
                assertEquals(1, rig.sandook.selectAllTrips().size)

                rig.testTime += 3.seconds
                rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
                rig.send()

                assertEquals(IntroEffect.NavigateToSavedTrips, awaitItem())
            }

            assertEquals(1, rig.log.count(SAVED_TRIP))
            assertEquals("2_to_5s", rig.outcomeEvent().properties?.get("stopsWait"))
        }

    @Test
    fun `a trip is saved while the stops failed, the stops load goes on in the background and the wait is failed`() =
        krailRunTest {
            val rig = rigAt(tripPicked, readiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))

            rig.withEffects {
                rig.answerAndFinish(yes = true)

                awaitFinish()
            }

            assertEquals(1, rig.sandook.selectAllTrips().size)
            assertEquals(1, rig.log.count(SAVED_TRIP))
            // Once when the Intro opened and once more at the finish.
            assertEquals(2, rig.readiness.retryCallCount)
            assertEquals("failed", rig.outcomeEvent().properties?.get("stopsWait"))
            assertEquals(1, rig.log.count(SEEN_INTRO))
            assertTrue(rig.state.saved)
        }

    @Test
    fun `not now while the stops failed writes no trip and still leaves`() = krailRunTest {
        val rig = rigAt(tripPicked, readiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))

        rig.withEffects {
            rig.answerAndFinish(yes = false)

            awaitFinish()
        }

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals("failed", rig.outcomeEvent().properties?.get("stopsWait"))
    }

    @Test
    fun `stops that fail while the finish waits do not stop it, it saves the trip and reports failed`() =
        krailRunTest {
            val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

            rig.answerAndFinish(yes = true)
            assertEquals(
                listOf(THEME_REPORTED, SAVED_TRIP, SUGGESTION_SHOWN, SEEN_INTRO, "awaitReady"),
                rig.log.entries,
            )
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

                rig.answerAndFinish(yes = true)
                // From Unknown the Intro opening asked the load to start, and that is the only difference.
                val opened = if (stillNotReady == StopsReadiness.Unknown) listOf("retry") else emptyList()
                assertEquals(
                    opened + listOf(THEME_REPORTED, SAVED_TRIP, SUGGESTION_SHOWN, SEEN_INTRO, "awaitReady"),
                    rig.log.entries,
                    "readiness $stillNotReady",
                )
                rig.testTime += FINISH_WAIT_TIMEOUT_MS.milliseconds
                pumpOnce(FINISH_WAIT_TIMEOUT_MS)

                assertEquals(1, rig.log.count(SAVED_TRIP), "readiness $stillNotReady")
                assertEquals(opened.size + 1, rig.readiness.retryCallCount, "readiness $stillNotReady")
                assertEquals("over_5s", rig.outcomeEvent().properties?.get("stopsWait"))
            }
        }

    @Test
    fun `the wait for the stops is fifteen seconds`() {
        assertEquals(15_000L, FINISH_WAIT_TIMEOUT_MS)
    }

    @Test
    fun `the finish happens in order theme, finale, trip, both flags, readiness, tracking, then the reveal`() =
        krailRunTest {
            val rig = rigAt(tripPicked)

            rig.withEffects {
                rig.answerAndFinish(yes = true)
                awaitFinish()
            }

            assertEquals(
                listOf(
                    THEME_REPORTED,
                    "effect",
                    SAVED_TRIP,
                    SUGGESTION_SHOWN,
                    SEEN_INTRO,
                    "awaitReady",
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

        rig.answerAndFinish(yes = true)

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
                "themeId" to KrailThemeStyle.Train.id.toString(),
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
        val rig = rigAt(tripPicked.copy(parkRideAnswer = ParkRideAnswer.ASK))

        rig.answerAndFinish(yes = false)

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
                "themeId" to KrailThemeStyle.Train.id.toString(),
            ),
            rig.outcomeEvent().properties,
        )
        assertNull(rig.analytics.getTrackedEvent("save_trip_click"))
    }

    @Test
    fun `a not now with a pair picked reports skipped, saves nothing and sends no save click`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.answerAndFinish(yes = false)

        assertEquals(true, rig.outcomeEvent().properties?.get("skipped"))
        assertEquals(false, rig.outcomeEvent().properties?.get("savedTrip"))
        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertNull(rig.analytics.getTrackedEvent("save_trip_click"))
    }

    @Test
    fun `a yes that saves the trip is never reported as skipped`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.answerAndFinish(yes = true)

        assertEquals(false, rig.outcomeEvent().properties?.get("skipped"))
        assertEquals(true, rig.outcomeEvent().properties?.get("savedTrip"))
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
            IntroUiEvent.SelectStop(CENTRAL.toIntroStop()),
            IntroUiEvent.ConfirmStop,
            IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()),
            IntroUiEvent.ConfirmStop,
        )
        rig.answerAndFinish(yes = true)
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
    fun `pressing finish repeatedly writes one trip tracks once, starts one finale and leaves once`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.withEffects {
            rig.answerAndFinish(yes = true)
            rig.send(IntroUiEvent.ConfirmTheme)
            awaitFinish()
            rig.send(IntroUiEvent.ConfirmTheme)
            expectNoEvents()
        }

        assertEquals(1, rig.log.count(SAVED_TRIP))
        assertEquals(1, rig.log.count("track:intro_lets_krail"))
        assertEquals(2, rig.log.count("effect"), "one to start the finale and one to reveal the app")
    }

    @Test
    fun `a second press while the first is still waiting for the stops does nothing more`() = krailRunTest {
        val rig = rigAt(tripPicked, readiness = StopsReadiness.Loading)

        rig.withEffects {
            rig.answerAndFinish(yes = true)
            assertEquals(IntroEffect.StartFinale, awaitItem())
            rig.send(
                IntroUiEvent.ConfirmTheme,
                IntroUiEvent.ConfirmTheme,
                IntroUiEvent.PickTheme(KrailThemeStyle.Bus.id),
            )
            expectNoEvents()
        }

        assertEquals(1, rig.log.count(SAVED_TRIP))
        assertEquals(1, rig.log.count(SEEN_INTRO))
        assertEquals(1, rig.log.count("effect"))
        assertEquals(KrailThemeStyle.Train.id, rig.state.selectedThemeId, "a pick after the press changes nothing")
    }

    @Test
    fun `lets krail after the trip was saved does not write the trip again`() = krailRunTest {
        val rig = rigAt(tripPicked.copy(saved = true))

        rig.answerAndFinish(yes = true)

        assertEquals(0, rig.log.count(SAVED_TRIP))
        assertNull(rig.analytics.getTrackedEvent("save_trip_click"))
        assertEquals(true, rig.outcomeEvent().properties?.get("savedTrip"))
        assertEquals(1, rig.log.count(SEEN_INTRO))
    }

    @Test
    fun `the stops wait is none when the stops were already ready however long the call took`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(tripPicked), timeSource = TickingTimeSource(1.seconds))

            rig.answerAndFinish(yes = true)

            assertEquals("none", rig.outcomeEvent().properties?.get("stopsWait"))
        }

    @Test
    fun `the confirm screen's answers only record it and move on to the theme screen, writing and reporting nothing`() =
        krailRunTest {
            listOf(true to TripDecision.SAVED, false to TripDecision.DECLINED).forEach { (yes, decision) ->
                val rig = rigAt(tripPicked)

                rig.send(IntroUiEvent.AnswerSaveTrip(yes = yes))

                assertEquals(IntroStep.THEME, rig.state.step, "yes $yes")
                assertEquals(decision, rig.state.tripDecision, "yes $yes")
                assertFalse(rig.state.saved, "yes $yes")
                assertEquals(emptyList(), rig.log.entries, "yes $yes")
                assertTrue(rig.sandook.selectAllTrips().isEmpty(), "yes $yes")
            }
        }

    @Test
    fun `an answer from any screen but the confirm screen does nothing, even with both stops chosen`() = krailRunTest {
        IntroStep.entries.filter { it != IntroStep.CONFIRM }.forEach { step ->
            val on = tripPicked.copy(step = step)
            val rig = rigAt(on)

            rig.send(IntroUiEvent.AnswerSaveTrip(yes = true), IntroUiEvent.AnswerSaveTrip(yes = false))

            assertEquals(on.step, rig.state.step, "$step")
            assertNull(rig.state.tripDecision, "$step")
            assertEquals(emptyList(), rig.log.entries, "$step")
        }
    }

    @Test
    fun `the theme button with no theme selected finishes nothing, and neither does any other screen`() = krailRunTest {
        IntroStep.entries.forEach { step ->
            val rig = rigAt(tripPicked.copy(step = step))

            rig.send(IntroUiEvent.ConfirmTheme)

            assertEquals(emptyList(), rig.log.entries, "$step")
            assertTrue(rig.sandook.selectAllTrips().isEmpty(), "$step")
        }
    }

    @Test
    fun `a finish with no answer recorded, such as after the process was killed, writes no trip and is not skipped`() =
        krailRunTest {
            val rig = rigAt(tripPicked.copy(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Train.id))

            rig.withEffects {
                rig.send(IntroUiEvent.ConfirmTheme)

                awaitFinish()
            }

            assertTrue(rig.sandook.selectAllTrips().isEmpty())
            assertEquals(false, rig.outcomeEvent().properties?.get("skipped"))
            assertEquals(false, rig.outcomeEvent().properties?.get("savedTrip"))
            assertEquals(1, rig.log.count(SEEN_INTRO))
        }

    @Test
    fun `the answer is kept while the theme is chosen, and picking a theme finishes nothing by itself`() = krailRunTest {
        val rig = rigAt(tripPicked)

        rig.send(IntroUiEvent.AnswerSaveTrip(yes = true), IntroUiEvent.PickTheme(KrailThemeStyle.Bus.id))

        assertEquals(TripDecision.SAVED, rig.state.tripDecision)
        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals(0, rig.log.count(SEEN_INTRO))
    }

    @Test
    fun `the finish effect is not replayed to a collector that starts later`() = krailRunTest {
        val rig = rigAt(tripPicked)
        rig.answerAndFinish(yes = true)

        rig.viewModel.effects.test {
            awaitFinish()
            cancelAndIgnoreRemainingEvents()
        }
        rig.viewModel.effects.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
