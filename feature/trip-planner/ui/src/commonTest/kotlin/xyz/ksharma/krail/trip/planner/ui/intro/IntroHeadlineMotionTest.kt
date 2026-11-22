package xyz.ksharma.krail.trip.planner.ui.intro

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

    @Test
    fun `a headline that changes on screen fades and rises over 350 ms with no overshoot, and is a plain swap with reduced motion`() {
        val moving = headlineChangeSpec(reduceMotion = false)
        val still = headlineChangeSpec(reduceMotion = true)

        assertEquals(HeadlineChangeSpec(animated = true, enterMs = 350, exitMs = 175, riseDp = 16), moving)
        assertEquals(HeadlineChangeSpec(animated = false, enterMs = 0, exitMs = 0, riseDp = 0), still)
        assertTrue(moving.exitMs <= moving.enterMs, "the old words are gone before the new ones settle")
    }

    @Test
    fun `two headlines are the same only when the words, the key word and the emoji all match`() {
        val base = HeadlineContent(listOf("On the track,", "no lookin' back!"), 1, "\uD83D\uDE86")

        assertEquals(base, base.copy())
        assertFalse(base == base.copy(lines = listOf("On the track,", "no lookin' back")))
        assertFalse(base == base.copy(emphasisIndex = 0))
        assertFalse(base == base.copy(emoji = "\uD83D\uDE87"))
    }

    private companion object {
        const val MAX_WOBBLE_MS = 600
        const val MAX_SWINGS = 3
        const val MAX_TILT = 20f
    }
}
