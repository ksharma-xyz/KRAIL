package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rules behind Save as, Go here and Start here on the floating cards, one table per rule. The cases
 * come from `docs/features/map-home/ERD.md`, section 8, the Go here cases the owner accepted as the
 * defaults.
 */
class StopCardActionRulesTest {

    private val home = StopLabel(emoji = "H", label = "Home")
    private val work = StopLabel(emoji = "W", label = "Work")
    private val gym = StopLabel(emoji = "G", label = "Gym")

    private fun set(label: StopLabel, stopId: String) = label.copy(stopId = stopId, stopName = "Stop $stopId")

    // region the words and the role

    @Test
    fun `the field being filled in decides the role`() {
        assertEquals(PickRole.Destination, pickRoleFor(SearchStopFieldType.TO))
        assertEquals(PickRole.Start, pickRoleFor(SearchStopFieldType.FROM))
        assertEquals(PickRole.Choose, pickRoleFor(SearchStopFieldType.LABEL))
    }

    @Test
    fun `a destination reads Go here, a start reads Start here and a label's stop reads Select stop`() {
        assertEquals("Go here", pickActionText(PickRole.Destination))
        assertEquals("Start here", pickActionText(PickRole.Start))
        assertEquals("Select stop", pickActionText(PickRole.Choose))
    }

    @Test
    fun `Search Stop's card says what the rider is doing, with the label's saved name exactly as saved`() {
        assertEquals("Set as destination", contextPickText(PickRole.Destination, labelName = null))
        assertEquals("Set as starting point", contextPickText(PickRole.Start, labelName = null))
        assertEquals("Set as Home", contextPickText(PickRole.Choose, labelName = "Home"))
        assertEquals("Set as Work", contextPickText(PickRole.Choose, labelName = "Work"))
        assertEquals("Set as Mum's  place", contextPickText(PickRole.Choose, labelName = "Mum's  place"))
        // A label name is only read for a label: the fields never take one.
        assertEquals("Set as destination", contextPickText(PickRole.Destination, labelName = "Home"))
    }

    @Test
    fun `a label with no name to read never leaves the action blank`() {
        assertEquals("Set as label", contextPickText(PickRole.Choose, labelName = null))
        assertEquals("Set as label", contextPickText(PickRole.Choose, labelName = "  "))
    }

    @Test
    fun `a stop with an id offers its action even when the stops table knows nothing else of it`() {
        assertTrue(offersPick("200060"))
        assertTrue(offersPick("not-in-any-table"))
        assertFalse(offersPick(""))
        assertFalse(offersPick("  "))
    }

    // endregion

    // region Save as

    @Test
    fun `a stop with no label is offered every label no stop carries, in stored order, outlined`() {
        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(home, work, gym))

