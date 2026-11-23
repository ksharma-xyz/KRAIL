package xyz.ksharma.krail.trip.planner.ui.components

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import xyz.ksharma.krail.trip.planner.ui.searchstop.MODE_BLOCK_ENTER_MS
import xyz.ksharma.krail.trip.planner.ui.searchstop.MODE_BLOCK_EXIT_MS
import xyz.ksharma.krail.trip.planner.ui.searchstop.MODE_BLOCK_FADE_IN_MS
import xyz.ksharma.krail.trip.planner.ui.searchstop.MODE_BLOCK_START_SCALE
import xyz.ksharma.krail.trip.planner.ui.searchstop.modeBlockEnter
import xyz.ksharma.krail.trip.planner.ui.searchstop.modeBlockExit

/**
 * The one motion spec of the map screens: finite and short, an arrival a little slower than a departure, every fade from and
 * to nothing, the Options card on the spec's numbers and not its own, and every transition plain with reduced motion.
 */
class MapMotionTest {

    @Test
    fun `an arrival takes longer than a departure, and nothing runs longer than a third of a second`() {
        assertTrue(MapMotion.ENTER_MS > MapMotion.EXIT_MS, "A rider who has moved on is not made to wait")
        assertTrue(MapMotion.FADE_IN_MS <= MapMotion.ENTER_MS, "The fade is solid before the movement stops")
        listOf(
            MapMotion.ENTER_MS, MapMotion.FADE_IN_MS, MapMotion.EXIT_MS, MapMotion.CROSSFADE_MS,
            MapMotion.EXPAND_MS, MapMotion.SLIDE_MS,
        ).forEach { assertTrue(it in 1..350, "$it ms is short and finite") }
        assertTrue(MapMotion.ROWS_DELAY_MS < MapMotion.EXPAND_MS, "The rows begin while the height is moving")
    }

    @Test
    fun `the Options card reads the spec's numbers, so there is one set`() {
        assertEquals(MapMotion.ENTER_MS, MODE_BLOCK_ENTER_MS)
        assertEquals(MapMotion.FADE_IN_MS, MODE_BLOCK_FADE_IN_MS)
        assertEquals(MapMotion.EXIT_MS, MODE_BLOCK_EXIT_MS)
        assertEquals(MapMotion.POP_START_SCALE, MODE_BLOCK_START_SCALE)
        assertEquals(mapPopIn(false).toString(), modeBlockEnter(false).toString())
        assertEquals(mapPopOut(false).toString(), modeBlockExit(false).toString())
    }

    @Test
    fun `every arrival fades in from nothing and every departure fades out to nothing`() {
        val arrivals = listOf(mapPopIn(false), mapVanishIn(false), mapSlideUpIn(false))
        val departures = listOf(mapPopOut(false), mapVanishOut(false), mapSlideDownOut(false))
        arrivals.forEach { assertTrue("Fade(alpha=0.0" in it.toString(), "Fades in from transparent: $it") }
        departures.forEach { assertTrue("Fade(alpha=0.0" in it.toString(), "Fades out to transparent: $it") }
    }

    @Test
    fun `with reduced motion every transition is plain, with nothing to animate`() {
        listOf(mapPopIn(true), mapVanishIn(true), mapSlideUpIn(true)).forEach {
            assertEquals("EnterTransition.None", it.toString(), "Plain arrival")
        }
        listOf(mapPopOut(true), mapVanishOut(true), mapSlideDownOut(true)).forEach {
            assertEquals("ExitTransition.None", it.toString(), "Plain departure")
        }
    }
}
