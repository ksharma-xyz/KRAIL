package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rules behind a Park & Ride tile on home: what a station's total is, which time it shows,
 * where Directions go, how wide a tile is and which tile is open. Each is driven on its own at
 * the boundary, because a single real reading cannot reach a car park with no number yet, no
 * position or no time.
 */
class ParkRideTileTest {

    private fun car(
        id: String,
        name: String = "Car park $id",
        free: Int = 10,
        total: Int = 100,
        percent: Int = 90,
        time: String = "8:00 AM",
        readAt: Long = 100L,
        lat: Double? = null,
        lon: Double? = null,
        stop: String = "S1",
    ) = ParkRideFacilityDetail(
        spotsAvailable = free,
        totalSpots = total,
        facilityName = name,
        percentageFull = percent,
        stopId = stop,
        timeText = time,
        facilityId = id,
        latitude = lat,
        longitude = lon,
        readAtEpochSeconds = readAt,
    )

    private fun noReading(id: String, name: String = "Car park $id") =
        car(id = id, name = name, free = -1, total = -1, percent = -1, time = "", readAt = 0L)

    private fun station(vararg facilities: ParkRideFacilityDetail, stop: String = "S1") = ParkRideUiState(
        stopId = stop,
        stopName = "Tallawong",
        facilities = facilities.toList().toImmutableSet(),
    )

    // region the total

    private class TotalCase(val name: String, val cars: List<ParkRideFacilityDetail>, val expected: Int?)

    @Test
    fun `the total is the free spots of every car park with a reading`() {
        val cases = listOf(
            TotalCase("one car park", listOf(car("1", free = 124)), 124),
            TotalCase(
                "three car parks add up",
                listOf(car("1", free = 120), car("2", free = 30), car("3", free = 5)),
                155,
            ),
            TotalCase("a full car park adds nothing but is counted", listOf(car("1", free = 0), car("2", free = 7)), 7),
            TotalCase("all full is zero and not missing", listOf(car("1", free = 0), car("2", free = 0)), 0),
            TotalCase("a car park with no reading is left out", listOf(car("1", free = 40), noReading("2")), 40),
            TotalCase("no reading anywhere has no total", listOf(noReading("1"), noReading("2")), null),
            TotalCase("no car parks has no total", emptyList(), null),
        )
        cases.forEach { case ->
            assertEquals(case.expected, totalFreeSpots(case.cars), "Total for ${case.name}")
        }
    }

    @Test
    fun `a zero total reads as full and a missing one reads as no number`() {
        assertEquals(0, station(car("1", free = 0)).toTile().totalFreeSpots)
        assertNull(station(noReading("1")).toTile().totalFreeSpots)
    }

    // endregion

    // region the time

    @Test
    fun `the time is the oldest reading the total is made of`() {
        val cars = listOf(
            car("1", time = "8:05 AM", readAt = 300L),
            car("2", time = "7:50 AM", readAt = 100L),
            car("3", time = "8:00 AM", readAt = 200L),
        )
        assertEquals("7:50 AM", oldestReadingTime(cars))
    }

    @Test
    fun `a car park with no reading or no time never sets the time`() {
        assertEquals("8:00 AM", oldestReadingTime(listOf(car("1", time = "8:00 AM"), noReading("2"))))
        assertEquals("8:00 AM", oldestReadingTime(listOf(car("1", time = "8:00 AM"), car("2", time = "", readAt = 1L))))
        assertNull(oldestReadingTime(listOf(noReading("1"))))
        assertNull(oldestReadingTime(emptyList()))
    }

    @Test
    fun `an old reading is still shown with its time and never hidden for being old`() {
        val tile = station(car("1", free = 12, time = "6:00 AM", readAt = 1L)).toTile()

        assertEquals(12, tile.totalFreeSpots)
        assertEquals("6:00 AM", tile.timeText)
    }

    // endregion

    // region directions

    private class DirectionsCase(val name: String, val lat: Double?, val lon: Double?, val expected: LatLng?)

    @Test
    fun `directions go to the car parks own position or nowhere`() {
        val cases = listOf(
            DirectionsCase("a real position", -33.69, 150.90, LatLng(-33.69, 150.90)),
            DirectionsCase("no latitude", null, 150.90, null),
            DirectionsCase("no longitude", -33.69, null, null),
            DirectionsCase("neither", null, null, null),
            DirectionsCase("0.0 and 0.0 is a missing position", 0.0, 0.0, null),
            DirectionsCase("one zero is still a place", 0.0, 150.90, LatLng(0.0, 150.90)),
            DirectionsCase("latitude off the globe", 91.0, 150.90, null),
            DirectionsCase("longitude off the globe", -33.69, 181.0, null),
            DirectionsCase("the edge of the globe", 90.0, -180.0, LatLng(90.0, -180.0)),
        )
        cases.forEach { case ->
            assertEquals(case.expected, directionsTarget(case.lat, case.lon), "Directions for ${case.name}")
        }
    }

    @Test
    fun `a car park without a position has no Directions and never borrows the stations`() {
        val tile = station(
            car("1", lat = -33.69, lon = 150.90),
            car("2", lat = null, lon = null),
            car("3", lat = 0.0, lon = 0.0),
        ).toTile()

        assertEquals(LatLng(-33.69, 150.90), tile.carParks[0].directionsTo)
        assertNull(tile.carParks[1].directionsTo)
        assertNull(tile.carParks[2].directionsTo)
    }

