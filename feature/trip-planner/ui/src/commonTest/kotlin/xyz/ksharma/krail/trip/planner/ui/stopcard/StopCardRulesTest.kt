package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rules behind the floating stop card: which parts a stop card and a Park & Ride card have,
 * which stop it is open on, and how big it is. Each is driven on its own at its boundary.
 */
class StopCardRulesTest {

    private fun stop(id: String) = NearbyStopFeature(
        stopId = id,
        stopName = "Stop $id",
        position = LatLng(-33.87, 151.2),
        transportModes = persistentListOf(),
    )

    // region the parts

    private class LayoutCase(
        val name: String,
        val kind: StopCardKind,
        val modes: Int,
        val carParks: Int,
        val expected: StopCardLayout,
    )

    @Test
    fun `a card has the parts its kind and its stop give it`() {
        val cases = listOf(
            LayoutCase("a stop with modes", StopCardKind.Stop, 3, 0, StopCardLayout(true, true, false, true)),
            LayoutCase("a stop with one mode", StopCardKind.Stop, 1, 0, StopCardLayout(true, true, false, true)),
            LayoutCase(
                "a stop with no mode has no mode row",
                StopCardKind.Stop,
                0,
                0,
                StopCardLayout(false, true, false, true),
            ),
            LayoutCase("a stop ignores car parks", StopCardKind.Stop, 2, 3, StopCardLayout(true, true, false, true)),
            LayoutCase(
                "a station of three car parks has a total",
                StopCardKind.ParkRide,
                0,
                3,
                StopCardLayout(false, false, true, true),
            ),
            LayoutCase(
                "a station of two car parks has a total",
                StopCardKind.ParkRide,
                0,
                2,
                StopCardLayout(false, false, true, true),
            ),
            LayoutCase(
                "a station of one car park has no total",
                StopCardKind.ParkRide,
                0,
                1,
                StopCardLayout(false, false, false, true),
            ),
            LayoutCase(
                "a station of none has no total",
                StopCardKind.ParkRide,
                0,
                0,
                StopCardLayout(false, false, false, true),
            ),
            LayoutCase(
                "a station ignores modes",
                StopCardKind.ParkRide,
                4,
                2,
                StopCardLayout(false, false, true, true),
            ),
        )
        cases.forEach { case ->
            assertEquals(case.expected, stopCardLayout(case.kind, case.modes, case.carParks), case.name)
        }
    }

    @Test
    fun `every card keeps room for the actions a later branch fills`() {
        StopCardKind.entries.forEach { kind ->
            assertTrue(stopCardLayout(kind, modeCount = 1, carParkCount = 1).showsActionSlot, kind.name)
        }
    }

    // endregion

    // region which stop

    @Test
    fun `the open stop is looked up again in the stops the map is drawing`() {
        val stops = listOf(stop("1"), stop("2"), stop("3"))
        assertEquals("2", nearbyStopFor("2", stops)?.stopId)
        assertEquals("1", nearbyStopFor("1", stops)?.stopId)
    }

    @Test
    fun `no stop open or a stop that left the map leaves the card closed`() {
        val stops = listOf(stop("1"), stop("2"))
        assertNull(nearbyStopFor(null, stops))
        assertNull(nearbyStopFor("9", stops))
        assertNull(nearbyStopFor("1", emptyList()))
    }

    @Test
    fun `a card's state is kept per stop and per kind`() {
        assertEquals("Stop:200060", stopCardStateKey(StopCardKind.Stop, "200060"))
        assertFalse(stopCardStateKey(StopCardKind.Stop, "1") == stopCardStateKey(StopCardKind.Stop, "2"))
        assertFalse(stopCardStateKey(StopCardKind.Stop, "1") == stopCardStateKey(StopCardKind.ParkRide, "1"))
        assertEquals(stopCardStateKey(StopCardKind.Stop, "1"), stopCardStateKey(StopCardKind.Stop, "1"))
    }

    // endregion

    // region size

