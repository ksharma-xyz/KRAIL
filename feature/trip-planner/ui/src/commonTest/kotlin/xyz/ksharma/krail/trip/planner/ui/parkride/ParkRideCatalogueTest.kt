package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Direct coverage of the catalogue, rather than only reaching it through the ViewModel.
 *
 * The grouping it performs is the part most likely to break silently when the Remote Config
 * list changes shape, so it is worth testing where it lives.
 */
class ParkRideCatalogueTest {

    private val facilityManager = FakeParkRideFacilityManager()
    private val sandook = FakeSandook()

    private val catalogue = RealParkRideCatalogue(
        nswParkRideFacilityManager = facilityManager,
        stopResultsManager = FakeStopResultsManager(),
        sandook = sandook,
        festivalManager = FakeFestivalManager(),
    )

    @Test
    fun `several car parks behind one stop collapse to one station`() {
        facilityManager.facilities = listOf(
            NswParkRideFacility("2155384", "26", "Park&Ride - Tallawong P1"),
            NswParkRideFacility("2155384", "27", "Park&Ride - Tallawong P2"),
            NswParkRideFacility("2155384", "28", "Park&Ride - Tallawong P3"),
        )

        val stations = catalogue.stations()

        assertEquals(1, stations.size)
        // The shared prefix names the station, with the trailing partial word dropped.
        assertEquals("Tallawong", stations.first().stationName)
        assertEquals(3, stations.first().carParkCount)
    }

    @Test
    fun `one car park behind several stops collapses to one station`() {
        facilityManager.facilities = listOf(
            NswParkRideFacility("210318", "12", "Park&Ride - Mona Vale"),
            NswParkRideFacility("2103108", "12", "Park&Ride - Mona Vale"),
        )

        val stations = catalogue.stations()

        assertEquals(1, stations.size)
        assertEquals("Mona Vale", stations.first().stationName)
        // Both mappings are kept, so adding the station stores every pair.
        assertEquals(2, stations.first().mappings.size)
    }

    @Test
    fun `stations are sorted by name and carry local coordinates`() {
        facilityManager.facilities = listOf(
            NswParkRideFacility("221210", "9", "Park&Ride - Revesby"),
            NswParkRideFacility("213110", "486", "Park&Ride - Ashfield"),
        )
        sandook.insertNswStop(
            stopId = "213110",
            stopName = "Ashfield Station",
            stopLat = -33.8886,
            stopLon = 151.1256,
            isParent = null,
        )

        val stations = catalogue.stations()

        assertEquals(listOf("Ashfield", "Revesby"), stations.map { it.stationName })
        assertEquals(-33.8886, stations.first().position?.latitude)
        // A stop the local table does not know is simply not plotted.
        assertNull(stations.last().position)
    }

    @Test
    fun `an empty remote config yields no stations`() {
        facilityManager.facilities = emptyList()

        assertTrue(catalogue.stations().isEmpty())
    }

    @Test
    fun `loading emoji comes with a greeting`() {
        val emoji = catalogue.loadingEmoji()

        assertTrue(emoji.emoji.isNotBlank())
        assertTrue(emoji.greeting.isNotBlank())
    }
}
