package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The words the rider reads from the decision bar and the headlines, for every screen and state the model
 * can produce. `IntroBannedWordsTest` reads the Intro sources for every other string. A button is always an
 * answer, never a way past a question, and no string uses a question mark, an exclamation mark, a colon, an
 * em dash, an arrow or a middle dot. The one exception is the five headlines, which are complete questions and
 * so end in a question mark on their last line, and nothing else does. The apostrophe is plain copy.
 */
class IntroCopyTest {

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

    // The one exemption: a theme's tagline is the app's own copy, read from the theme, and is shown exactly as it is
    // written, so its exclamation mark is not the Intro's. It is a tagline or one of the lines it is split into, and
    // nothing else is exempt.
    private val taglines = KrailThemeStyle.entries.map { it.tagLine }

    // The five headlines are complete plain questions, so each ends in one question mark on its last line. Those lines
    // and the phrases they make are the only strings that may have one.
    private val headlines = listOf(
        PARK_RIDE_HEADLINE,
        DESTINATION_HEADLINE,
        ORIGIN_HEADLINE,
        CONFIRM_HEADLINE,
        PICK_THEME_HEADLINE,
    )
    private val questionStrings = headlines.flatMap { listOf(it.last(), introHeadlinePhrase(it)) } + PICK_THEME_HEADING

    private fun barWords(): List<String> = states.flatMap { state ->
        listOfNotNull(introBar(state)).flatMap { bar ->
            listOfNotNull(bar.left, bar.right).flatMap { listOf(it.label, it.spokenLabel) }
        }
    }.distinct()

    @Test
    fun `the bars are found, so this cannot pass by looking at nothing`() {
        val words = barWords()

        assertTrue(
            "Yes please, show me parking spots" in words && "Add to home" in words && "Not now" in words,
            "$words",
        )
        assertTrue("Yes, this one" in words, "the stop screens' bar was not found: $words")
        assertTrue("OK" in words, "the no station message's button was not found: $words")
        assertTrue("No, search" !in words, "the left button of the stop screens is gone: $words")
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
    fun `no button and no other string has a question mark, an exclamation mark, a colon, a dash, an arrow or a dot`() {
        val strings = barWords() + STOP_NO_MATCH + introThemeChoices().map { it.name }

        strings.forEach { text ->
            BANNED_GLYPHS.forEach { glyph -> assertTrue(!text.contains(glyph), "\"$text\" has $glyph") }
        }
    }

    @Test
    fun `the five headlines are questions, with one question mark at the end of the last line and no other banned glyph`() {
        assertEquals(listOf("Show", "parking spots", "at your", "nearest station?"), PARK_RIDE_HEADLINE)
        assertEquals(listOf("Where are you", "going?"), DESTINATION_HEADLINE)
        assertEquals(listOf("Where are you", "leaving from?"), ORIGIN_HEADLINE)
        assertEquals(listOf("Save", "this trip?"), CONFIRM_HEADLINE)
        assertEquals(listOf("Your favourite", "transport mode", "in Sydney?"), PICK_THEME_HEADLINE)
        listOf(PARK_RIDE_HEADING, DESTINATION_HEADING, ORIGIN_HEADING, CONFIRM_HEADING)
            .zip(headlines)
            .forEach { (heading, lines) -> assertEquals(lines.joinToString(" "), heading) }
        // The colour screen draws a short form of its question and says the whole of it, so its heading is not the lines joined.
        assertEquals("What's your favourite transport mode in Sydney?", PICK_THEME_HEADING)
        assertEquals(PICK_THEME_HEADING, introThemeHeadline(null).spoken)
        headlines.forEach { lines ->
            assertEquals(1, lines.joinToString(" ").count { it == QUESTION_MARK }, "$lines")
            assertTrue(lines.last().endsWith(QUESTION_MARK), "$lines")
        }
        questionStrings.forEach { text ->
            (BANNED_GLYPHS - QUESTION_MARK).forEach { glyph ->
                assertTrue(
                    !text.contains(glyph),
                    "\"$text\" has $glyph",
                )
            }
        }
    }

    @Test
    fun `a question mark anywhere else is still caught, in a button, a lead line or another string`() {
        val elsewhere = barWords() + STOP_NO_MATCH + headlines.flatMap { it.dropLast(1) }

        assertTrue(elsewhere.none { it.contains(QUESTION_MARK) })
        assertTrue(wordingProblems("Which one?").isNotEmpty())
        assertTrue(wordingProblems("Where are you going? Now").isNotEmpty())
    }

    @Test
    fun `a headline is two to four short lines, and the apostrophe is plain copy`() {
        headlines.forEach { lines ->
            assertTrue(lines.size in 2..4, "$lines")
            assertTrue(lines.all { it.isNotBlank() && it.length <= MAX_LINE_LENGTH }, "$lines")
        }
        assertTrue(wordingProblems("What's your").isEmpty())
    }

    @Test
    fun `each screen says its whole question as one text of its own, with the hero marked, and the word free is gone`() {
        assertEquals("Show parking spots at your nearest station?", PARK_RIDE_HEADING)
        assertEquals("Where are you going?", DESTINATION_HEADING)
        assertEquals("Where are you leaving from?", ORIGIN_HEADING)
        assertEquals("Save this trip?", CONFIRM_HEADING)
        assertEquals("What's your favourite transport mode in Sydney?", PICK_THEME_HEADING)
        // The hero of each question, the line that is drawn large and in the accent: the question mark of a hero is its own.
        assertEquals("parking spots", PARK_RIDE_HEADLINE[PARK_RIDE_HERO_INDEX])
        assertEquals("going?", DESTINATION_HEADLINE[STOP_HERO_INDEX])
        assertEquals("leaving from?", ORIGIN_HEADLINE[STOP_HERO_INDEX])
        assertEquals("Save", CONFIRM_HEADLINE[CONFIRM_HERO_INDEX])
        assertEquals("transport mode", PICK_THEME_HEADLINE[PICK_THEME_HERO_INDEX])
        assertEquals(PICK_THEME_HERO_INDEX, introThemeHeadline(null).emphasisIndex)
        assertEquals(STOP_HERO_INDEX, introStopHeadline(IntroStep.DESTINATION)?.emphasisIndex)
        assertEquals(STOP_HERO_INDEX, introStopHeadline(IntroStep.ORIGIN)?.emphasisIndex)
        assertEquals(DESTINATION_HEADING, introStopHeadline(IntroStep.DESTINATION)?.spoken)
        assertEquals(ORIGIN_HEADING, introStopHeadline(IntroStep.ORIGIN)?.spoken)

        val everything = barWords() + headlines.flatten() +
            listOf(PARK_RIDE_HEADING, DESTINATION_HEADING, ORIGIN_HEADING, CONFIRM_HEADING, PICK_THEME_HEADING)
        everything.forEach {
            assertTrue(
                !Regex("\\bfree\\b", RegexOption.IGNORE_CASE).containsMatchIn(it),
                "\"$it\" has the word free",
            )
        }
    }

    private fun isTagline(text: String) = taglines.any { text == it || text in taglineLines(it) }

    private fun wordingProblems(text: String): List<String> {
        if (isTagline(text)) return emptyList()
        val words = BANNED_WORDS.filter { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) }
        val glyphs = BANNED_GLYPHS.filter { text.contains(it) && !(it == QUESTION_MARK && text in questionStrings) }
        return words.map { "has the word $it" } + glyphs.map { "has U+${it.code.toString(HEX)}" }
    }

