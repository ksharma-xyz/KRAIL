package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStopRowsTest {

    private val suggested = DEFAULT_SUGGESTED_STOPS
    private val nearby = persistentListOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP)

    private val onDestination = IntroState(step = IntroStep.DESTINATION)
    private val onOrigin = IntroState(step = IntroStep.ORIGIN)

    private fun IntroState.chosenEnd(stop: IntroStop) = copy(endStopId = stop.stopId, endName = stop.name)
    private fun IntroState.chosenStart(stop: IntroStop) = copy(startStopId = stop.stopId, startName = stop.name)

    private fun results(vararg stops: IntroStop) = IntroSearchPhase.Results(persistentListOf(*stops))

    // region the rows of each screen

    @Test
    fun `the destination screen lists the curated ends and the origin screen the nearby stations then the curated starts`() {
        assertEquals(
            listOf(CENTRAL_STOP, WYNYARD_STOP, PARRAMATTA_STOP, TOWN_HALL_STOP, MARTIN_PLACE_STOP),
            onDestination.stopRows(suggested),
        )
        assertEquals(
            listOf(TALLAWONG_STOP, CENTRAL_STOP, SCHOFIELDS_STOP, PARRAMATTA_STOP, TOWN_HALL_STOP),
            onOrigin.stopRows(suggested),
            "no fix, the curated starts",
        )
        assertEquals(
            listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP, TALLAWONG_STOP, CENTRAL_STOP),
            onOrigin.copy(nearbyStops = nearby).stopRows(suggested),
        )
    }

    @Test
    fun `the destination list ignores the nearby stations`() {
        assertEquals(onDestination.stopRows(suggested), onDestination.copy(nearbyStops = nearby).stopRows(suggested))
    }

    @Test
    fun `the other screens have no rows`() {
        listOf(IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.CONFIRM).forEach { step ->
            assertEquals(emptyList(), IntroState(step = step).stopRows(suggested), "$step")
            assertNull(IntroState(step = step).highlightedStop(suggested), "$step")
        }
    }

    @Test
    fun `the stop chosen for the destination is not offered as the origin, and the other way round`() {
        val originRows = onOrigin.copy(nearbyStops = nearby).chosenEnd(CENTRAL_STOP).stopRows(suggested)
        val destinationRows = onDestination.chosenStart(TALLAWONG_STOP).stopRows(suggested)

        assertFalse(CENTRAL_STOP in originRows, "the destination is not a start: $originRows")
        assertEquals(5, originRows.size, "the list is still five long, made up from the curated starts")
        assertFalse(TALLAWONG_STOP in destinationRows)
        assertEquals(onDestination.stopRows(suggested), destinationRows, "Tallawong is not an end, so nothing moves")
    }

    @Test
    fun `a station with the same name as the other end is left out too`() {
        val sameStationOtherStop = IntroStop("999", "Redfern Station, Platform 1")

        val rows = onOrigin.copy(nearbyStops = nearby).chosenEnd(sameStationOtherStop).stopRows(suggested)

        assertFalse(rows.any { it.name.startsWith("Redfern Station") }, "$rows")
    }

    @Test
    fun `back on a screen the rider's earlier choice leads the list and is the highlighted row`() {
        val comeBack = onDestination.chosenEnd(WYNYARD_STOP)

        assertEquals(
            listOf(WYNYARD_STOP, CENTRAL_STOP, PARRAMATTA_STOP, TOWN_HALL_STOP, MARTIN_PLACE_STOP),
            comeBack.stopRows(suggested),
        )
        assertEquals(WYNYARD_STOP, comeBack.highlightedStop(suggested))
    }

    @Test
    fun `an earlier choice that was found by search leads the list and pushes the last row off`() {
        val found = IntroStop("2000169", "Circular Quay Station")
        val comeBack = onDestination.chosenEnd(found)

        val rows = comeBack.stopRows(suggested)

        assertEquals(found, rows.first())
        assertEquals(5, rows.size)
        assertFalse(MARTIN_PLACE_STOP in rows, "the fifth suggestion made way for the choice")
    }

    @Test
    fun `back on the origin screen the earlier start leads the list`() {
        val comeBack = onOrigin.copy(nearbyStops = nearby).chosenStart(MUSEUM_STOP).chosenEnd(CENTRAL_STOP)

        assertEquals(MUSEUM_STOP, comeBack.highlightedStop(suggested))
        assertEquals(1, comeBack.stopRows(suggested).count { it == MUSEUM_STOP })
    }

    // endregion

    // region the typed search

    @Test
    fun `typed results replace the suggestions, and the first result is the highlighted one`() {
        val typed = onDestination.copy(query = "ch", search = results(CHATSWOOD_STOP, JUNIORS_KINGSFORD))

        assertEquals(listOf(CHATSWOOD_STOP, JUNIORS_KINGSFORD), typed.stopRows(suggested))
        assertEquals(CHATSWOOD_STOP, typed.highlightedStop(suggested))
    }

    @Test
    fun `typed results leave out the stop chosen for the other end`() {
        val typed = onOrigin.chosenEnd(CHATSWOOD_STOP).copy(search = results(CHATSWOOD_STAND_A, REDFERN_STOP))

        assertEquals(listOf(REDFERN_STOP), typed.stopRows(suggested))
        assertEquals(REDFERN_STOP, typed.highlightedStop(suggested))
    }

    @Test
    fun `typed results are not moved by the rider's earlier choice`() {
        val typed = onDestination.chosenEnd(WYNYARD_STOP).copy(search = results(CHATSWOOD_STOP, REDFERN_STOP))

        assertEquals(listOf(CHATSWOOD_STOP, REDFERN_STOP), typed.stopRows(suggested))
    }

    @Test
    fun `no match is no rows and nothing is highlighted, so the right button has nothing to take`() {
        val none = onDestination.copy(query = "zzzz", search = IntroSearchPhase.NoMatch)

        assertEquals(emptyList(), none.stopRows(suggested))
        assertNull(none.highlightedStop(suggested))
    }

    @Test
    fun `an idle search shows the suggestions, which is also what a stops table that is not ready gives`() {
        val idle = onDestination.copy(query = "cen", search = IntroSearchPhase.Idle)

        assertEquals(onDestination.stopRows(suggested), idle.stopRows(suggested))
    }

    // endregion

    // region choosing a stop

    @Test
    fun `choosing on the destination screen sets the end and moves to the origin screen`() {
        val chosen = onDestination.withStopChosen(CENTRAL_STOP)

        assertEquals("200060", chosen.endStopId)
        assertEquals("Central Station", chosen.endName)
        assertNull(chosen.startStopId)
        assertEquals(IntroStep.ORIGIN, chosen.step)
    }

    @Test
    fun `choosing on the origin screen sets the start and moves to the confirm screen`() {
        val chosen = onOrigin.chosenEnd(CENTRAL_STOP).withStopChosen(REDFERN_STOP)

        assertEquals("201510", chosen.startStopId)
        assertEquals("Redfern Station", chosen.startName)
        assertEquals("200060", chosen.endStopId)
        assertEquals(IntroStep.CONFIRM, chosen.step)
        assertTrue(chosen.hasTrip)
    }

    @Test
    fun `with a car park as the start the destination goes straight to the confirm screen`() {
        val card = ParkRideCardPhase.Card(ParkRideCardStation(stationId = "2155384", stationName = "Tallawong"))
        val parkRideStart = onDestination.copy(
            parkRideAnswer = ParkRideAnswer.YES,
            parkRideCard = card,
            addedParkRide = true,
            startStopId = TALLAWONG_STOP.stopId,
            startName = TALLAWONG_STOP.name,
            startFromParkRide = true,
        )

        val chosen = parkRideStart.withStopChosen(CENTRAL_STOP)

        assertEquals(IntroStep.CONFIRM, chosen.step)
        assertTrue(chosen.hasTrip, "the start is the car park and the end is the pick")
        assertEquals(TALLAWONG_STOP.stopId, chosen.startStopId)
    }

    @Test
    fun `picking the car park station as the destination clears the start and shows the origin screen`() {
        val parkRideStart = onDestination.copy(
            addedParkRide = true,
            startStopId = TALLAWONG_STOP.stopId,
            startName = TALLAWONG_STOP.name,
            startFromParkRide = true,
        )

        val chosen = parkRideStart.withStopChosen(TALLAWONG_STOP)

        assertNull(chosen.startStopId)
        assertFalse(chosen.startFromParkRide)
        assertEquals(
            IntroStep.ORIGIN,
            chosen.step,
            "a trip is never from a stop to itself, so the start is asked again",
        )
    }

    @Test
    fun `choosing drops what was typed and found`() {
        val typed = onDestination.copy(query = "chat", search = results(CHATSWOOD_STOP))

        val chosen = typed.withStopChosen(CHATSWOOD_STOP)

        assertEquals("", chosen.query)
        assertEquals(IntroSearchPhase.Idle, chosen.search)
    }

    @Test
    fun `a stop from the typed results counts as a search, and one from the suggestions does not`() {
        val typed = onDestination.copy(query = "chat", search = results(CHATSWOOD_STOP, REDFERN_STOP))

        assertTrue(typed.withStopChosen(CHATSWOOD_STOP).usedSearch)
        assertTrue(typed.withStopChosen(REDFERN_STOP).usedSearch)
        assertFalse(onDestination.withStopChosen(CENTRAL_STOP).usedSearch)
        // A suggestion tapped while typed results show cannot happen, but one that is not among them is no search.
        assertFalse(typed.withStopChosen(CENTRAL_STOP).usedSearch)
    }

    @Test
    fun `once a search was used the flag stays set for the rest of the Intro`() {
        val used = onDestination.copy(usedSearch = true)

        assertTrue(used.withStopChosen(CENTRAL_STOP).usedSearch)
    }

    @Test
    fun `choosing on any other screen changes nothing`() {
        listOf(IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.CONFIRM).forEach { step ->
            val state = IntroState(step = step)
            assertEquals(state, state.withStopChosen(CENTRAL_STOP), "$step")
        }
    }

    @Test
    fun `any two chosen stops reach the confirm screen with a complete trip`() {
        // Every pair of the curated ends and starts, the real lists, in the order a rider meets the screens.
        suggested.end.map { it.toIntroStop() }.forEach { end ->
            suggested.start.map { it.toIntroStop() }.filterNot { it.isSameStopAs(end) }.forEach { start ->
                val atConfirm = onDestination.withStopChosen(end).withStopChosen(start)

                assertEquals(IntroStep.CONFIRM, atConfirm.step, "$end to $start")
                assertTrue(atConfirm.hasTrip, "$end to $start")
            }
        }
    }

    // endregion
}
