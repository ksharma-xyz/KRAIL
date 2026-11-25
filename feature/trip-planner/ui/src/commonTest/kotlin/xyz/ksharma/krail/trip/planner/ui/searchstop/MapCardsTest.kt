package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.mutableStateOf
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapCards
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Which card a stop map has asked for, and what a tap on the map itself does with it. */
class MapCardsTest {

    private val askedStation = mutableStateOf<String?>(null)
    private val askedParking = mutableStateOf<String?>(null)

    // Made while nothing is open, as the one whose tap handler the map keeps.
    private val firstGiven = MapCards(
        stationStops = persistentListOf(),
        open = null,
        askedStation = askedStation,
        askedParking = askedParking,
    )

    @Test
    fun `a map tap with nothing asked for is not taken`() {
        assertFalse(firstGiven.closeIfAsked())
    }

    @Test
    fun `a tap handler from before a parking card opened still closes it`() {
        firstGiven.openParkingCard("2155384")

        assertTrue(firstGiven.closeIfAsked())
        assertNull(askedParking.value)
    }

    @Test
    fun `a tap handler from before a station card opened still closes it`() {
        askedStation.value = "2155384"

        assertTrue(firstGiven.closeIfAsked())
        assertNull(askedStation.value)
    }

    @Test
    fun `one card is asked for at a time`() {
        firstGiven.openStationCard("1")
        firstGiven.openParkingCard("2")
        assertEquals(null to "2", askedStation.value to askedParking.value)

        firstGiven.openStationCard("1")
        assertEquals("1" to null, askedStation.value to askedParking.value)
    }
}