    @Test
    fun `every string of the theme screen follows the wording rules, and only the taglines of the themes are exempt`() {
        val ours = barWords() + PICK_THEME_HEADLINE + PICK_THEME_HEADING +
            introThemeChoices().map { it.name }
        val shown = introThemeChoices().flatMap { introThemeHeadline(it.style.id).lines }

        (ours + shown).forEach { text ->
            assertTrue(wordingProblems(text).isEmpty(), "\"$text\" ${wordingProblems(text)}")
        }
        assertTrue(
            ours.none { isTagline(it) },
            "something of the Intro's own is exempt: ${ours.filter { isTagline(it) }}",
        )
        assertEquals(
            introThemeChoices().flatMap { taglineLines(it.style.tagLine) },
            shown,
            "the headline after a choice is the tagline's lines and nothing else",
        )
    }

    @Test
    fun `the exemption is not a hole, because a line of our own with the same faults is still caught`() {
        listOf("Pick your colour!", "Hop on : now", "Skip the line", "Which one?", "Later, then").forEach { text ->
            assertTrue(wordingProblems(text).isNotEmpty(), "\"$text\" was not caught")
        }
        // A tagline with the faults is exempt only as the app's own, and the same text a hair different is not.
        val tagLine = KrailThemeStyle.Train.tagLine
        assertTrue(wordingProblems(tagLine).isEmpty(), "the tagline is exempt")
        assertTrue(wordingProblems("$tagLine now").isNotEmpty(), "the tagline with a word added is not")
        assertTrue(wordingProblems(tagLine.replace("track", "line")).isNotEmpty(), "a reworded tagline is not")
    }

    private companion object {
        const val HEX = 16
        val BANNED_WORDS = listOf("Skip", "Later", "Next", "Continue", "Done")

        // Written as code points so none of the banned glyphs is typed into this file.
        val BANNED_GLYPHS = listOf(0x3F, 0x21, 0x3A, 0x2014, 0x2190, 0x2192, 0x21D2, 0x00B7).map { Char(it) }
        const val MAX_LINE_LENGTH = 16
        val QUESTION_MARK = Char(0x3F)
    }
}