    // endregion

    // region the tile

    @Test
    fun `car parks keep their order when the numbers change`() {
        val before = station(car("3", name = "Tallawong P3", free = 5), car("1", name = "Tallawong P1", free = 50))
        val after = station(car("1", name = "Tallawong P1", free = 2), car("3", name = "Tallawong P3", free = 90))

        assertEquals(listOf("1", "3"), before.toTile().carParks.map { it.facilityId })
        assertEquals(listOf("1", "3"), after.toTile().carParks.map { it.facilityId })
    }

    @Test
    fun `every car park of the station is listed at equal weight, with or without a reading`() {
        val tile = station(
            car("1", name = "Tallawong P1", free = 50),
            noReading("2", name = "Tallawong P2"),
            car("3", name = "Tallawong P3", free = 7),
        ).toTile()

        assertEquals(3, tile.carParks.size)
        assertEquals(listOf(50, null, 7), tile.carParks.map { it.freeSpots })
        assertEquals(57, tile.totalFreeSpots)
    }

    @Test
    fun `a car park is called by its display name, and by the station when it has none`() {
        val tile = station(
            car("1", name = "Park&Ride - Tallawong P1"),
            car("2", name = ""),
        ).toTile()

        assertEquals(listOf("Tallawong", "Tallawong P1"), tile.carParks.map { it.name })
    }

    @Test
    fun `a tile carries every stop id of its station for the Go here branch`() {
        val tile = station(
            car("1", stop = "210318"),
            car("1b", stop = "2103108", name = "Other"),
            car("2", stop = "210318", name = "Another"),
            stop = "210318",
        ).toTile()

        assertEquals(listOf("2103108", "210318"), tile.stopIds)
        assertEquals("210318", tile.stationId)
        assertEquals("Tallawong", tile.stationName)
    }

    @Test
    fun `a station with no car park has no tile`() {
        val tiles = listOf(
            station(car("1")),
            ParkRideUiState(stopId = "S9", stopName = "Empty", facilities = persistentSetOf()),
        ).toTiles()

        assertEquals(listOf("S1"), tiles.map { it.stationId })
    }

    @Test
    fun `the percent full of a car park with no reading is missing and not zero`() {
        val tile = station(car("1", percent = 40), noReading("2")).toTile()

        assertEquals(40, tile.carParks[0].percentFull)
        assertNull(tile.carParks[1].percentFull)
        assertNull(tile.carParks[1].timeText)
    }

    // endregion

    // region the width

    private class WidthCase(val name: String, val column: Dp, val fontScale: Float, val expected: Dp)

    @Test
    fun `a tile is 148 dp scaling with the text and never wider than the column less 60`() {
        val cases = listOf(
            WidthCase("a phone at normal text", 411.dp, 1.0f, 148.dp),
            WidthCase("the narrowest phone at normal text", 320.dp, 1.0f, 148.dp),
            WidthCase("a phone at 130 percent", 411.dp, 1.3f, 192.4.dp),
            WidthCase("a phone at 200 percent", 411.dp, 2.0f, 296.dp),
            WidthCase("320 dp at 200 percent stops at the column less 60", 320.dp, 2.0f, 260.dp),
            WidthCase("exactly at the cap", 296.dp + 60.dp, 2.0f, 296.dp),
            WidthCase("one dp over the cap", 295.dp + 60.dp, 2.0f, 295.dp),
            WidthCase("a column too narrow for the peek", 40.dp, 1.0f, 0.dp),
            WidthCase("a negative column is no tile", (-10).dp, 1.0f, 0.dp),
            WidthCase("the 560 dp column at 200 percent", 560.dp, 2.0f, 296.dp),
        )
        cases.forEach { case ->
            assertEquals(
                case.expected.value,
                parkRideTileWidth(case.column, case.fontScale).value,
                0.01f,
                "Tile width for ${case.name}",
            )
        }
    }

    @Test
    fun `a closed tile always leaves 60 dp of the column for the next one`() {
        listOf(320.dp, 360.dp, 411.dp, 480.dp, 560.dp).forEach { column ->
            listOf(0.85f, 1.0f, 1.3f, 1.5f, 2.0f, 3.0f).forEach { scale ->
                assertTrue(
                    parkRideTileWidth(column, scale) <= column - 60.dp,
                    "A $column column at $scale text must keep 60 dp past the tile",
                )
            }
        }
    }

    // endregion

    // region one open at a time

    @Test
    fun `tapping a tile opens it, and only one tile is open at a time`() {
        assertEquals(setOf("A"), openTilesAfterTap(emptySet(), "A"))
        assertEquals(setOf("B"), openTilesAfterTap(setOf("A"), "B"))
        assertEquals(emptySet(), openTilesAfterTap(setOf("A"), "A"))
    }

    @Test
    fun `a state restored with two open tiles closes both on a tap of one of them`() {
        assertEquals(emptySet(), openTilesAfterTap(setOf("A", "B"), "B"))
        assertEquals(setOf("C"), openTilesAfterTap(setOf("A", "B"), "C"))
    }

    // endregion
}
