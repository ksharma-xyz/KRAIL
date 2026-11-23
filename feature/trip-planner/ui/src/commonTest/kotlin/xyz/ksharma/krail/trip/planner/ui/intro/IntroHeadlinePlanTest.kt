package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The size rules of the hero and whisper headline, against the numbers of the approved mock. The mock measured every line once at
 * 47.6 sp in Roboto Black (the widths below, in px at that size) and scaled it, and so does the width function here, so the plan
 * is held to the mock's own arithmetic with no font and no layout. A font scale of 1.0 is 47.6 sp.
 */
class IntroHeadlinePlanTest {

    // Widths in px at 47.6 sp, capitals, one line, from the mock (`widths-at-47.6.json`).
    private val widthAt476 = mapOf(
        "SHOW" to 137.484375f,
        "PARKING SPOTS" to 364.046875f,
        "AT YOUR" to 194.890625f,
        "NEAREST STATION?" to 442.09375f,
        "WHERE ARE YOU" to 366.671875f,
        "GOING?" to 169.84375f,
        "LEAVING FROM?" to 362.578125f,
        "SAVE" to 118.546875f,
        "THIS TRIP?" to 248.859375f,
        "YOUR FAVOURITE" to 385.921875f,
        "TRANSPORT MODE" to 423f,
        "IN SYDNEY?" to 264.359375f,
        "TRANSPORT" to 280.515625f,
        "MODE" to 132.015625f,
    )

    private fun widthAt(line: String, scale: Float): Int =
        ceil(widthAt476.getValue(introHeadlineDisplay(line)) * scale).toInt()

    private class Screen(val name: String, val lines: List<String>, val hero: Int)

    private val screens = listOf(
        Screen("Park and Ride", PARK_RIDE_HEADLINE, PARK_RIDE_HERO_INDEX),
        Screen("Destination", DESTINATION_HEADLINE, STOP_HERO_INDEX),
        Screen("Origin", ORIGIN_HEADLINE, STOP_HERO_INDEX),
        Screen("Confirm", CONFIRM_HEADLINE, CONFIRM_HERO_INDEX),
        Screen("Theme", PICK_THEME_HEADLINE, PICK_THEME_HERO_INDEX),
    )

    // A frame is a window and the rider's text size. The room is the window less the 16 dp each side the Intro's frame leaves.
    private class Frame(val name: String, val roomDp: Int, val systemScale: Float) {
        val normal: Float get() = headlineFontScale(systemScale, roomDp.dp)
    }

    private val phone = Frame("411 dp, 100 percent", 379, 1.0f)
    private val phoneLarge = Frame("411 dp, 150 percent", 379, 1.5f)
    private val narrow = Frame("320 dp, 200 percent", 288, 2.0f)

    private fun plan(screen: Screen, frame: Frame) =
        planHeroHeadline(screen.lines, screen.hero, frame.normal, frame.roomDp, ::widthAt)

    private fun HeadlinePlan.sizes(): List<Float> = units.map { it.scale * BASE_SP }

    private fun assertSizes(expected: List<Float>, screen: Screen, frame: Frame, tolerance: Float = TOLERANCE_SP) {
        val actual = plan(screen, frame).sizes()
        assertEquals(expected.size, actual.size, "${screen.name}, ${frame.name}: lines")
        expected.zip(actual).forEachIndexed { i, (want, got) ->
            assertTrue(abs(want - got) <= tolerance, "${screen.name}, ${frame.name}: line ${i + 1} is $got sp, the mock has $want")
        }
    }

    @Test
    fun `the frames are the mock's, with the normal size 47_6 at 411 dp, a held 1_05 at 150 percent and 44_5 at 320 dp and 200 percent`() {
        assertEquals(47.6f, phone.normal * BASE_SP, 0.001f)
        assertEquals(1.05f * BASE_SP, phoneLarge.normal * BASE_SP, 0.001f)
        assertEquals(44.5f, narrow.normal * BASE_SP, 0.05f)
    }

    @Test
    fun `at 411 dp and 100 percent the hero is 47_6 and each whisper 33_3, and Theme's hero is fitted to 42_6 with whispers of 29_8`() {
        assertSizes(listOf(33.3f, 47.6f, 33.3f, 33.3f), screens[0], phone)
        assertSizes(listOf(33.3f, 47.6f), screens[1], phone)
        assertSizes(listOf(33.3f, 47.6f), screens[2], phone)
        assertSizes(listOf(47.6f, 33.3f), screens[3], phone)
        assertSizes(listOf(29.8f, 42.6f, 29.8f), screens[4], phone)
    }

