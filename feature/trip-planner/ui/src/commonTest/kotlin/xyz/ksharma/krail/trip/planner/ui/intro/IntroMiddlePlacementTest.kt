package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntroMiddlePlacementTest {

    private class MiddleCase(
        val windowMiddle: Int,
        val middle: Int,
        val headlineBottom: Int,
        val barTop: Int,
        val gap: Int,
        val expected: Int,
    )

    // A 1000 px frame whose bar slot starts at 880, a headline that ends at 300 and a gap of 16 above the bar.
    private val rows = listOf(
        // Fits: the middle is centred on the window, 500, with a middle 200 tall, so it starts at 400.
        MiddleCase(500, 200, 300, 880, 16, 400),
        // The ideal top is above the headline: the headline's limit wins and the middle starts at its bottom.
        MiddleCase(500, 400, 300, 880, 16, 300),
        MiddleCase(500, 800, 300, 880, 16, 300),
        // The ideal top would end lower than one gap above the bar, so it stops there: 880 - 16 - 200 = 664.
        MiddleCase(800, 200, 300, 880, 16, 664),
        // No room for both: it starts under the headline and the column scrolls.
        MiddleCase(500, 700, 300, 880, 16, 300),
        // A window middle above the frame's middle, because the bottom inset is smaller than the top one.
        MiddleCase(460, 200, 300, 880, 16, 360),
        // A frame with no bar slot: the bar's top is the bottom of the frame.
        MiddleCase(500, 200, 300, 1000, 16, 400),
        MiddleCase(960, 200, 300, 1000, 16, 784),
    )

    @Test
    fun `the middle is centred on the window and moved only to clear the headline and the bar`() {
        rows.forEach { row ->
            assertEquals(
                row.expected,
                windowCentredMiddleTop(row.windowMiddle, row.middle, row.headlineBottom, row.barTop, row.gap),
                "window middle ${row.windowMiddle}, middle ${row.middle}, headline bottom ${row.headlineBottom}, " +
                    "bar top ${row.barTop}, gap ${row.gap}",
            )
        }
    }

    @Test
    fun `it never overlaps the headline, and clears the bar by the gap whenever the two can both be kept`() {
        for (windowMiddle in 0..1000 step 20) {
            for (middle in listOf(0, 100, 184, 400, 800)) {
                val top =
                    windowCentredMiddleTop(windowMiddle, middle, headlineBottom = 300, barTop = 880, bottomGap = 16)

                assertTrue(top >= 300, "middle $middle at window middle $windowMiddle starts at $top, in the headline")
                if (300 + middle + 16 <= 880) {
                    assertTrue(
                        top + middle + 16 <= 880,
                        "middle $middle at $windowMiddle ends at ${top + middle}, near the bar",
                    )
                }
            }
        }
    }

    @Test
    fun `when it fits it is centred on the window middle to the pixel, and a longer headline above does not move it`() {
        // The headline's height is the only thing from above that is read, and a headline that is shorter moves nothing
        // while it ends above the middle's own top.
        val tall =
            windowCentredMiddleTop(windowMiddle = 500, middle = 184, headlineBottom = 380, barTop = 880, bottomGap = 16)
        val short =
            windowCentredMiddleTop(windowMiddle = 500, middle = 184, headlineBottom = 200, barTop = 880, bottomGap = 16)

        assertEquals(408, tall)
        assertEquals(tall, short)
        assertEquals(500, tall + 184 / 2)
    }

    @Test
    fun `the window middle is half way down the frame and both insets, less the top inset`() {
        assertEquals(500, windowMiddleInZone(zone = 1000, topInset = 0, bottomInset = 0))
        // A 1000 px frame with 80 px of status bar above it and 120 px of navigation bar below: the window is 1200 tall.
        assertEquals(520, windowMiddleInZone(zone = 1000, topInset = 80, bottomInset = 120))
        assertEquals(500, windowMiddleInZone(zone = 1000, topInset = 100, bottomInset = 100))
        assertEquals(480, windowMiddleInZone(zone = 1000, topInset = 100, bottomInset = 60))
    }

    @Test
    fun `the placement follows the two flags, and the window wins`() {
        assertEquals(MiddlePlacement.CentredInRoom, middlePlacement(centreMiddle = true, centreOnWindow = false))
        assertEquals(MiddlePlacement.Under, middlePlacement(centreMiddle = false, centreOnWindow = false))
        assertEquals(MiddlePlacement.CentredOnWindow, middlePlacement(centreMiddle = true, centreOnWindow = true))
        assertEquals(MiddlePlacement.CentredOnWindow, middlePlacement(centreMiddle = false, centreOnWindow = true))
    }
}
