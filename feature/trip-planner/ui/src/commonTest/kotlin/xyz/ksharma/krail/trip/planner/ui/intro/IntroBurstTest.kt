package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroBurstTest {

    // region the shapes

    @Test
    fun `the burst is twelve shapes spread evenly round a full turn`() {
        val particles = burstParticles()

        assertEquals(BURST_COUNT, particles.size)
        assertEquals(12, BURST_COUNT)
        assertEquals((0 until 12).map { it * 30f }, particles.map { it.angle })
    }

    @Test
    fun `the shapes alternate circle and square and are plain shapes only`() {
        val shapes = burstParticles().map { it.shape }

        assertEquals(List(BURST_COUNT) { if (it % 2 == 0) BurstShape.Circle else BurstShape.Square }, shapes)
        assertEquals(setOf(BurstShape.Circle, BurstShape.Square), BurstShape.entries.toSet())
    }

    @Test
    fun `every shape has a reach inside the ring, a size, and only squares turn`() {
        burstParticles().forEach { particle ->
            assertTrue(particle.reach in 0.7f..1.0f, "$particle")
            assertTrue(particle.size in 0.16f..0.26f, "$particle")
            assertEquals(particle.shape == BurstShape.Square, particle.spin > 0f, "$particle")
        }
        assertTrue(burstParticles().map { it.reach }.distinct().size > 1, "the ring is not a perfect circle")
    }

    @Test
    fun `the shapes are a pure function of the count, so the burst looks the same every time`() {
        assertEquals(burstParticles(), burstParticles())
        assertEquals(6, burstParticles(count = 6).size)
        assertNotEquals(burstParticles(count = 6), burstParticles(count = 8))
    }

    // endregion

    // region one frame

    @Test
    fun `at the start nothing has moved and everything shows, and at the end nothing shows`() {
        val start = burstFrame(0f)
        val end = burstFrame(1f)

        assertEquals(BurstFrame(distance = 0f, alpha = 1f, scale = 1f), start)
        assertEquals(1f, end.distance)
        assertEquals(0f, end.alpha)
        assertTrue(abs(end.scale - 0.4f) < TOLERANCE, "the shapes end at 40 percent of their size: ${end.scale}")
    }

    @Test
    fun `the shapes fly fast and slow down, fade out and shrink, never going backwards`() {
        val frames = (0..20).map { burstFrame(it / 20f) }

        frames.zipWithNext().forEach { (before, after) ->
            assertTrue(after.distance >= before.distance, "distance went back: $before $after")
            assertTrue(after.alpha <= before.alpha, "alpha went up: $before $after")
            assertTrue(after.scale <= before.scale, "scale went up: $before $after")
        }
        val firstHalf = burstFrame(0.5f).distance
        assertTrue(firstHalf > 0.5f, "more than half the way is flown in half the time: $firstHalf")
    }

    @Test
    fun `a progress outside zero to one is held at the nearest end`() {
        assertEquals(burstFrame(0f), burstFrame(-3f))
        assertEquals(burstFrame(1f), burstFrame(7f))
    }

    // endregion

    // region which celebration

    private class Showing(
        val celebrate: Boolean,
        val reduceMotion: Boolean,
        val inspection: Boolean,
        val played: Boolean,
        val expected: CelebrationMode,
    ) {
        fun describe() = "celebrate=$celebrate reduce=$reduceMotion inspection=$inspection played=$played"
    }

    @Test
    fun `the celebration for every way the screen can be shown`() {
        val rows = listOf(
            Showing(true, reduceMotion = false, inspection = false, played = false, CelebrationMode.Burst),
            Showing(true, reduceMotion = true, inspection = false, played = false, CelebrationMode.Fade),
            Showing(true, reduceMotion = false, inspection = true, played = false, CelebrationMode.Settled),
            Showing(true, reduceMotion = true, inspection = true, played = false, CelebrationMode.Settled),
            Showing(true, reduceMotion = false, inspection = false, played = true, CelebrationMode.Settled),
            Showing(true, reduceMotion = true, inspection = false, played = true, CelebrationMode.Settled),
            Showing(false, reduceMotion = false, inspection = false, played = false, CelebrationMode.Settled),
            Showing(false, reduceMotion = true, inspection = false, played = false, CelebrationMode.Settled),
        )

        rows.forEach { row ->
            val actual = celebrationMode(row.celebrate, row.reduceMotion, row.inspection, row.played)

            assertEquals(row.expected, actual, row.describe())
        }
    }

    @Test
    fun `the burst takes about eight tenths of a second and the plain fade a fifth`() {
        assertEquals(800, BURST_DURATION_MS)
        assertEquals(200, CELEBRATION_FADE_MS)
    }

    // endregion

    // region the hand off

    @Test
    fun `with no finale host the end of the Intro goes straight to Saved Trips, once, and starts no finale`() {
        val finale = IntroFinaleSignal()
        var calls = 0

        onIntroFinished(finale, themeHexColorCode = KrailThemeStyle.Metro.hexColorCode) { calls++ }

        assertEquals(1, calls)
        assertNull(finale.request.value)
    }

    @Test
    fun `with a finale host the end of the Intro starts the finale in the theme colour and does not navigate yet`() {
        val finale = IntroFinaleSignal()
        finale.attachHost()
        var calls = 0

        onIntroFinished(finale, themeHexColorCode = KrailThemeStyle.Metro.hexColorCode) { calls++ }

        assertEquals(0, calls)
        assertEquals(KrailThemeStyle.Metro.hexColorCode, finale.request.value?.themeHexColorCode)
    }

    @Test
    fun `a second finish while a finale is playing goes straight to Saved Trips and starts no second finale`() {
        val finale = IntroFinaleSignal()
        finale.attachHost()
        var calls = 0
        onIntroFinished(finale, themeHexColorCode = KrailThemeStyle.Train.hexColorCode) { calls++ }
        val first = finale.request.value

        onIntroFinished(finale, themeHexColorCode = KrailThemeStyle.Bus.hexColorCode) { calls++ }

        assertEquals(1, calls)
        assertEquals(first?.id, finale.request.value?.id)
    }

    // endregion
}

private const val TOLERANCE = 0.0001f