    @Test
    fun `at 411 dp and 150 percent the headline is held at 1_05 and the two widest heroes are fitted by a hair`() {
        val normal = 1.05f * BASE_SP
        // PARKING SPOTS is 382.3 dp at 1.05 in a room of 379 and LEAVING FROM? is 380.7, so those two are fitted, the rest are not.
        assertSizes(listOf(0.7f * 49.55f, 49.55f, 0.7f * 49.55f, 0.7f * 49.55f), screens[0], phoneLarge)
        assertSizes(listOf(0.7f * normal, normal), screens[1], phoneLarge)
        assertSizes(listOf(0.7f * 49.75f, 49.75f), screens[2], phoneLarge)
        assertSizes(listOf(normal, 0.7f * normal), screens[3], phoneLarge)
        assertSizes(listOf(29.8f, 42.6f, 29.8f), screens[4], phoneLarge)
    }

    @Test
    fun `at 320 dp and 200 percent Park, Origin and Confirm are fitted and the Theme hero stacks at the normal size`() {
        assertSizes(listOf(0.7f * 37.66f, 37.66f, 0.7f * 37.66f, 0.7f * 37.66f), screens[0], narrow)
        assertSizes(listOf(0.7f * 44.5f, 44.5f), screens[1], narrow)
        assertSizes(listOf(0.7f * 37.8f, 37.8f), screens[2], narrow)
        assertSizes(listOf(44.5f, 0.7f * 44.5f), screens[3], narrow)
        // The Theme hero is 0.73 of the normal size on one line, under the 0.75 floor, so it stacks at the normal size.
        assertSizes(listOf(0.7f * 44.5f, 44.5f, 0.7f * 44.5f), screens[4], narrow)
        val theme = plan(screens[4], narrow)
        assertTrue(theme.heroStacked)
        assertEquals(listOf("transport", "mode"), theme.units[PICK_THEME_HERO_INDEX].rows)
    }

    @Test
    fun `no hero stacks in any other frame, and the hero is never above the normal size`() {
        for (frame in listOf(phone, phoneLarge, narrow)) {
            for (screen in screens) {
                val plan = plan(screen, frame)
                assertTrue(plan.heroScale!! <= frame.normal + 0.0001f, "${screen.name}, ${frame.name}")
                assertEquals(screen === screens[4] && frame === narrow, plan.heroStacked, "${screen.name}, ${frame.name}")
            }
        }
    }

    @Test
    fun `every whisper is 0_70 of the hero in every frame, so none is fitted`() {
        for (frame in listOf(phone, phoneLarge, narrow)) {
            for (screen in screens) {
                val plan = plan(screen, frame)
                assertFalse(plan.whisperFitted, "${screen.name}, ${frame.name}")
                plan.units.filter { it.role == HeadlineRole.Whisper }.forEach {
                    assertEquals(plan.heroScale!! * WHISPER_SHARE, it.scale, 0.0001f, "${screen.name}, ${frame.name}")
                }
            }
        }
    }

    @Test
    fun `no line is wider than its room in any frame, and no word is split`() {
        for (frame in listOf(phone, phoneLarge, narrow)) {
            for (screen in screens) {
                val plan = plan(screen, frame)
                val words = screen.lines.flatMap { it.split(' ') }.toSet()
                plan.units.forEach { unit ->
                    unit.rows.forEach { row ->
                        assertTrue(widthAt(row, unit.scale) <= frame.roomDp, "${screen.name}, ${frame.name}: \"$row\" is too wide")
                        assertTrue(row.split(' ').all { it in words }, "${screen.name}: \"$row\" splits a word")
                    }
                }
                assertEquals(screen.lines.joinToString(" "), plan.units.flatMap { it.rows }.joinToString(" "), screen.name)
            }
        }
    }

    @Test
    fun `the hero is the line at the hero index and carries its question mark, and a whisper that ends the question keeps its own`() {
        assertEquals("going?", plan(screens[1], phone).units.single { it.role == HeadlineRole.Hero }.rows.single())
        assertEquals("leaving from?", plan(screens[2], phone).units.single { it.role == HeadlineRole.Hero }.rows.single())
        assertEquals("Save", plan(screens[3], phone).units.single { it.role == HeadlineRole.Hero }.rows.single())
        assertEquals("this trip?", plan(screens[3], phone).units.single { it.role == HeadlineRole.Whisper }.rows.single())
        assertEquals(listOf(HeadlineRole.Whisper, HeadlineRole.Hero, HeadlineRole.Whisper, HeadlineRole.Whisper), plan(screens[0], phone).units.map { it.role })
    }

