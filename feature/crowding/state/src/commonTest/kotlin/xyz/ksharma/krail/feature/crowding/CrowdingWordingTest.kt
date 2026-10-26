package xyz.ksharma.krail.feature.crowding

import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.feature.crowding.SeatLevel.STANDING_ONLY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** One test per row of the wording table in CROWDING_DESIGN_REVIEW.md, plus the gate. */
class CrowdingWordingTest {

    private fun cars(vararg levels: SeatLevel) = CarriageLevels(levels.toList())

    private fun live(cars: CarriageLevels, ends: Boolean = false) =
        CrowdingWording.forCarriages(cars, Certainty.LIVE, endWordingEnabled = ends)

    @Test
    fun `one end has seats`() {
        val advice = live(cars(SEATS_AVAILABLE, SEATS_AVAILABLE, STANDING_ONLY, STANDING_ONLY), ends = true)
        assertEquals("Seats at the back of the train.", advice?.sentence)
        assertEquals("Seats at the back", advice?.tag)
    }

    @Test
    fun `one end has a few seats`() {
        val advice = live(cars(STANDING_ONLY, STANDING_ONLY, STANDING_ONLY, FEW_SEATS), ends = true)
        assertEquals("A few seats left at the front.", advice?.sentence)
        assertEquals("Few seats at the front", advice?.tag)
    }

    @Test
    fun `forecast not live`() {
        val advice = CrowdingWording.forCarriages(
            cars(SEATS_AVAILABLE, STANDING_ONLY, STANDING_ONLY, STANDING_ONLY),
            Certainty.FORECAST,
            endWordingEnabled = true,
        )
        assertEquals("Seats expected at the back.", advice?.sentence)
    }

    @Test
    fun `both ends have seats`() {
        val advice = live(cars(SEATS_AVAILABLE, FEW_SEATS, FEW_SEATS, SEATS_AVAILABLE))
        assertEquals("Seats at both ends of the train.", advice?.sentence)
        assertEquals("Seats at both ends", advice?.tag)
    }

    @Test
    fun `seats everywhere`() {
        val advice = live(cars(SEATS_AVAILABLE, SEATS_AVAILABLE, SEATS_AVAILABLE, SEATS_AVAILABLE))
        assertEquals("Seats available all along the train.", advice?.sentence)
        assertEquals("Seats available", advice?.tag)
    }

    @Test
    fun `few seats everywhere`() {
        assertEquals(
            "A few seats left along the train.",
            live(cars(FEW_SEATS, FEW_SEATS, FEW_SEATS, FEW_SEATS))?.sentence,
        )
    }

    @Test
    fun `full everywhere`() {
        val advice = live(cars(STANDING_ONLY, STANDING_ONLY, STANDING_ONLY, STANDING_ONLY))
        assertEquals("Standing only, all along the train.", advice?.sentence)
        assertEquals("Standing only", advice?.tag)
    }

    @Test
    fun `next train has seats`() {
        assertEquals(
            "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due " +
                "at Central at 9:26am.",
            CrowdingWording.nextTrain(
                service = NextService(
                    line = "T1",
                    departureTime = "8:59am",
                    level = SEATS_AVAILABLE,
                    certainty = Certainty.LIVE,
                    platform = "Platform 1",
                ),
                destination = "Central",
                arrivalTime = "9:26am",
            ),
        )
    }

    @Test
    fun `next bus has seats`() {
        assertEquals(
            "Want a seat? The next 333, at 9:04am, has seats available.",
            CrowdingWording.nextBus(NextService("333", "9:04am", SEATS_AVAILABLE, Certainty.LIVE)),
        )
    }

    @Test
    fun `forecast next service says should and never will`() {
        val line = CrowdingWording.nextBus(NextService("333", "9:04am", FEW_SEATS, Certainty.FORECAST))
        assertEquals("Want a seat? The next 333, at 9:04am, should have a few seats left.", line)
        assertFalse(line.contains("will"))
    }

    @Test
    fun `night replaces the zone line with the Nightsafe line`() {
        val advice = CrowdingWording.forCarriages(
            cars(SEATS_AVAILABLE, STANDING_ONLY, STANDING_ONLY, SEATS_AVAILABLE),
            Certainty.LIVE,
            isNight = true,
        )
        assertEquals(CrowdingWording.NIGHT_LINE, advice?.sentence)
        assertEquals("Seats available", advice?.tag)
    }

    @Test
    fun `night starts at 9pm and ends at 5am`() {
        assertFalse(CrowdingWording.isNight(20))
        assertEquals(true, CrowdingWording.isNight(21))
        assertEquals(true, CrowdingWording.isNight(4))
        assertFalse(CrowdingWording.isNight(5))
    }

    @Test
    fun `gate off names no end`() {
        assertNull(live(cars(SEATS_AVAILABLE, STANDING_ONLY, STANDING_ONLY, STANDING_ONLY)))
        assertFalse(CrowdingWording.END_WORDING_ENABLED)
    }

    @Test
    fun `alternating cars say nothing while ends are gated`() {
        val alternating = CarriageLevels(List(8) { if (it % 2 == 0) SEATS_AVAILABLE else STANDING_ONLY })
        assertNull(live(alternating))
    }

    @Test
    fun `middle and one end quieter says middle with the gate off`() {
        val advice = live(cars(SEATS_AVAILABLE, SEATS_AVAILABLE, SEATS_AVAILABLE, STANDING_ONLY))
        assertEquals("Seats in the middle of the train.", advice?.sentence)
        assertEquals("Seats in the middle", advice?.tag)
    }

    @Test
    fun `whole train level tags`() {
        assertEquals("Seats available", CrowdingWording.levelTag(SEATS_AVAILABLE))
        assertEquals("Few seats", CrowdingWording.levelTag(FEW_SEATS))
        assertEquals("Standing only", CrowdingWording.levelTag(STANDING_ONLY))
    }

    @Test
    fun `no line says room or names a car number`() {
        val all = SeatLevel.entries.flatMap { a ->
            SeatLevel.entries.flatMap { b ->
                Certainty.entries.mapNotNull {
                    CrowdingWording.forCarriages(cars(a, b, b, b, b, b, b, a), it, endWordingEnabled = true)
                }
            }
        }
        all.forEach { advice ->
            val text = "${advice.sentence} ${advice.tag}"
            assertFalse(text.contains("room"), text)
            assertFalse(text.any { it.isDigit() }, text)
        }
    }
}
