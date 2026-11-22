package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
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
    fun `the Intro offers the four transport themes, each with its transport mode and name, in this order`() {
        assertEquals(
            listOf(
                Triple(KrailThemeStyle.Train, TransportMode.Train, "Train"),
                Triple(KrailThemeStyle.Metro, TransportMode.Metro, "Metro"),
                Triple(KrailThemeStyle.Bus, TransportMode.Bus, "Bus"),
                Triple(KrailThemeStyle.Ferry, TransportMode.Ferry, "Ferry"),
            ),
            choices.map { Triple(it.style, it.mode, it.name) },
        )
    }

    @Test
    fun `the badge fill is the transport mode's own colour and it is the theme colour, so the recolour never disagrees`() {
        // The badge is TransportModeIcon, which fills with NswTransportConfig.colorFor(mode). The theme the app
        // switches to is the style's own hex. Both are read from their real sources here.
        val modeFills = choices.map { NswTransportConfig.colorFor(it.mode).uppercase() }
        val themeFills = choices.map { "#" + it.style.hexColorCode.uppercase().takeLast(HEX_RGB_LENGTH) }

        assertEquals(themeFills, modeFills, "mode fills $modeFills, theme colours $themeFills")
        assertEquals(
            listOf("#F6891F", "#009B77", "#00B5EF", "#5AB031"),
            modeFills,
            "the four identity colours",
        )
    }

    @Test
    fun `the theme id is the mode's product class, and the spoken name is the mode's own name`() {
        choices.forEach {
            assertEquals(it.mode.productClass, it.style.id, it.name)
            assertEquals(NswTransportConfig.nameFor(it.mode), it.name)
        }
    }

    @Test
    fun `every choice is a theme of the app, and the fun themes are not offered`() {
        choices.forEach { assertTrue(it.style in KrailThemeStyle.entries, it.name) }
        assertTrue(KrailThemeStyle.PurpleDrip !in choices.map { it.style })
        assertTrue(KrailThemeStyle.BarbiePink !in choices.map { it.style })
    }

    @Test
    fun `each theme has its own id, mode and tag, and the tag is the one the Maestro flow taps`() {
        assertEquals(choices.size, choices.map { it.style.id }.toSet().size)
        assertEquals(choices.size, choices.map { it.mode }.toSet().size)
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
    fun `before a choice the headline is the plain question about the favourite transport mode, in four lines`() {
        val headline = introThemeHeadline(null)

        assertEquals(listOf("What's your", "favourite", "transport mode", "in Sydney?"), headline.lines)
        assertEquals("in Sydney?", headline.lines[headline.emphasisIndex])
        assertEquals("What's your favourite transport mode in Sydney?", PICK_THEME_HEADING)
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
    fun `the headline variants are the question and each theme's tagline lines, and the room is the longest as drawn`() {
        val variants = themeHeadlineVariants()

        assertEquals(1 + choices.size, variants.size)
        assertEquals(PICK_THEME_HEADLINE, variants.first())
        assertEquals(choices.map { taglineLines(it.style.tagLine) }, variants.drop(1))

        // Each line wraps by itself, so a variant takes the sum of its lines. The lines are measured in capitals.
        val seen = mutableListOf<String>()
        val room = reservedHeadlineLines(variants) { drawn ->
            seen += drawn
            if (drawn.length > TEN) 2 else 1
        }

        assertTrue(seen.all { it == it.uppercase() }, "a line was measured in lower case: $seen")
        val expected = variants.maxOf { lines -> lines.sumOf { if (it.length > TEN) 2 else 1 } }
        assertEquals(expected, room)
        assertEquals(1, reservedHeadlineLines(emptyList()) { 1 })
        assertEquals(1, reservedHeadlineLines(listOf(emptyList())) { 1 })
    }

    @Test
    fun `the finale floods in the theme the rider chose, and in the default theme when there is none`() {
        choices.forEach { assertEquals(it.style.hexColorCode, finaleThemeHex(it.style.id), it.name) }
        assertEquals(DEFAULT_THEME_STYLE.hexColorCode, finaleThemeHex(null))
        assertEquals(DEFAULT_THEME_STYLE.hexColorCode, finaleThemeHex(KrailThemeStyle.PurpleDrip.id))
        assertEquals(DEFAULT_THEME_STYLE.hexColorCode, finaleThemeHex(-1))
    }

    private companion object {
        const val NOT_A_THEME = 12_345
        const val HEX_RGB_LENGTH = 6
        const val TEN = 10
    }
}
