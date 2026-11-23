package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The size a headline that is kept to its lines is drawn at: the largest at which its widest line fits, never above the normal
 * size, never below the floor, and the floor rises with the rider's text setting up to 0.75.
 */
class IntroHeadlineFitTest {

    // A line that is 420 px wide at a scale of 1.0 and grows in step with the scale, as text does.
    private fun widthAt(scale: Float): Int = (WIDEST_AT_ONE * scale).toInt()

    @Test
    fun `the floor is 0_65 of the rider's text setting and 0_75 at the most`() {
        val table = listOf(
            0.85f to 0.5525f,
            1.0f to 0.65f,
            1.1f to 0.715f,
            1.15f to 0.7475f,
            1.2f to 0.75f,
            1.5f to 0.75f,
            2.0f to 0.75f,
        )

        table.forEach { (system, floor) -> assertEquals(floor, headlineFitFloor(system), 0.0001f, "at $system") }
    }

    @Test
    fun `a headline that fits at the normal size is drawn at it`() {
        assertEquals(1.0f, fitHeadlineScale(normal = 1.0f, floor = 0.65f, roomPx = 500, widestPxAt = ::widthAt))
        assertEquals(1.0f, fitHeadlineScale(normal = 1.0f, floor = 0.65f, roomPx = WIDEST_AT_ONE, widestPxAt = ::widthAt))
        assertEquals(0.936f, fitHeadlineScale(normal = 0.936f, floor = 0.65f, roomPx = 400, widestPxAt = ::widthAt))
    }

    @Test
    fun `one that does not fit is drawn at the largest hundredth at which its widest line does`() {
        // 379 of 420 is 0.9024, so the largest hundredth is 0.90, which is 378 px.
        val scale = fitHeadlineScale(normal = 1.0f, floor = 0.65f, roomPx = 379, widestPxAt = ::widthAt)

        assertEquals(0.90f, scale, 0.0001f)
        assertTrue(widthAt(scale) <= 379)
        assertTrue(widthAt(scale + 0.01f) > 379, "a hundredth more would not fit")
    }

    @Test
    fun `it is never above the normal size and never below the floor, so a narrower room wraps a line instead`() {
        // 288 of 420 is 0.6857, so 0.68, which is above a floor of 0.65.
        assertEquals(0.68f, fitHeadlineScale(normal = 1.0f, floor = 0.65f, roomPx = 288, widestPxAt = ::widthAt), 0.0001f)
        val atFloor = fitHeadlineScale(normal = 1.0f, floor = 0.75f, roomPx = 288, widestPxAt = ::widthAt)
        assertEquals(0.75f, atFloor, 0.0001f)
        assertTrue(widthAt(atFloor) > 288, "the widest line is still too wide, so it wraps at a word")
        assertEquals(0.75f, fitHeadlineScale(normal = 1.0f, floor = 0.75f, roomPx = 10, widestPxAt = ::widthAt), 0.0001f)
    }

    @Test
    fun `a floor above the normal size is held at the normal size`() {
        assertEquals(0.6f, fitHeadlineScale(normal = 0.6f, floor = 0.75f, roomPx = 100, widestPxAt = ::widthAt), 0.0001f)
    }

    @Test
    fun `text that is a little wider than the first guess is stepped down until it fits`() {
        // The widest line is 3 percent wider at the guess than a straight line says, as hinting can make it.
        val wider: (Float) -> Int = { scale -> (WIDEST_AT_ONE * scale * if (scale < 1f) 1.03f else 1f).toInt() }

        val scale = fitHeadlineScale(normal = 1.0f, floor = 0.5f, roomPx = 300, widestPxAt = wider)

        assertTrue(wider(scale) <= 300, "the line is ${wider(scale)} px in 300")
        assertTrue(wider(scale + 0.02f) > 300, "it was lowered more than it had to be")
    }

    private companion object {
        const val WIDEST_AT_ONE = 420
    }
}
