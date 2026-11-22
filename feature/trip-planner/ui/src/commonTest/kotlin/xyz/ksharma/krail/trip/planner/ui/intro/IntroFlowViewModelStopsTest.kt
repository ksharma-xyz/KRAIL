package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.sandook.db.SelectStopsNearby
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The destination and origin screens in the view model: choosing a stop, the right button and the typed search. The
 * lists they act on are held by `IntroStopRowsTest`, so these are about the events.
 */
class IntroFlowViewModelStopsTest {

    private val onDestination = IntroState(step = IntroStep.DESTINATION, parkRideAnswer = ParkRideAnswer.NO)

    private fun result(stop: IntroStop, vararg modes: TransportMode) =
        SearchStopState.SearchResult.Stop(stop.name, stop.stopId, persistentListOf(*modes))

    private fun nearbyRow(stop: IntroStop) = SelectStopsNearby(
        stopId = stop.stopId,
        stopName = stop.name,
        stopLat = -33.88,
        stopLon = 151.2,
        isParent = null,
        productClasses = "1",
        distanceSquared = 0.0,
    )

    // region choosing

    @Test
    fun `tapping a row on the destination screen sets the end and moves to the origin screen`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))

        rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP))

        assertEquals(WYNYARD_STOP.stopId, rig.state.endStopId)
        assertEquals(WYNYARD_STOP.name, rig.state.endName)
        assertNull(rig.state.startStopId)
        assertEquals(IntroStep.ORIGIN, rig.state.step)
    }

    @Test
    fun `tapping a row on the origin screen sets the start and moves to the confirm screen with a trip`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(onDestination))

            rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP), IntroUiEvent.ChooseStop(TALLAWONG_STOP))

            assertEquals(TALLAWONG_STOP.stopId, rig.state.startStopId)
            assertEquals(IntroStep.CONFIRM, rig.state.step)
            assertTrue(rig.state.hasTrip)
        }

    @Test
    fun `the right button takes the first row of the destination list, the first curated end`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))

        rig.send(IntroUiEvent.UseHighlightedStop)

        assertEquals(CENTRAL_STOP.stopId, rig.state.endStopId)
        assertEquals(IntroStep.ORIGIN, rig.state.step)
    }

    @Test
    fun `the right button on the origin screen takes the first curated start when there is no fix`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))

        rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP), IntroUiEvent.UseHighlightedStop)

        assertEquals(TALLAWONG_STOP.stopId, rig.state.startStopId)
        assertEquals(IntroStep.CONFIRM, rig.state.step)
    }

    @Test
    fun `the right button on the origin screen takes the nearest station when there is a fix`() = krailRunTest {
        val rig = IntroRig(this)
        rig.nswStops.nearbyRows = listOf(REDFERN_STOP, MUSEUM_STOP).map(::nearbyRow)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )
        // The rider takes their time on the destination screen, so the stations are found by then.
        rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP))
        assertEquals(IntroStep.ORIGIN, rig.state.step)

        rig.send(IntroUiEvent.UseHighlightedStop)

        assertEquals(REDFERN_STOP.stopId, rig.state.startStopId, "the nearest station, not the first curated one")
    }

    @Test
    fun `a destination that is also the nearest station is left out of the origin list and the next is taken`() =
        krailRunTest {
            val rig = IntroRig(this)
            rig.nswStops.nearbyRows = listOf(REDFERN_STOP, MUSEUM_STOP).map(::nearbyRow)
            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
            )
            rig.send(IntroUiEvent.ChooseStop(REDFERN_STOP), IntroUiEvent.UseHighlightedStop)

            assertEquals(MUSEUM_STOP.stopId, rig.state.startStopId)
            assertEquals(REDFERN_STOP.stopId, rig.state.endStopId)
        }

    @Test
    fun `no search never chooses and never leaves the screen`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        val before = rig.state

        rig.send(IntroUiEvent.FocusSearch)

        assertEquals(before, rig.state)
    }

    @Test
    fun `a choice on a screen that does not list stops does nothing`() = krailRunTest {
        listOf(IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.CONFIRM).forEach { step ->
            val on = IntroState(step = step)
            val rig = IntroRig(this, handle = savedAt(on))

            rig.send(
                IntroUiEvent.ChooseStop(CENTRAL_STOP),
                IntroUiEvent.UseHighlightedStop,
                IntroUiEvent.QueryChanged("a"),
            )

            assertEquals(on, rig.state, "$step")
        }
    }

    @Test
    fun `with a car park as the start the destination goes to the confirm screen`() =
        krailRunTest {
            val parkRideStart = onDestination.copy(
                parkRideAnswer = ParkRideAnswer.YES,
                addedParkRide = true,
                startStopId = TALLAWONG_STOP.stopId,
                startName = TALLAWONG_STOP.name,
                startFromParkRide = true,
            )
            val rig = IntroRig(this, handle = savedAt(parkRideStart))

            rig.send(IntroUiEvent.ChooseStop(CENTRAL_STOP))
            assertEquals(IntroStep.CONFIRM, rig.state.step, "the origin screen is passed over")
            assertEquals(CENTRAL_STOP.stopId, rig.state.endStopId)
        }

    // endregion

    // region the typed search

    @Test
    fun `typing shows the text at once and the results after the pause, the same pause as Search Stop`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(onDestination))
            rig.stopResults.answer = listOf(result(CHATSWOOD_STOP, TransportMode.Train))

            rig.send(IntroUiEvent.QueryChanged("chat"))

            assertEquals("chat", rig.state.query)
            assertEquals(IntroSearchPhase.Idle, rig.state.search, "nothing is searched before the pause")
            assertTrue(rig.stopResults.searches.isEmpty())

            pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

            assertEquals(IntroSearchPhase.Results(persistentListOf(CHATSWOOD_STOP)), rig.state.search)
            assertEquals(listOf("chat" to false), rig.stopResults.searches)
            assertEquals(100L, INTRO_SEARCH_DEBOUNCE_MS)
        }

    @Test
    fun `typing on within the pause searches only for the text the rider ended on`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

        rig.send(IntroUiEvent.QueryChanged("c"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS / 2)
        rig.send(IntroUiEvent.QueryChanged("ch"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS / 2)
        rig.send(IntroUiEvent.QueryChanged("cha"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        assertEquals(listOf("cha" to false), rig.stopResults.searches)
    }

    @Test
    fun `the results show the first result as the highlighted one and the stop's badges`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(
            result(CHATSWOOD_STOP, TransportMode.Train, TransportMode.Metro),
            result(JUNIORS_KINGSFORD, TransportMode.LightRail),
        )

        rig.send(IntroUiEvent.QueryChanged("ch"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        assertEquals(CHATSWOOD_STOP, rig.state.highlightedStop(rig.viewModel.suggestedStops))
        assertEquals<List<TransportMode>?>(
            listOf(TransportMode.Train, TransportMode.Metro),
            rig.state.stopModes[CHATSWOOD_STOP.stopId],
        )
    }

    @Test
    fun `a search that finds nothing is no match, and the right button has nothing to take`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = emptyList()

        rig.send(IntroUiEvent.QueryChanged("zzzz"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)
        rig.send(IntroUiEvent.UseHighlightedStop)

        assertEquals(IntroSearchPhase.NoMatch, rig.state.search)
        assertEquals(IntroStep.DESTINATION, rig.state.step, "nothing was highlighted, so nothing was chosen")
        assertNull(rig.state.endStopId)
    }

    @Test
    fun `clearing the text shows the suggestions again at once and cancels the search that was waiting`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(onDestination))
            rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

            rig.send(IntroUiEvent.QueryChanged("chat"))
            rig.send(IntroUiEvent.QueryChanged(""))
            pumpOnce(INTRO_SEARCH_DEBOUNCE_MS * 2)

            assertEquals(IntroSearchPhase.Idle, rig.state.search)
            assertTrue(rig.stopResults.searches.isEmpty(), "the cleared text was never searched")
        }

    @Test
    fun `the same text again changes and searches nothing more`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))
        rig.send(IntroUiEvent.QueryChanged("chat"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        rig.send(IntroUiEvent.QueryChanged("chat"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        assertEquals(1, rig.stopResults.searches.size)
    }

    @Test
    fun `the field is usable while the stops load, finds nothing, and fills in as soon as they are ready`() =
        krailRunTest {
            val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading, handle = savedAt(onDestination))
            rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

            rig.send(IntroUiEvent.QueryChanged("chat"))
            pumpOnce(INTRO_SEARCH_DEBOUNCE_MS * 20)

            assertEquals("chat", rig.state.query, "the field takes the text")
            assertEquals(IntroSearchPhase.Idle, rig.state.search, "the suggestions still show")
            assertTrue(rig.stopResults.searches.isEmpty(), "a table that is not ready is not searched")

            rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
            runCurrent()

            assertEquals(IntroSearchPhase.Results(persistentListOf(CHATSWOOD_STOP)), rig.state.search)
        }

    @Test
    fun `stops that failed leave the suggestions showing, with no error and no search`() = krailRunTest {
        val rig = IntroRig(
            this,
            initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk full"),
            handle = savedAt(onDestination),
        )
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

        rig.send(IntroUiEvent.QueryChanged("chat"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        assertEquals(IntroSearchPhase.Idle, rig.state.search)
        assertTrue(rig.stopResults.searches.isEmpty())
        assertEquals(IntroStep.DESTINATION, rig.state.step)
    }

    @Test
    fun `a search that throws leaves the suggestions showing`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.shouldThrowError = true

        rig.send(IntroUiEvent.QueryChanged("chat"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        assertEquals(IntroSearchPhase.Idle, rig.state.search)
        assertEquals("chat", rig.state.query)
    }

    @Test
    fun `choosing while a search is waiting cancels it, so it cannot change the next screen`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

        rig.send(IntroUiEvent.QueryChanged("chat"), IntroUiEvent.ChooseStop(CENTRAL_STOP))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS * 2)

        assertEquals(IntroStep.ORIGIN, rig.state.step)
        assertEquals("", rig.state.query)
        assertEquals(IntroSearchPhase.Idle, rig.state.search)
        assertTrue(rig.stopResults.searches.isEmpty())
    }

    @Test
    fun `a stop chosen from the results sets used search and one chosen from the suggestions does not`() =
        krailRunTest {
            val typed = IntroRig(this, handle = savedAt(onDestination))
            typed.stopResults.answer = listOf(result(CHATSWOOD_STOP))
            typed.send(IntroUiEvent.QueryChanged("chat"))
            pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)
            typed.send(IntroUiEvent.UseHighlightedStop)
            assertTrue(typed.state.usedSearch)
            assertEquals(CHATSWOOD_STOP.stopId, typed.state.endStopId)

            val suggested = IntroRig(this, handle = savedAt(onDestination))
            suggested.send(IntroUiEvent.ChooseStop(CENTRAL_STOP))
            assertFalse(suggested.state.usedSearch)
        }

    @Test
    fun `the typed text is never reported, so no tracked event carries it`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))

        rig.send(IntroUiEvent.QueryChanged("chatswo"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS * 10)
        rig.send(IntroUiEvent.UseHighlightedStop, IntroUiEvent.ChooseStop(TALLAWONG_STOP), IntroUiEvent.FinishPressed)

        assertTrue(rig.analytics.events.isNotEmpty(), "the finish reported its outcome")
        rig.analytics.events.forEach { event ->
            assertFalse("chatswo" in event.toString(), "an event carries the typed text: $event")
            assertFalse("Chatswood" in event.toString(), "an event carries a stop name: $event")
        }
        assertTrue(
            rig.analytics.events.none { it.name.contains("search") },
            "no search event is sent: ${rig.analytics.events.map { it.name }}",
        )
        assertEquals(true, rig.outcomeEvent().properties?.get("usedSearch"), "only the flag says search was used")
    }

    // endregion

    // region the saved state

    @Test
    fun `the chosen stops and the screen survive a new view model, and what was typed does not`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))
        rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP), IntroUiEvent.QueryChanged("chat"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)
        assertTrue(rig.state.search is IntroSearchPhase.Results)

        val restored = IntroRig(this, handle = rig.handle)
        restored.settle()

        assertEquals(IntroStep.ORIGIN, restored.state.step)
        assertEquals(WYNYARD_STOP.stopId, restored.state.endStopId)
        assertEquals(WYNYARD_STOP.name, restored.state.endName)
        assertEquals("", restored.state.query)
        assertEquals(IntroSearchPhase.Idle, restored.state.search)
        assertTrue(restored.state.nearbyStops.isEmpty())
    }

    @Test
    fun `the typed text is not in what is saved`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))
        rig.stopResults.answer = listOf(result(CHATSWOOD_STOP))
        rig.send(IntroUiEvent.QueryChanged("chatswo"))
        pumpOnce(INTRO_SEARCH_DEBOUNCE_MS)

        val saved = rig.handle.get<String>(INTRO_SAVED_STATE_KEY).orEmpty()

        assertFalse("chatswo" in saved || "Chatswood" in saved, saved)
    }

    // endregion

    // region the whole way

    @Test
    fun `two chosen stops reach the confirm screen, and yes saves the trip with the picks and no skip`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(onDestination))

            rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP), IntroUiEvent.ChooseStop(TALLAWONG_STOP))
            rig.send(IntroUiEvent.FinishPressed)

            val trip = rig.sandook.selectAllTrips().single()
            assertEquals(
                listOf(TALLAWONG_STOP.stopId, WYNYARD_STOP.stopId),
                listOf(trip.fromStopId, trip.toStopId),
            )
            assertEquals(false, rig.outcomeEvent().properties?.get("skipped"))
            assertEquals(true, rig.outcomeEvent().properties?.get("savedTrip"))
        }

    @Test
    fun `not now on the confirm screen after two stops saves nothing and is reported as skipped`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onDestination))

        rig.send(IntroUiEvent.ChooseStop(WYNYARD_STOP), IntroUiEvent.ChooseStop(TALLAWONG_STOP))
        rig.send(IntroUiEvent.FinishWithoutTrip)

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals(true, rig.outcomeEvent().properties?.get("skipped"))
        assertEquals(false, rig.outcomeEvent().properties?.get("savedTrip"))
    }

    // endregion
}
