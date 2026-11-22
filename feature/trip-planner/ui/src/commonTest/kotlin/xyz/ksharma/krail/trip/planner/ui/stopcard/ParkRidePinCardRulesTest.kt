package xyz.ksharma.krail.trip.planner.ui.stopcard

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Park & Ride pin card's content: every car park of the station at equal weight, each with its
 * own numbers and its own Directions, never the station's. Driven on its own because one real
 * station cannot reach a car park with no reading yet or no position.
 */
class ParkRidePinCardRulesTest {

    private fun station(vararg mappings: ParkRideMapping, position: StationPosition? = StationPosition(-33.6, 150.9)) =
        ParkRideStationPickerItem(
            stationId = "2155384",
            stationName = "Tallawong",
            stopName = "Tallawong Station",
            carParkNames = mappings.map { it.facilityName },
            mappings = mappings.toList(),
            position = position,
        )

    private fun mapping(id: String, name: String = "Tallawong P$id", stop: String = "2155384") =
        ParkRideMapping(stopId = stop, facilityId = id, facilityName = name)

    private fun reading(
        id: String,
        free: Int = 50,
        percent: Int = 80,
        time: String = "8:05 AM",
        readAt: Long = 100L,
        lat: Double? = -33.7,
        lon: Double? = 150.8,
        name: String = "Tallawong P$id",
    ) = ParkRideFacilityDetail(
        spotsAvailable = free,
        totalSpots = 250,
        facilityName = name,
        percentageFull = percent,
        stopId = "2155384",
        timeText = time,
        facilityId = id,
        latitude = lat,
        longitude = lon,
        readAtEpochSeconds = readAt,
    )

    @Test
    fun `the card lists every car park of the station, with a reading or not`() {
        val tile = station(mapping("26"), mapping("27"), mapping("28"))
            .toPinCardTile(listOf(reading("26", free = 120)))
        assertEquals(listOf("26", "27", "28"), tile.carParks.map { it.facilityId })
        assertEquals(120, tile.carParks[0].freeSpots)
        assertNull(tile.carParks[1].freeSpots)
        assertNull(tile.carParks[2].freeSpots)
    }

    @Test
    fun `a car park with no reading shows its name alone and never borrows a position`() {
        val block = station(mapping("27", name = "Tallawong P2")).toPinCardTile(emptyList()).carParks.single()
        assertEquals("Tallawong P2", block.name)
        assertNull(block.freeSpots)
        assertNull(block.percentFull)
        assertNull(block.timeText)
        assertNull(block.directionsTo)
    }

    @Test
    fun `directions go to the car parks own position and never the stations`() {
        val tile = station(mapping("26"), mapping("27"), position = StationPosition(-33.60, 150.90))
            .toPinCardTile(listOf(reading("26", lat = -33.71, lon = 150.81), reading("27", lat = null, lon = null)))
        assertEquals(LatLng(-33.71, 150.81), tile.carParks[0].directionsTo)
        assertNull(tile.carParks[1].directionsTo)
    }

    @Test
    fun `a car park whose reading carried no position has no directions even when the station has one`() {
        val tile = station(mapping("26"), position = StationPosition(-33.60, 150.90))
            .toPinCardTile(listOf(reading("26", lat = 0.0, lon = 0.0)))
        assertNull(tile.carParks.single().directionsTo)
    }

    @Test
    fun `a station with no position of its own still gets directions for a car park that has one`() {
        val tile = station(mapping("26"), position = null)
            .toPinCardTile(listOf(reading("26", lat = -33.71, lon = 150.81)))
        assertEquals(LatLng(-33.71, 150.81), tile.carParks.single().directionsTo)
    }

    @Test
    fun `the total is the free spots of the car parks that have a reading and its time is the oldest`() {
        val tile = station(mapping("26"), mapping("27"), mapping("28"))
            .toPinCardTile(
                listOf(
                    reading("26", free = 120, time = "8:05 AM", readAt = 300L),
                    reading("27", free = 30, time = "8:01 AM", readAt = 100L),
                ),
            )
        assertEquals(150, tile.totalFreeSpots)
        assertEquals("8:01 AM", tile.timeText)
    }

    @Test
    fun `a station with no reading yet has no total and no time`() {
        val tile = station(mapping("26"), mapping("27")).toPinCardTile(emptyList())
        assertNull(tile.totalFreeSpots)
        assertNull(tile.timeText)
    }

    @Test
    fun `car parks keep their order when the numbers change`() {
        val before = station(mapping("28"), mapping("26"), mapping("27"))
            .toPinCardTile(listOf(reading("26", free = 5), reading("27", free = 90), reading("28", free = 40)))
        val after = station(mapping("27"), mapping("28"), mapping("26"))
            .toPinCardTile(listOf(reading("26", free = 400), reading("27", free = 0), reading("28", free = 1)))
        assertEquals(before.carParks.map { it.facilityId }, after.carParks.map { it.facilityId })
        assertEquals(listOf("26", "27", "28"), after.carParks.map { it.facilityId })
    }

    @Test
    fun `a car park behind two stop ids is listed once`() {
        val tile = station(mapping("12", stop = "A"), mapping("12", stop = "B")).toPinCardTile(emptyList())
        assertEquals(listOf("12"), tile.carParks.map { it.facilityId })
    }

    @Test
    fun `a station with no car park has no blocks`() {
        assertEquals(0, station().toPinCardTile(emptyList()).carParks.size)
    }

    @Test
    fun `the card carries the station name and id`() {
        val tile = station(mapping("26")).toPinCardTile(emptyList())
        assertEquals("2155384", tile.stationId)
        assertEquals("Tallawong", tile.stationName)
    }

    @Test
    fun `the action reads Remove from home on home and Add to home off it`() {
        assertEquals("Remove from home", parkRidePinActionLabel(isOnHome = true))
        assertEquals("Add to home", parkRidePinActionLabel(isOnHome = false))
    }
}