        assertEquals(listOf("Home", "Work", "Gym"), pills.map { it.label.label })
        assertTrue(pills.all { it is SaveAsPill.Offered })
        assertEquals(
            listOf("Save as Home", "Save as Work", "Save as Gym"),
            pills.map { (it as SaveAsPill.Offered).text },
        )
    }

    @Test
    fun `a label that sits on another stop is left out and never shown filled`() {
        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(set(home, "999"), work, gym))

        assertEquals(listOf("Work", "Gym"), pills.map { it.label.label })
    }

    @Test
    fun `a stop that already carries a label shows that one pill filled and nothing else`() {
        val pills = saveAsPillsFor("200060", PickRole.Start, listOf(set(home, "200060"), work, gym))

        assertEquals(listOf(SaveAsPill.Saved(set(home, "200060"))), pills)
    }

    @Test
    fun `a custom label on the stop is the one shown`() {
        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(home, work, set(gym, "200060")))

        assertEquals(listOf(SaveAsPill.Saved(set(gym, "200060"))), pills)
    }

    @Test
    fun `two labels on one stop, which only corrupt data holds, shows the first in stored order`() {
        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(set(work, "200060"), set(home, "200060")))

        assertEquals(listOf("Work"), pills.map { it.label.label })
        assertTrue(pills.single() is SaveAsPill.Saved)
    }

    @Test
    fun `a label with a stop id but no name is not set, so it is offered`() {
        val half = home.copy(stopId = "200060", stopName = null)

        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(half))

        assertTrue(pills.single() is SaveAsPill.Offered)
    }

    @Test
    fun `choosing a stop for a label offers no Save as`() {
        assertTrue(saveAsPillsFor("200060", PickRole.Choose, listOf(home, work)).isEmpty())
        assertFalse(offersSaveAs(PickRole.Choose))
        assertTrue(offersSaveAs(PickRole.Destination))
        assertTrue(offersSaveAs(PickRole.Start))
    }

    @Test
    fun `no labels, or a stop with no id, offers no Save as`() {
        assertTrue(saveAsPillsFor("200060", PickRole.Destination, emptyList()).isEmpty())
        assertTrue(saveAsPillsFor("", PickRole.Destination, listOf(home)).isEmpty())
    }

    @Test
    fun `every label set elsewhere and none left to offer leaves the row empty`() {
        val pills = saveAsPillsFor("200060", PickRole.Destination, listOf(set(home, "1"), set(work, "2")))

        assertTrue(pills.isEmpty())
    }

    @Test
    fun `the row keeps the tallest room it has held`() {
        assertEquals(48.dp, reservedRoom(0.dp, 48.dp))
        assertEquals(104.dp, reservedRoom(48.dp, 104.dp))
        assertEquals(104.dp, reservedRoom(104.dp, 48.dp), "A row that got shorter must not shrink the card.")
        assertEquals(104.dp, reservedRoom(104.dp, 0.dp))
    }

    // endregion

    // region the stop Go here sets on a Park & Ride station

    @Test
    fun `go here on a single stop station sets that stop`() {
        assertEquals("200070", goHereStopId(listOf("200070"), setOf("200070", "1")))
    }

    @Test
    fun `go here on a multi stop station sets the smallest stop id the table holds`() {
        assertEquals(
            "2067100",
            goHereStopId(listOf("2067105", "2067100", "2067103"), setOf("2067105", "2067100", "2067103")),
        )
    }

    @Test
    fun `a stop id the table does not hold is never chosen, even the smallest`() {
        assertEquals("2067103", goHereStopId(listOf("2067100", "2067105", "2067103"), setOf("2067105", "2067103")))
    }

    @Test
    fun `go here gives the same stop after the catalogue is reordered`() {
        val table = setOf("19", "20", "21")

        assertEquals(goHereStopId(listOf("21", "19", "20"), table), goHereStopId(listOf("19", "20", "21"), table))
        assertEquals(goHereStopId(listOf("20", "21", "19"), table), goHereStopId(listOf("21", "20", "19"), table))
    }

    @Test
    fun `ids compare as text, the way the stations own id does`() {
        // As numbers 9 is smaller. As text "10" sorts first, and that is the station key's own order.
        assertEquals("10", goHereStopId(listOf("9", "10"), setOf("9", "10")))
    }

    @Test
    fun `a station with no stop in the table has no go here`() {
        assertNull(goHereStopId(listOf("2067100", "2067105"), setOf("1", "2")))
        assertNull(goHereStopId(emptyList(), setOf("1")))
        assertNull(goHereStopId(listOf("2067100"), emptySet()))
    }

    // endregion

    // region the Park & Ride pin card's action

    @Test
    fun `a station with a stop reads Go here when choosing a destination and sets the station`() {
        val action = parkRidePickActionFor("Tallawong Station", "2155384", PickRole.Destination)

        assertEquals(
            PickAction(text = "Go here", stop = StopItem(stopId = "2155384", stopName = "Tallawong Station")),
            action,
        )
    }

    @Test
    fun `the same card in a start search reads Start here`() {
        assertEquals("Start here", parkRidePickActionFor("Tallawong Station", "2155384", PickRole.Start)?.text)
    }

    @Test
    fun `a station with no stop offers no go here`() {
        assertNull(parkRidePickActionFor("Tallawong Station", null, PickRole.Destination))
    }

    @Test
    fun `a blank stop id offers no go here`() {
        assertNull(parkRidePickActionFor("Tallawong Station", "", PickRole.Destination))
    }

    // endregion
}
