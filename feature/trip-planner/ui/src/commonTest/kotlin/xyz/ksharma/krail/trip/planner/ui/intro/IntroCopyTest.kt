package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The words the rider reads from the decision bar and the headlines, for every screen and state the model
 * can produce. `IntroBannedWordsTest` reads the Intro sources for every other string. A button is always an
 * answer, never a way past a question, and no string uses a question mark, an exclamation mark, a colon, an
 * em dash, an arrow or a middle dot.
 */
class IntroCopyTest {

    private val readiness = listOf(
        StopsReadiness.Unknown,
        StopsReadiness.Loading,
        StopsReadiness.Ready(stopCount = 100),
        StopsReadiness.Failed(attempt = 1, reason = "disk full"),
    )

    private val cards = listOf(
        ParkRideCardPhase.None,
        ParkRideCardPhase.NoStationNear,
        ParkRideCardPhase.Locating(),
        ParkRideCardPhase.Locating(stationId = "1"),
    ) + ParkRideAvailabilityState.entries.map { availability ->
        ParkRideCardPhase.Card(ParkRideCardStation("1", "Bella Vista", availability = availability))
    }

    private val states = IntroStep.entries.flatMap { step ->
        ParkRideAnswer.entries.flatMap { answer ->
            cards.flatMap { card ->
                listOf(false, true).map { saved ->
                    IntroState(step = step, parkRideAnswer = answer, parkRideCard = card, saved = saved)
                }
            }
        }
    }

    private fun barWords(): List<String> = states.flatMap { state ->
        readiness.mapNotNull { introBar(state, it) }.flatMap { bar ->
            listOf(bar.left, bar.right).flatMap { listOfNotNull(it.label, it.compactLabel) }
        }
    }.distinct()

    @Test
    fun `the bars are found, so this cannot pass by looking at nothing`() {
        val words = barWords()

        assertTrue("Yes, show me free spots" in words && "Add to home" in words && "Not now" in words, "$words")
    }

    @Test
    fun `no button on any screen or in any state is worded as a way past a question`() {
        barWords().forEach { words ->
            BANNED_WORDS.forEach { banned ->
                assertTrue(
                    !Regex("\\b$banned\\b", RegexOption.IGNORE_CASE).containsMatchIn(words),
                    "$words has $banned",
                )
            }
        }
    }

    @Test
    fun `no button and no headline has a question mark, an exclamation mark, a colon, a dash, an arrow or a dot`() {
        val strings = barWords() + PARK_RIDE_HEADLINE + FIRST_TRIP_HEADLINE + PARK_RIDE_HEADING + BACK_DESCRIPTION

        strings.forEach { text ->
            BANNED_GLYPHS.forEach { glyph -> assertTrue(!text.contains(glyph), "\"$text\" has $glyph") }
        }
    }

    @Test
    fun `a headline is two to four short lines`() {
        listOf(PARK_RIDE_HEADLINE, FIRST_TRIP_HEADLINE).forEach { lines ->
            assertTrue(lines.size in 2..4, "$lines")
            assertTrue(lines.all { it.isNotBlank() && it.length <= MAX_LINE_LENGTH }, "$lines")
        }
    }

    private companion object {
        val BANNED_WORDS = listOf("Skip", "Later", "Next", "Continue", "Done")

        // Written as code points so none of the banned glyphs is typed into this file.
        val BANNED_GLYPHS = listOf(0x3F, 0x21, 0x3A, 0x2014, 0x2190, 0x2192, 0x21D2, 0x00B7).map { Char(it) }
        const val MAX_LINE_LENGTH = 16
    }
}
