package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroThemeChoicesTest {

    private val choices = introThemeChoices()

    @Test
    fun `the Intro offers the four transport themes, with their letters and names, in this order`() {
        assertEquals(
            listOf(
                Triple(KrailThemeStyle.Train, "T", "Train"),
                Triple(KrailThemeStyle.Metro, "M", "Metro"),
                Triple(KrailThemeStyle.Bus, "B", "Bus"),
                Triple(KrailThemeStyle.Ferry, "F", "Ferry"),
            ),
            choices.map { Triple(it.style, it.letter, it.name) },
        )
    }

    @Test
    fun `every choice is a theme of the app, and the fun themes are not offered`() {
        choices.forEach { assertTrue(it.style in KrailThemeStyle.entries, it.name) }
        assertTrue(KrailThemeStyle.PurpleDrip !in choices.map { it.style })
        assertTrue(KrailThemeStyle.BarbiePink !in choices.map { it.style })
    }

    @Test
    fun `each theme has its own id, letter and tag, and the tag is the one the Maestro flow taps`() {
        assertEquals(choices.size, choices.map { it.style.id }.toSet().size)
        assertEquals(choices.size, choices.map { it.letter }.toSet().size)
        assertEquals(
            listOf(
                TripPlannerTestTags.INTRO_THEME_TRAIN,
                TripPlannerTestTags.INTRO_THEME_METRO,
                TripPlannerTestTags.INTRO_THEME_BUS,
                TripPlannerTestTags.INTRO_THEME_FERRY,
            ),
            choices.map { it.tag },
        )
        assertEquals(
            listOf("intro.theme.train", "intro.theme.metro", "intro.theme.bus", "intro.theme.ferry"),
            choices.map { it.tag },
        )
    }

    @Test
    fun `the choice for an id is found, and an id the Intro does not offer is not`() {
        assertEquals("Metro", themeChoiceFor(KrailThemeStyle.Metro.id)?.name)
        assertNull(themeChoiceFor(null))
        assertNull(themeChoiceFor(KrailThemeStyle.PurpleDrip.id))
        assertNull(themeChoiceFor(KrailThemeStyle.BarbiePink.id))
        assertNull(themeChoiceFor(NOT_A_THEME))
    }

    @Test
    fun `the hex to switch to is the theme's own, and null when the Intro does not offer the id`() {
        choices.forEach { assertEquals(it.style.hexColorCode, themeHexFor(it.style.id), it.name) }
        assertNull(themeHexFor(null))
        assertNull(themeHexFor(KrailThemeStyle.BarbiePink.id))
    }

    @Test
    fun `the left button moves to the next choice and wraps, and the first press picks the first`() {
        val ids = choices.map { it.style.id }
        val expected = listOf(
            Row("nothing chosen", null, ids[0]),
            Row("an id that is not offered", NOT_A_THEME, ids[0]),
            Row("a fun theme that is not offered", KrailThemeStyle.PurpleDrip.id, ids[0]),
            Row("the first", ids[0], ids[1]),
            Row("the second", ids[1], ids[2]),
            Row("the third", ids[2], ids[3]),
            Row("the last wraps to the first", ids[3], ids[0]),
        )

        expected.forEach { row -> assertEquals(row.next, nextThemeChoice(row.current).style.id, row.name) }
    }

    @Test
    fun `cycling visits every choice once before it comes round`() {
        var current: Int? = null
        val seen = List(choices.size) { nextThemeChoice(current).also { current = it.style.id }.style.id }

        assertEquals(choices.map { it.style.id }, seen)
        assertEquals(choices.first().style.id, nextThemeChoice(current).style.id)
    }

    @Test
    fun `a tagline splits after its first comma and is one line without one, and the lines are the tagline again`() {
        val table = listOf(
            "On the track, no lookin' back!" to listOf("On the track,", "no lookin' back!"),
            "Surf the sub, no cap!" to listOf("Surf the sub,", "no cap!"),
            "Hoppin' the concrete jungle!" to listOf("Hoppin' the concrete jungle!"),
            "one, two, three" to listOf("one,", "two, three"),
            "ends with a comma," to listOf("ends with a comma,"),
            "no   space,after" to listOf("no   space,", "after"),
            "" to listOf(""),
        )

        table.forEach { (tagLine, lines) -> assertEquals(lines, taglineLines(tagLine), tagLine) }
    }

    @Test
    fun `every offered theme's tagline is shown exactly as the app wrote it, once its lines are joined`() {
        choices.forEach { choice ->
            val lines = taglineLines(choice.style.tagLine)

            assertEquals(choice.style.tagLine, lines.joinToString(separator = " "), choice.name)
            assertTrue(lines.size in 1..2, choice.name)
        }
    }

    @Test
    fun `before a choice the headline asks for the colour, and the key word is the last line`() {
        val headline = introThemeHeadline(null)

        assertEquals(listOf("Pick", "your ride", "colour"), headline.lines)
        assertEquals("colour", headline.lines[headline.emphasisIndex])
        assertEquals("Pick your ride colour", PICK_THEME_HEADING)
        assertEquals(headline, introThemeHeadline(NOT_A_THEME), "an id that is not offered is no choice")
    }

    @Test
    fun `after a choice the headline is that theme's tagline, with the last line the heaviest`() {
        choices.forEach { choice ->
            val headline = introThemeHeadline(choice.style.id)

            assertEquals(taglineLines(choice.style.tagLine), headline.lines, choice.name)
            assertEquals(headline.lines.lastIndex, headline.emphasisIndex, choice.name)
            assertNotNull(headline.lines.getOrNull(headline.emphasisIndex), choice.name)
        }
    }

    @Test
    fun `the headline fits in the room the screen keeps for it`() {
        val longest = (listOf(null) + choices.map { it.style.id }).maxOf { introThemeHeadline(it).lines.size }

        assertTrue(longest <= THEME_HEADLINE_RESERVED_LINES, "$longest lines")
    }

    private class Row(val name: String, val current: Int?, val next: Int)

    private companion object {
        const val NOT_A_THEME = 12_345
    }
}
