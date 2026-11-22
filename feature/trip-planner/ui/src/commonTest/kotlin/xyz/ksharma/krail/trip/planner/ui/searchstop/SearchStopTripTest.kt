package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.saveable.SaverScope
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The stay in place flow of Search Stop (rules 35, 36 and 39 of `docs/MAP_HOME_UX.md`): a pick fills
 * its bar, editing moves to the other bar only while it is empty, and the go circle is the one way out.
 * One table, so the focus move after every pick is an expectation and not a feel on a device.
 */
class SearchStopTripTest {

    private val central = StopItem(stopId = "200060", stopName = "Central Station")
    private val townHall = StopItem(stopId = "200070", stopName = "Town Hall Station")
    private val wynyard = StopItem(stopId = "200080", stopName = "Wynyard Station")

    private val listingTo = SearchStopFlow(mode = SearchStopMode.List, editing = ToFromField.To)
    private val listingFrom = SearchStopFlow(mode = SearchStopMode.List, editing = ToFromField.From)
    private val mapTo = SearchStopFlow(mode = SearchStopMode.Map, editing = ToFromField.To)

    // region the focus move after a pick

    @Test
    fun `a destination pick with an empty start moves editing to the start and keeps the keyboard up`() {
        val step = reduceTripPick(SearchStopTrip(), listingTo, ToFromField.To, townHall, autoFrom = null)

        assertEquals(SearchStopTrip(to = townHall), step.trip)
        assertEquals(ToFromField.From, step.flow.editing)
        assertEquals(SearchStopMode.List, step.flow.mode, "The keyboard follows the focused bar.")
    }

    @Test
    fun `a destination pick with a start that filled itself in leaves nothing to edit`() {
        val step = reduceTripPick(SearchStopTrip(), listingTo, ToFromField.To, townHall, autoFrom = central)

        assertNull(step.flow.editing, "The nearest stop counts as a filled start.")
        assertEquals(SearchStopMode.Map, step.flow.mode, "No bar to type in, so the keyboard goes.")
        assertEquals(SearchStopTrip(to = townHall), step.trip, "The auto start is not stored as a pick.")
    }

    @Test
    fun `a destination pick with a start already picked leaves nothing to edit`() {
        val held = SearchStopTrip(from = central)
        val step = reduceTripPick(held, listingTo, ToFromField.To, townHall, autoFrom = null)

        assertNull(step.flow.editing)
        assertEquals(SearchStopMode.Map, step.flow.mode)
        assertEquals(SearchStopTrip(from = central, to = townHall), step.trip)
    }

    @Test
    fun `a start pick with an empty destination moves editing to the destination`() {
        val step = reduceTripPick(SearchStopTrip(), listingFrom, ToFromField.From, central, autoFrom = null)

        assertEquals(SearchStopTrip(from = central), step.trip)
        assertEquals(ToFromField.To, step.flow.editing)
        assertEquals(SearchStopMode.List, step.flow.mode)
    }

    @Test
    fun `a start pick with a destination already held leaves nothing to edit`() {
        val held = SearchStopTrip(to = townHall)
        val step = reduceTripPick(held, listingFrom, ToFromField.From, central, autoFrom = null)

        assertNull(step.flow.editing)
        assertEquals(SearchStopMode.Map, step.flow.mode)
        assertEquals(SearchStopTrip(from = central, to = townHall), step.trip)
    }

    @Test
    fun `a pick from the map state keeps the map state while editing moves`() {
        val step = reduceTripPick(SearchStopTrip(), mapTo, ToFromField.To, townHall, autoFrom = null)

        assertEquals(ToFromField.From, step.flow.editing)
        assertEquals(SearchStopMode.Map, step.flow.mode, "A pin card pick does not raise the keyboard over the map.")
    }