    @Test
    fun `the card is one column wide less its margins, at most 560 and the pane when narrower`() {
        assertEquals(288.dp, stopCardWidth(320.dp))
        assertEquals(379.dp, stopCardWidth(411.dp))
        assertEquals(448.dp, stopCardWidth(480.dp))
        assertEquals(528.dp, stopCardWidth(560.dp))
        assertEquals(528.dp, stopCardWidth(700.dp))
        assertEquals(528.dp, stopCardWidth(1200.dp))
    }

    @Test
    fun `a pane with no room gets a card with no width and never a negative one`() {
        assertEquals(0.dp, stopCardWidth(0.dp))
        assertEquals(0.dp, stopCardWidth((-5).dp))
        assertEquals(0.dp, stopCardWidth(20.dp))
        assertEquals(0.dp, stopCardWidth(32.dp))
    }

    @Test
    fun `the card stops short of the form above it and the navigation bar below it`() {
        // 800 pane, 200 form, 48 bar, less the 16 edge and the 8 gap.
        assertEquals(528.dp, stopCardMaxHeight(paneHeight = 800.dp, topReserve = 200.dp, bottomInset = 48.dp))
        assertEquals(776.dp, stopCardMaxHeight(paneHeight = 800.dp, topReserve = 0.dp, bottomInset = 0.dp))
    }

    @Test
    fun `a pane too short for the card keeps the card at its floor`() {
        assertEquals(120.dp, stopCardMaxHeight(paneHeight = 200.dp, topReserve = 180.dp, bottomInset = 48.dp))
        assertEquals(120.dp, stopCardMaxHeight(paneHeight = 0.dp, topReserve = 0.dp, bottomInset = 0.dp))
        assertEquals(120.dp, stopCardMaxHeight(paneHeight = (-10).dp, topReserve = 0.dp, bottomInset = 0.dp))
    }

    @Test
    fun `the map controls sit above an open card and stay put with none`() {
        assertEquals(0.dp, controlsClearance(0.dp))
        assertEquals(0.dp, controlsClearance((-1).dp))
        assertEquals(308.dp, controlsClearance(300.dp))
    }

    @Test
    fun `a host that hides its controls is never given a clearance, and every other host keeps the lift`() {
        assertEquals(0.dp, controlsClearanceFor(cardHeight = 300.dp, cardOpen = true, controlsHide = true))
        assertEquals(0.dp, controlsClearanceFor(cardHeight = 300.dp, cardOpen = false, controlsHide = false))
        assertEquals(308.dp, controlsClearanceFor(cardHeight = 300.dp, cardOpen = true, controlsHide = false))
    }

    @Test
    fun `back shuts an expanded board first, then dismisses the card, then is the screen's`() {
        assertEquals(StopCardBackStep.CollapseBoard, stopCardBackStepFor(cardOpen = true, boardExpanded = true, covered = false))
        assertEquals(StopCardBackStep.DismissCard, stopCardBackStepFor(cardOpen = true, boardExpanded = false, covered = false))
        assertEquals(StopCardBackStep.Screen, stopCardBackStepFor(cardOpen = false, boardExpanded = false, covered = false))
        // A covered map has no card the rider can see, so back is the screen's whatever the board was doing.
        assertEquals(StopCardBackStep.Screen, stopCardBackStepFor(cardOpen = true, boardExpanded = true, covered = true))
        assertEquals(StopCardBackStep.Screen, stopCardBackStepFor(cardOpen = false, boardExpanded = true, covered = false))
    }

    @Test
    fun `the card keeps all of the form's height clear with the box in place and none with it gone`() {
        assertEquals(176.dp, stopCardTopReserveFor(headerHeight = 176.dp, slide = 0f))
        assertEquals(88.dp, stopCardTopReserveFor(headerHeight = 176.dp, slide = 0.5f))
        assertEquals(0.dp, stopCardTopReserveFor(headerHeight = 176.dp, slide = 1f))
        assertEquals(176.dp, stopCardTopReserveFor(headerHeight = 176.dp, slide = -1f), "never more than the form's height")
        assertEquals(0.dp, stopCardTopReserveFor(headerHeight = 176.dp, slide = 2f), "and never less than none")
    }

    // endregion
}
