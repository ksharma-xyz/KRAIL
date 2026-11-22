package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroHeadlineMotionTest {

    @Test
    fun `the headline reads as one phrase, lines in order with a single space between`() {
        assertEquals("Driving to the station", introHeadlinePhrase(listOf("Driving", "to the", "station")))
        assertEquals("One", introHeadlinePhrase(listOf("One")))
        assertEquals("", introHeadlinePhrase(emptyList()))
    }

    @Test
    fun `the Park and Ride headline is three lines with the key word last and no punctuation`() {
        assertEquals(listOf("Driving", "to the", "station"), PARK_RIDE_HEADLINE)
        assertEquals("Driving to the station", PARK_RIDE_HEADING)
        assertTrue(PARK_RIDE_HEADLINE.size in 2..4, "two to four short lines")
    }

    @Test
    fun `each line starts one stagger after the one before it and the emphasised one takes a beat more`() {
        val rows = listOf(
            // line count, emphasis index, delays
            Triple(3, 2, listOf(0L, 70L, 180L)),
            Triple(3, 0, listOf(40L, 70L, 140L)),
            Triple(2, 1, listOf(0L, 110L)),
            Triple(4, 3, listOf(0L, 70L, 140L, 250L)),
            // An emphasis outside the lines is the plain stagger.
            Triple(3, -1, listOf(0L, 70L, 140L)),
            Triple(3, 3, listOf(0L, 70L, 140L)),
            Triple(1, 0, listOf(40L)),
            Triple(0, 0, emptyList()),
        )

        rows.forEach { (count, emphasis, delays) ->
            assertEquals(delays, headlineTimeline(count, emphasis).lineDelaysMs, "$count lines, emphasis $emphasis")
        }
    }

    @Test
    fun `the stagger is between 60 and 80 milliseconds and the emoji pops first`() {
        assertTrue(LINE_STAGGER_MS in 60L..80L)
        assertEquals(0L, headlineTimeline(3, 2).emojiDelayMs)
    }

    @Test
    fun `a line frame follows its progress, overshooting a little past place and size and then settling`() {
        val rows = listOf(
            // progress, alpha, rise, scale
            Triple(0f, LineFrame(0f, 0.5f, 0.92f), "not started"),
            Triple(0.5f, LineFrame(0.5f, 0.25f, 0.96f), "halfway"),
            Triple(1f, LineFrame(1f, 0f, 1f), "in place"),
            Triple(1.1f, LineFrame(1f, -0.05f, 1.008f), "overshoot, a touch past its place and size"),
            Triple(-0.1f, LineFrame(0f, 0.55f, 0.912f), "a spring never starts below zero, but alpha is held"),
        )

        rows.forEach { (progress, expected, name) ->
            val frame = lineFrame(progress)
            assertEquals(expected.alpha, frame.alpha, 0.0001f, "$name alpha")
            assertEquals(expected.rise, frame.rise, 0.0001f, "$name rise")
            assertEquals(expected.scale, frame.scale, 0.0001f, "$name scale")
        }
    }

    @Test
    fun `the lines that are not the emphasised one take one step down from the heaviest weight`() {
        val rows = listOf(
            FontWeight.Black to FontWeight.Bold,
            FontWeight.ExtraBold to FontWeight.Bold,
            FontWeight.Bold to FontWeight.Bold,
            // A style at or below bold is left as it is.
            FontWeight.SemiBold to FontWeight.SemiBold,
            FontWeight.Normal to FontWeight.Normal,
            null to FontWeight.Normal,
        )

        rows.forEach { (base, expected) -> assertEquals(expected, leadLineWeight(base), "base $base") }
    }

    @Test
    fun `a lead line is never heavier than the emphasised one for any weight`() {
        (100..900 step 100).forEach { weight ->
            val base = FontWeight(weight)

            assertTrue(leadLineWeight(base).weight <= base.weight, "weight $weight")
        }
    }

    @Test
    fun `the emoji wobbles once, starting and ending at rest, in under half a second and a bit`() {
        assertEquals(0f, EmojiWobble.first().second)
        assertEquals(0f, EmojiWobble.last().second)
        assertEquals(0, EmojiWobble.first().first)
        assertTrue(EmojiWobble.last().first <= MAX_WOBBLE_MS)
        assertEquals(EmojiWobble.map { it.first }, EmojiWobble.map { it.first }.sorted(), "time moves forward")
        val swings = EmojiWobble.map { it.second }.zipWithNext().count { (a, b) -> a * b < 0f }
        assertTrue(swings <= MAX_SWINGS, "one wobble is out, back and a little out again, not a shake: $swings")
        assertTrue(EmojiWobble.all { kotlin.math.abs(it.second) <= MAX_TILT }, "a small tilt only")
    }

    @Test
    fun `the headline starts in place for reduced motion, a preview, or one that already played`() {
        val rows = listOf(
            // reduce motion, inspection, played, settled
            listOf(false, false, false, false),
            listOf(true, false, false, true),
            listOf(false, true, false, true),
            listOf(false, false, true, true),
            listOf(true, true, true, true),
        )

        rows.forEach { row ->
            val (reduce, inspection, played) = row
            assertEquals(row[3], headlineStartsSettled(reduce, inspection, played), "$reduce $inspection $played")
        }
        assertFalse(headlineStartsSettled(reduceMotion = false, inspection = false, played = false))
    }

    @Test
    fun `the middle zone is centred in the room under the headline, and starts under it when there is none`() {
        val rows = listOf(
            // zone, headline, middle, top of the middle
            listOf(1000, 200, 300, 200 + 250),
            listOf(1000, 200, 800, 200),
            listOf(1000, 200, 801, 200),
            listOf(1000, 200, 799, 200),
            listOf(500, 600, 100, 600),
            listOf(1000, 0, 0, 500),
        )

        rows.forEach { row ->
            val (zone, headline, middle) = row
            assertEquals(row[3], introMiddleTop(zone, headline, middle), "zone $zone headline $headline middle $middle")
        }
    }

    private companion object {
        const val MAX_WOBBLE_MS = 600
        const val MAX_SWINGS = 3
        const val MAX_TILT = 20f
    }
}
