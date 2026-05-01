package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [StopDisplay] and its converter extensions.
 *
 * The resolver is the single source of truth for "label vs name" across every
 * surface that renders a stop, so exhaustively cover the combinations: missing
 * labels, unset labels, matching labels, multiple matches (data-corruption
 * case), and verbatim text preservation.
 *
 * Lives under feature/trip-planner/ui/src/commonTest because the state module
 * doesn't have its own test infra wired up yet (same arrangement as
 * [StopLabelTest]).
 */
class StopDisplayTest {

    // region StopItem.toDisplay

    @Test
    fun `StopItem toDisplay - empty labels returns name only`() {
        val display = central.toDisplay(emptyList())

        assertEquals("200060", display.stopId)
        assertEquals("Central Station", display.name)
        assertNull(display.label)
    }

    @Test
    fun `StopItem toDisplay - matching set label returns label`() {
        val display = central.toDisplay(listOf(homeLabel, workLabel))

        assertEquals("Home", display.label)
        assertEquals("Central Station", display.name)
        assertEquals("200060", display.stopId)
    }

    @Test
    fun `StopItem toDisplay - unset label is ignored even with matching string fields`() {
        // unsetGym has no stopId so isSet is false; it must not be picked up.
        val display = central.toDisplay(listOf(unsetGym))

        assertNull(display.label)
    }

    @Test
    fun `StopItem toDisplay - non-matching label returns null`() {
        val display = central.toDisplay(listOf(workLabel))

        assertNull(display.label)
    }

    @Test
    fun `StopItem toDisplay - duplicate matching labels - first wins`() {
        val secondHome = StopLabel(
            emoji = "🏠",
            label = "Other Home",
            stopId = "200060",
            stopName = "Central Station",
        )
        val display = central.toDisplay(listOf(homeLabel, secondHome))

        assertEquals("Home", display.label)
    }

    @Test
    fun `StopItem toDisplay - whitespace and casing preserved verbatim`() {
        val weird = StopLabel(
            emoji = "🌟",
            label = "  My Place  ",
            stopId = "200060",
            stopName = "Central Station",
        )
        val display = central.toDisplay(listOf(weird))

        assertEquals("  My Place  ", display.label)
    }

    // endregion
    // region Trip.fromStopDisplay / Trip.toStopDisplay

    @Test
    fun `Trip displays - both stops labelled`() {
        val labels = listOf(homeLabel, workLabel)

        val from = centralToTownHall.fromStopDisplay(labels)
        val to = centralToTownHall.toStopDisplay(labels)

        assertEquals("200060", from.stopId)
        assertEquals("Central Station", from.name)
        assertEquals("Home", from.label)

        assertEquals("200070", to.stopId)
        assertEquals("Town Hall Station", to.name)
        assertEquals("Work", to.label)
    }

    @Test
    fun `Trip displays - only from labelled`() {
        val labels = listOf(homeLabel)

        assertEquals("Home", centralToTownHall.fromStopDisplay(labels).label)
        assertNull(centralToTownHall.toStopDisplay(labels).label)
    }

    @Test
    fun `Trip displays - only to labelled`() {
        val labels = listOf(workLabel)

        assertNull(centralToTownHall.fromStopDisplay(labels).label)
        assertEquals("Work", centralToTownHall.toStopDisplay(labels).label)
    }

    @Test
    fun `Trip displays - neither labelled`() {
        val labels = emptyList<StopLabel>()

        assertNull(centralToTownHall.fromStopDisplay(labels).label)
        assertNull(centralToTownHall.toStopDisplay(labels).label)
    }

    @Test
    fun `Trip displays - unset labels mixed with set labels`() {
        // The unset Gym must not match anything; the set Home label still applies.
        val labels = listOf(unsetGym, homeLabel)

        assertEquals("Home", centralToTownHall.fromStopDisplay(labels).label)
        assertNull(centralToTownHall.toStopDisplay(labels).label)
    }

    // endregion
    // region fixtures

    private val central = StopItem(stopId = "200060", stopName = "Central Station")

    private val centralToTownHall = Trip(
        fromStopId = "200060",
        fromStopName = "Central Station",
        toStopId = "200070",
        toStopName = "Town Hall Station",
    )

    private val homeLabel = StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "200060",
        stopName = "Central Station",
    )

    private val workLabel = StopLabel(
        emoji = "💼",
        label = "Work",
        stopId = "200070",
        stopName = "Town Hall Station",
    )

    private val unsetGym = StopLabel(emoji = "🏋", label = "Gym")

    // endregion
}
