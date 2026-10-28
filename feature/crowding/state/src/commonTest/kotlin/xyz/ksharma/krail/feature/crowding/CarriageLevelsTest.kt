package xyz.ksharma.krail.feature.crowding

import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.feature.crowding.SeatLevel.STANDING_ONLY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CarriageLevelsTest {

    @Test
    fun `eight cars split one six one`() {
        val cars = CarriageLevels(
            listOf(
                SEATS_AVAILABLE,
                SEATS_AVAILABLE,
                STANDING_ONLY,
                FEW_SEATS,
                FEW_SEATS,
                FEW_SEATS,
                FEW_SEATS,
                SEATS_AVAILABLE,
            ),
        )
        assertEquals(
            mapOf(Zone.BACK to SEATS_AVAILABLE, Zone.MIDDLE to STANDING_ONLY, Zone.FRONT to SEATS_AVAILABLE),
            cars.zoneLevels(),
        )
        // Both end cars have seats, but two cars do at the back and one at the front.
        assertEquals(setOf(Zone.BACK), cars.quietestZones())
    }

    @Test
    fun `the end with the longer run of quiet cars wins`() {
        // Seen on a platform on 28 October: three cars with seats at the back, one at the front.
        val back = CarriageLevels(
            listOf(
                SEATS_AVAILABLE,
                SEATS_AVAILABLE,
                SEATS_AVAILABLE,
                STANDING_ONLY,
                STANDING_ONLY,
                FEW_SEATS,
                STANDING_ONLY,
                SEATS_AVAILABLE,
            ),
        )
        assertEquals(setOf(Zone.BACK), back.quietestZones())
        assertEquals(setOf(Zone.FRONT), CarriageLevels(back.levels.reversed()).quietestZones())
    }

    @Test
    fun `equal runs at each end stay both ends`() {
        val evenEnds = CarriageLevels(
            listOf(SEATS_AVAILABLE, STANDING_ONLY, STANDING_ONLY, STANDING_ONLY, STANDING_ONLY, SEATS_AVAILABLE),
        )
        assertEquals(setOf(Zone.BACK, Zone.FRONT), evenEnds.quietestZones())
    }

    @Test
    fun `four cars split one two one`() {
        val cars = CarriageLevels(listOf(FEW_SEATS, SEATS_AVAILABLE, SEATS_AVAILABLE, FEW_SEATS))
        assertEquals(
            mapOf(Zone.BACK to FEW_SEATS, Zone.MIDDLE to SEATS_AVAILABLE, Zone.FRONT to FEW_SEATS),
            cars.zoneLevels(),
        )
        assertEquals(setOf(Zone.MIDDLE), cars.quietestZones())
    }

    @Test
    fun `alternating cars never claim the middle or both ends`() {
        val alternating = List(8) { if (it % 2 == 0) SEATS_AVAILABLE else STANDING_ONLY }
        val cars = CarriageLevels(alternating)
        assertEquals(setOf(Zone.BACK), cars.quietestZones())
    }

    @Test
    fun `twelve cars have two cars per end`() {
        // The second car is fuller, so it only counts if it belongs to the back.
        val cars = CarriageLevels(listOf(SEATS_AVAILABLE, FEW_SEATS) + List(10) { SEATS_AVAILABLE })
        assertEquals(FEW_SEATS, cars.zoneLevels()?.get(Zone.BACK))
        assertEquals(SEATS_AVAILABLE, CarriageLevels(List(8) { SEATS_AVAILABLE }).zoneLevels()?.get(Zone.BACK))
    }

    @Test
    fun `under three cars has no zones`() {
        val cars = CarriageLevels(listOf(SEATS_AVAILABLE, STANDING_ONLY))
        assertNull(cars.zoneLevels())
        assertEquals(STANDING_ONLY, cars.quietestZoneLevel())
    }

    @Test
    fun `one based Sydney Trains positions are re-sequenced`() {
        val cars = CarriageLevels.fromPositions(
            mapOf(2 to FEW_SEATS, 1 to SEATS_AVAILABLE, 4 to STANDING_ONLY, 3 to FEW_SEATS),
        )
        assertEquals(listOf(SEATS_AVAILABLE, FEW_SEATS, FEW_SEATS, STANDING_ONLY), cars?.levels)
    }

    @Test
    fun `zero based Metro positions give the same row`() {
        val oneBased = CarriageLevels.fromPositions(mapOf(1 to SEATS_AVAILABLE, 2 to STANDING_ONLY))
        val zeroBased = CarriageLevels.fromPositions(mapOf(0 to SEATS_AVAILABLE, 1 to STANDING_ONLY))
        assertEquals(oneBased, zeroBased)
    }

    @Test
    fun `a car with no level drops the whole row`() {
        assertNull(CarriageLevels.fromPositions(mapOf(1 to SEATS_AVAILABLE, 2 to null)))
        assertNull(CarriageLevels.fromPositions(emptyMap()))
    }

    @Test
    fun `parses trip planner and departure strings`() {
        assertEquals(SEATS_AVAILABLE, SeatLevel.fromApi("MANY_SEATS"))
        assertEquals(SEATS_AVAILABLE, SeatLevel.fromApi("MANY_SEATS_AVAILABLE"))
        assertEquals(FEW_SEATS, SeatLevel.fromApi("FEW_SEATS"))
        assertEquals(STANDING_ONLY, SeatLevel.fromApi("STANDING_ONLY"))
        assertEquals(STANDING_ONLY, SeatLevel.fromApi("standing_room_only"))
        assertNull(SeatLevel.fromApi(null))
        assertNull(SeatLevel.fromApi("UNKNOWN"))
    }

    @Test
    fun `parses GTFS occupancy status`() {
        assertEquals(SEATS_AVAILABLE, SeatLevel.fromGtfs(1))
        assertEquals(FEW_SEATS, SeatLevel.fromGtfs(2))
        assertEquals(STANDING_ONLY, SeatLevel.fromGtfs(3))
        assertNull(SeatLevel.fromGtfs(6))
        assertNull(SeatLevel.fromGtfs(null))
    }

    @Test
    fun `busier means at least one full level`() {
        assertEquals(true, STANDING_ONLY.isBusierThan(FEW_SEATS))
        assertEquals(false, FEW_SEATS.isBusierThan(FEW_SEATS))
    }

    @Test
    fun `typical level is the one most cars share and the busier one on a tie`() {
        val s = SeatLevel.SEATS_AVAILABLE
        val f = SeatLevel.FEW_SEATS
        assertEquals(f, CarriageLevels(listOf(s, s, f, f, f, f, f, f)).typicalLevel())
        assertEquals(f, CarriageLevels(listOf(s, s, f, f)).typicalLevel())
        assertEquals(s, CarriageLevels(listOf(s, s, s, f)).typicalLevel())
    }
}
