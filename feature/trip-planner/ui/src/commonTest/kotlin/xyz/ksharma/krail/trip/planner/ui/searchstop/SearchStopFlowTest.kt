package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Search Stop's two states and two step back (`docs/MAP_HOME_UX.md` rule 39) as a table, so the
 * walk from open to home is one expectation per move and not a feel on a device.
 */
class SearchStopFlowTest {

    private val opening = initialSearchStopFlow(SearchStopFieldType.TO, editTripLeg = false)

    private fun SearchStopFlow.after(vararg events: SearchStopFlowEvent): SearchStopStep =
        events.fold(SearchStopStep(this)) { step, event -> reduceSearchStopFlow(step.flow, event) }

    // region opening

    @Test
    fun `it opens as the map state, editing the field the route names`() {
        val to = initialSearchStopFlow(SearchStopFieldType.TO, editTripLeg = false)
        val from = initialSearchStopFlow(SearchStopFieldType.FROM, editTripLeg = false)

        assertEquals(SearchStopMode.Map, to.mode)
        assertEquals(SearchStopMode.Map, from.mode)
        assertEquals(ToFromField.To, to.editing)
        assertEquals(ToFromField.From, from.editing)
        assertEquals(from.editing, from.openedOn)
    }

    @Test
    fun `a label or one leg of a trip edits the one bar`() {
        assertEquals(ToFromField.To, initialSearchStopFlow(SearchStopFieldType.LABEL, false).editing)
        assertEquals(ToFromField.To, initialSearchStopFlow(SearchStopFieldType.FROM, editTripLeg = true).editing)
        assertEquals(ToFromField.To, initialSearchStopFlow(SearchStopFieldType.TO, editTripLeg = true).editing)
    }

    @Test
    fun `a form opened with a stop held edits the bar that is still empty`() {
        val central = StopItem(stopId = "200060", stopName = "Central Station")
        val townHall = StopItem(stopId = "200070", stopName = "Town Hall Station")

        val toHeld = initialSearchStopFlow(SearchStopFieldType.TO, false, SearchStopTrip(to = central))
        assertEquals(ToFromField.From, toHeld.editing, "A held destination leaves the start to choose.")
        assertEquals(ToFromField.From, toHeld.openedOn)

        val fromOpenedToHeld = initialSearchStopFlow(SearchStopFieldType.FROM, false, SearchStopTrip(to = central))
        assertEquals(ToFromField.From, fromOpenedToHeld.editing)

        val fromHeld = initialSearchStopFlow(SearchStopFieldType.TO, false, SearchStopTrip(from = central))
        assertEquals(ToFromField.To, fromHeld.editing)

        val both = initialSearchStopFlow(SearchStopFieldType.TO, false, SearchStopTrip(from = central, to = townHall))
        assertNull(both.editing, "Both held, nothing to edit.")
        assertEquals(SearchStopMode.Map, both.mode)
    }

    @Test
    fun `a single bar ignores a held stop`() {
        val central = StopItem(stopId = "200060", stopName = "Central Station")

        val flow = initialSearchStopFlow(SearchStopFieldType.LABEL, false, SearchStopTrip(to = central))

        assertEquals(ToFromField.To, flow.editing)
    }

    // endregion

    // region the table

    @Test
    fun `tapping the bar raises the list state and keeps the field`() {
        val step = opening.after(SearchStopFlowEvent.BarTapped)

        assertEquals(SearchStopMode.List, step.flow.mode)
        assertEquals(opening.editing, step.flow.editing)
        assertFalse(step.exit)
    }

    @Test
    fun `tapping the other bar edits it in place and raises the list state`() {
        val step = opening.after(SearchStopFlowEvent.FieldTapped(ToFromField.From))

        assertEquals(SearchStopMode.List, step.flow.mode)
        assertEquals(ToFromField.From, step.flow.editing)
        assertEquals(ToFromField.To, step.flow.openedOn, "The field the screen opened on does not move.")
    }

    @Test
    fun `a tap on the map is the first step back and does not leave`() {
        val step = opening.after(SearchStopFlowEvent.BarTapped, SearchStopFlowEvent.MapTapped)

        assertEquals(SearchStopMode.Map, step.flow.mode)
        assertFalse(step.exit)
    }

    @Test
    fun `a tap on the map while already on the map changes nothing`() {
        val step = opening.after(SearchStopFlowEvent.MapTapped)

        assertEquals(opening, step.flow)
        assertFalse(step.exit)
    }

    @Test
    fun `back from the list state goes to the map state and does not leave`() {
        val step = opening.after(SearchStopFlowEvent.BarTapped, SearchStopFlowEvent.BackPressed())

        assertEquals(SearchStopMode.Map, step.flow.mode)
        assertFalse(step.exit, "The first back never leaves the screen.")
    }

    @Test
    fun `back again from the map state goes home`() {
        val step = opening.after(
            SearchStopFlowEvent.BarTapped,
            SearchStopFlowEvent.BackPressed(),
            SearchStopFlowEvent.BackPressed(),
        )

        assertTrue(step.exit, "The second back leaves.")
        assertEquals(SearchStopMode.Map, step.flow.mode)
    }