    @Test
    fun `a pick replaces the stop already in that bar`() {
        val step = reduceTripPick(
            SearchStopTrip(from = central, to = townHall),
            listingTo,
            ToFromField.To,
            wynyard,
            autoFrom = null,
        )

        assertEquals(SearchStopTrip(from = central, to = wynyard), step.trip)
        assertNull(step.flow.editing)
    }

    @Test
    fun `the opened on field is not moved by a pick`() {
        val opened = listingTo.copy(openedOn = ToFromField.From)

        val step = reduceTripPick(SearchStopTrip(), opened, ToFromField.To, townHall, autoFrom = null)

        assertEquals(ToFromField.From, step.flow.openedOn)
    }

    @Test
    fun `with nothing being edited a pick is for the destination`() {
        assertEquals(ToFromField.To, pickedBarFor(null))
        assertEquals(ToFromField.From, pickedBarFor(ToFromField.From))
        assertEquals(ToFromField.To, pickedBarFor(ToFromField.To))
    }

    // endregion

    // region a start that arrives late

    @Test
    fun `a start that arrives while the map state waits on the start bar finishes the trip`() {
        val waiting = SearchStopFlow(mode = SearchStopMode.Map, editing = ToFromField.From, openedOn = ToFromField.From)

        val settled = settleLateAutoFrom(waiting, SearchStopTrip(to = townHall), autoFrom = central)

        assertNull(settled.editing)
    }

    @Test
    fun `a late start changes nothing while the rider is typing, picked a start, or holds no destination`() {
        val listing = SearchStopFlow(mode = SearchStopMode.List, editing = ToFromField.From)
        val waiting = SearchStopFlow(mode = SearchStopMode.Map, editing = ToFromField.From)

        assertEquals(listing, settleLateAutoFrom(listing, SearchStopTrip(to = townHall), central))
        assertEquals(waiting, settleLateAutoFrom(waiting, SearchStopTrip(from = wynyard, to = townHall), central))
        assertEquals(waiting, settleLateAutoFrom(waiting, SearchStopTrip(), central))
        assertEquals(waiting, settleLateAutoFrom(waiting, SearchStopTrip(to = townHall), autoFrom = null))
    }

    // endregion

    // region the go circle

    @Test
    fun `go leaves with the picked start, else the one that filled itself in`() {
        assertEquals(central to townHall, tripToGo(SearchStopTrip(from = central, to = townHall), autoFrom = wynyard))
        assertEquals(wynyard to townHall, tripToGo(SearchStopTrip(to = townHall), autoFrom = wynyard))
    }

    @Test
    fun `go cannot start without both stops or from a stop to itself`() {
        assertNull(tripToGo(SearchStopTrip(to = townHall), autoFrom = null))
        assertNull(tripToGo(SearchStopTrip(from = central), autoFrom = null))
        assertNull(tripToGo(SearchStopTrip(), autoFrom = central))
        assertNull(tripToGo(SearchStopTrip(to = central), autoFrom = central))
    }

    // endregion

    // region filled bars and saving

    @Test
    fun `a start that filled itself in counts as a filled bar`() {
        assertEquals(emptySet(), SearchStopTrip().filled())
        assertEquals(setOf(ToFromField.From), SearchStopTrip().filled(autoFrom = central))
        assertEquals(setOf(ToFromField.To), SearchStopTrip(to = townHall).filled())
        assertEquals(setOf(ToFromField.To, ToFromField.From), SearchStopTrip(central, townHall).filled())
    }

    @Test
    fun `the saver reads back every trip it wrote`() {
        val address = StopItem("addr1", "1 George St", LocationKind.ADDRESS, "street")
        val trips = listOf(
            SearchStopTrip(),
            SearchStopTrip(from = central),
            SearchStopTrip(to = townHall),
            SearchStopTrip(from = address, to = townHall),
        )
        val scope = SaverScope { true }
        trips.forEach { trip ->
            val saved = with(SearchStopTripSaver) { scope.save(trip) }
            assertEquals(trip, saved?.let(SearchStopTripSaver::restore), "Round trip of $trip")
        }
    }

    // endregion
}
