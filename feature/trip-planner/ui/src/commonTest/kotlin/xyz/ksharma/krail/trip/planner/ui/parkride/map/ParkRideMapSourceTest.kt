package xyz.ksharma.krail.trip.planner.ui.parkride.map

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.parkRideStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the stop maps mark with a `P`, and what a station's parking card shows. */
class ParkRideMapSourceTest {

    private val sandook = FakeSandook()
    private val parkRideSandook = FakeNswParkRideSandook()
    private val catalogue = FakeParkRideCatalogue()
    private val loader = FakeParkRideAvailabilityLoader()
    private val platformOps = FakePlatformOps()
    private val source = RealParkRideMapSource(
        catalogue = catalogue,
        sandook = sandook,
        parkRideSandook = parkRideSandook,
        availabilityLoader = loader,
        platformOps = platformOps,
    )

    private fun stop(stopId: String, name: String, vararg productClasses: Int) {
        sandook.insertNswStop(stopId = stopId, stopName = name, stopLat = -33.69, stopLon = 150.9, isParent = null)
        productClasses.forEach { sandook.insertNswStopProductClass(stopId, it) }
    }

    private fun reading(facilityId: String, spotsAvailable: Long) = NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = spotsAvailable,
        totalSpots = 100,
        facilityName = "Name the reading carries",
        percentageFull = 40,
        stopId = "1",
        stopName = "Station",
        timeText = "8:00 AM",
        suburb = "",
        address = "",
        latitude = -33.69,
        longitude = 150.9,
        timestamp = 1,
    )

    @Test
    fun `a station takes its name, place and modes from the stops database`() {
        stop("2155384", "Tallawong Station", 2)
        catalogue.stations = listOf(parkRideStation("2155384"))

        val marked = source.stops().single()

        assertEquals("2155384", marked.stopId)
        assertEquals("Tallawong Station", marked.stopName)
        assertEquals(LatLng(-33.69, 150.9), marked.position)
        assertEquals(listOf(NswTransportConfig.modeFromProductClass(2)), marked.transportModes.toList())
        assertTrue(marked.hasParkAndRide)
    }

    @Test
    fun `a station with several stop ids is marked once, at the first the database knows`() {
        stop("2", "Second Stop", 1)
        stop("3", "Third Stop", 1)
        catalogue.stations = listOf(parkRideStation("1", "2", "3"))

        assertEquals(listOf("2"), source.stops().map { it.stopId })
    }

    @Test
    fun `a station the database knows no stop of is left out`() {
        stop("2155384", "Tallawong Station", 2)
        catalogue.stations = listOf(parkRideStation("999"), parkRideStation("2155384"))

        assertEquals(listOf("2155384"), source.stops().map { it.stopId })
    }

    @Test
    fun `the card has every car park, with the stored numbers and one fetch for that station`() = runTest {
        catalogue.stations = listOf(parkRideStation("1", "2"), parkRideStation("9"))
        parkRideSandook.insertOrReplace(reading("f1", spotsAvailable = 60))

        source.card("2").test {
            var card = awaitItem()
            while (card.isLoading) card = awaitItem()

            assertEquals(listOf("Car park 1", "Car park 2"), card.carParks.map { it.facilityName })
            assertEquals(listOf(60, -1), card.carParks.map { it.spotsAvailable })
            assertEquals("2", card.stationId)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(listOf("f1", "f2")), loader.refreshedMappings.map { call -> call.map { it.facilityId } })
    }

    @Test
    fun `numbers the fetch writes reach the open card`() = runTest {
        catalogue.stations = listOf(parkRideStation("1"))
        loader.onRefresh = { parkRideSandook.insertOrReplace(reading("f1", spotsAvailable = 12)) }

        source.card("1").test {
            var card = awaitItem()
            while (card.isLoading || card.carParks.single().spotsAvailable < 0) card = awaitItem()

            assertEquals(12, card.carParks.single().spotsAvailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed fetch ends the loading and keeps the stored numbers`() = runTest {
        catalogue.stations = listOf(parkRideStation("1"))
        parkRideSandook.insertOrReplace(reading("f1", spotsAvailable = 7))
        loader.onRefresh = { error("no network") }

        source.card("1").test {
            var card = awaitItem()
            while (card.isLoading) card = awaitItem()

            assertFalse(card.isLoading)
            assertEquals(7, card.carParks.single().spotsAvailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a stop with no parking has no card and fetches nothing`() = runTest {
        catalogue.stations = listOf(parkRideStation("1"))

        source.card("999").test { awaitComplete() }

        assertTrue(loader.refreshedMappings.isEmpty())
    }

    @Test
    fun `directions open the phone's maps app at the car park`() {
        source.openDirections(latitude = -33.69, longitude = 150.9, label = "Tallawong P1")

        assertEquals(
            FakePlatformOps.MapDirections(-33.69, 150.9, "Tallawong P1"),
            platformOps.lastMapDirections,
        )
    }
}