    @Test
    fun `a stacked hero keeps its question mark on its last word and stacks at the largest size that fits its widest word`() {
        val lines = listOf("Where are you", "leaving from?")
        // A room of 200 px: LEAVING FROM? is 400 px at the normal size, so 0.5 of it, under the floor, but LEAVING is 195 and fits.
        val width = { line: String, scale: Float ->
            val px = when (line) {
                "leaving from?" -> 400f
                "leaving" -> 195f
                "from?" -> 150f
                else -> 100f
            }
            ceil(px * scale).toInt()
        }
        val plan = planHeroHeadline(lines, 1, normal = 1f, roomPx = 200, widthAt = width)

        assertTrue(plan.heroStacked)
        assertEquals(listOf("leaving", "from?"), plan.units[1].rows)
        assertEquals(1f, plan.units[1].scale, 0.0001f, "the widest word fits at the normal size, so the stack is at the normal size")
        assertEquals(WHISPER_SHARE, plan.units[0].scale, 0.0001f)
    }

    @Test
    fun `a whisper that does not fit at 0_70 takes the largest size that fits, never below 0_55 of the hero, and says so`() {
        val lines = listOf("Where are you", "going?")
        // The whisper is 300 px at the hero's size here in a room of 250: at 0.70 it is 210, which fits, so make it wider.
        val wide = { line: String, scale: Float -> if (line == "Where are you") (500 * scale).toInt() else (100 * scale).toInt() }
        val fitted = planHeroHeadline(lines, 1, normal = 1f, roomPx = 300, widthAt = wide)

        // 500 px at 0.70 is 350 over a room of 300, so it is fitted to 0.60 of the hero, which is over the floor of 0.55.
        assertTrue(fitted.whisperFitted)
        assertEquals(0.6f, fitted.units[0].scale, 0.006f)
        assertTrue(fitted.units[0].scale >= WHISPER_FLOOR_SHARE * fitted.units[1].scale)

        val tight = planHeroHeadline(lines, 1, normal = 1f, roomPx = 200, widthAt = wide)
        assertEquals(WHISPER_FLOOR_SHARE, tight.units[0].scale / tight.units[1].scale, 0.0001f, "never below the floor")
        assertTrue(tight.whisperFitted)
    }

    @Test
    fun `a plain headline is every line at the normal size`() {
        val plan = planPlainHeadline(listOf("On the track,", "no lookin' back!"), normal = 0.9357f)

        assertEquals(listOf(HeadlineRole.Plain, HeadlineRole.Plain), plan.units.map { it.role })
        assertTrue(plan.units.all { it.scale == 0.9357f && it.rows.size == 1 })
        assertEquals(null, plan.heroScale)
    }

    @Test
    fun `fitLineScale gives the ceiling when the line fits, else the largest half hundredth that does`() {
        val width = { _: String, scale: Float -> (400 * scale).toInt() }

        assertEquals(1f, fitLineScale("x", 1f, roomPx = 400, widthAt = width))
        val fitted = fitLineScale("x", 1f, roomPx = 300, widthAt = width)
        assertTrue(width("x", fitted) <= 300 && width("x", fitted + 0.005f) > 300, "fitted to $fitted")
        assertTrue(fitLineScale("x", 1f, roomPx = 1, widthAt = width) >= 0.1f, "never fitted to nothing")
    }

    @Test
    fun `the height of a plan is its rows, the gaps between lines and the hero's room above and below`() {
        val plan = HeadlinePlan(
            listOf(
                HeadlineUnit(HeadlineRole.Whisper, listOf("a"), 0.5f),
                HeadlineUnit(HeadlineRole.Hero, listOf("b", "c"), 1f),
                HeadlineUnit(HeadlineRole.Whisper, listOf("d"), 0.5f),
            ),
        )

        // Rows of 100 dp at scale 1: 50 + (200 + 10 between its rows + 2 x 6 of room) + 50, and 2 gaps of 10 between the 3 units.
        assertEquals(50f + 222f + 50f + 20f, headlineHeightDp(plan, rowHeightDp = 100f, lineGap = 10f, heroPad = 6f))
        assertEquals(0f, headlineHeightDp(HeadlinePlan(emptyList()), 100f, 10f, 6f))
    }

    private companion object {
        const val BASE_SP = 47.6f
        const val TOLERANCE_SP = 0.5f
    }
}