    @Test
    fun `back on a fresh map state leaves at once`() {
        assertTrue(opening.after(SearchStopFlowEvent.BackPressed()).exit)
    }

    @Test
    fun `back from a start field the rider switched to returns to the field the screen opened on`() {
        val step = opening.after(
            SearchStopFlowEvent.FieldTapped(ToFromField.From),
            SearchStopFlowEvent.BackPressed(),
        )

        assertEquals(SearchStopMode.Map, step.flow.mode)
        assertEquals(ToFromField.To, step.flow.editing, "Back lands on To and From with the destination kept.")
        assertFalse(step.exit)
    }

    @Test
    fun `back from the list state lands on a bar that still needs a stop`() {
        val listing = opening.after(SearchStopFlowEvent.BarTapped).flow

        // To was picked, so back lands on From even though the screen opened on To.
        val toPicked = reduceSearchStopFlow(listing, SearchStopFlowEvent.BackPressed(setOf(ToFromField.To)))
        assertEquals(ToFromField.From, toPicked.flow.editing)
        assertFalse(toPicked.exit)

        // Both bars hold a stop, so back lands on nothing and the go circle shows.
        val bothPicked = reduceSearchStopFlow(
            listing,
            SearchStopFlowEvent.BackPressed(setOf(ToFromField.To, ToFromField.From)),
        )
        assertNull(bothPicked.flow.editing)
        assertEquals(SearchStopMode.Map, bothPicked.flow.mode)
        assertFalse(bothPicked.exit)
    }

    @Test
    fun `with nothing being edited back leaves at once and a map tap changes nothing`() {
        val nothing = SearchStopFlow(mode = SearchStopMode.Map, editing = null)

        assertEquals(nothing, nothing.after(SearchStopFlowEvent.MapTapped).flow)
        assertTrue(nothing.after(SearchStopFlowEvent.BackPressed()).exit)
    }

    @Test
    fun `back from a start field the screen opened on keeps that field`() {
        val from = initialSearchStopFlow(SearchStopFieldType.FROM, editTripLeg = false)
        val step = from.after(SearchStopFlowEvent.BarTapped, SearchStopFlowEvent.BackPressed())

        assertEquals(ToFromField.From, step.flow.editing)
        assertFalse(step.exit)
    }

    @Test
    fun `sending a trip saves the screen as the map state with nothing edited, from the list state too`() {
        val editingTo = SearchStopFlow(mode = SearchStopMode.List, editing = ToFromField.To, openedOn = ToFromField.To)

        val step = editingTo.after(SearchStopFlowEvent.TripSent)

        assertEquals(SearchStopMode.Map, step.flow.mode, "Back from the results must not raise the keyboard.")
        assertNull(step.flow.editing, "Both bars are set, so nothing is being edited and the go circle shows.")
        assertFalse(step.exit, "Sending a trip leaves through the entry, not through this table.")
    }

    @Test
    fun `back from the results lands on this state, and back from there leaves with nothing`() {
        val sent = opening.after(SearchStopFlowEvent.TripSent).flow

        // The rider is back on Search Stop. One back press leaves, so no result is sent and the half pick rule holds.
        assertEquals(sent, sent.after(SearchStopFlowEvent.MapTapped).flow)
        assertTrue(sent.after(SearchStopFlowEvent.BackPressed()).exit)
    }

    // endregion

    // region maps off

    @Test
    fun `with no map to open on the list is the state that draws, and the saved state stays the map state`() {
        val drawn = effectiveFlow(opening, mapsAvailable = false)

        assertEquals(SearchStopMode.List, drawn.mode)
        assertEquals(SearchStopMode.Map, opening.mode, "Saved state is untouched so back still leaves in one step.")
        assertTrue(opening.after(SearchStopFlowEvent.BackPressed()).exit)
    }

    @Test
    fun `with nothing being edited the state is never forced to the list`() {
        val nothing = SearchStopFlow(mode = SearchStopMode.Map, editing = null)

        assertEquals(nothing, effectiveFlow(nothing, mapsAvailable = false))
    }

    @Test
    fun `with a map the saved state is the state that draws`() {
        assertEquals(opening, effectiveFlow(opening, mapsAvailable = true))
        val listing = opening.after(SearchStopFlowEvent.BarTapped).flow
        assertEquals(listing, effectiveFlow(listing, mapsAvailable = true))
    }

    // endregion

    // region saving

    @Test
    fun `the saver reads back every flow it wrote`() {
        val saver = SearchStopFlowSaver
        val scope = androidx.compose.runtime.saveable.SaverScope { true }
        val flows = SearchStopMode.entries.flatMap { mode ->
            (ToFromField.entries + null).flatMap { editing ->
                ToFromField.entries.map { openedOn -> SearchStopFlow(mode, editing, openedOn) }
            }
        }
        flows.forEach { flow ->
            val saved = with(saver) { scope.save(flow) }
            assertEquals(flow, saved?.let(saver::restore), "Round trip of $flow")
        }
        assertEquals(flows.size, 12)
    }

    @Test
    fun `a saved string that is not three known names restores the opening flow`() {
        assertEquals(SearchStopFlow(), SearchStopFlowSaver.restore("garbage"))
        assertEquals(SearchStopFlow(), SearchStopFlowSaver.restore(""))
    }

    // endregion
}
