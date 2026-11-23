package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntroHeadlineDisplayTest {

    private class Row(val line: String, val drawn: String)

    @Test
    fun `every line is drawn in capitals, from the words as they are written`() {
        val table = listOf(
            Row("Show", "SHOW"),
            Row("parking spots", "PARKING SPOTS"),
            Row("at your", "AT YOUR"),
            Row("nearest station?", "NEAREST STATION?"),
            Row("Where are you", "WHERE ARE YOU"),
            Row("going?", "GOING?"),
            Row("leaving from?", "LEAVING FROM?"),
            Row("Save", "SAVE"),
            Row("this trip?", "THIS TRIP?"),
            Row("What's your", "WHAT'S YOUR"),
            Row("favourite", "FAVOURITE"),
            Row("transport mode", "TRANSPORT MODE"),
            Row("in Sydney?", "IN SYDNEY?"),
            Row("On the track,", "ON THE TRACK,"),
            Row("no lookin' back!", "NO LOOKIN' BACK!"),
            Row("Hoppin' the concrete jungle!", "HOPPIN' THE CONCRETE JUNGLE!"),
            Row("", ""),
        )

        table.forEach { row -> assertEquals(row.drawn, introHeadlineDisplay(row.line), row.line) }
    }

    @Test
    fun `the case rule is the invariant one, so no letter changes with the language`() {
        // Under a Turkish locale a plain i would become a dotted capital. The invariant rule keeps it an I.
        assertEquals("I", introHeadlineDisplay("i"))
        assertEquals("TITLE", introHeadlineDisplay("title"))
        assertEquals("STRASSE", introHeadlineDisplay("Stra\u00DFe"))
    }

    @Test
    fun `drawing in capitals does not touch the strings in the source, the phrase or the copy guards read`() {
        listOf(
            PICK_THEME_HEADLINE,
            PARK_RIDE_HEADLINE,
            DESTINATION_HEADLINE,
            ORIGIN_HEADLINE,
            CONFIRM_HEADLINE,
        ).forEach { lines ->
            assertEquals(lines.joinToString(" "), introHeadlinePhrase(lines))
            assertTrue(lines.any { it != it.uppercase() }, "$lines is already in capitals in the source")
        }
    }

    @Test
    fun `the headline is 1_7 times the size of the display style, in size and line height`() {
        val base = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)

        val headline = base.asHeadlineStyle()

        assertEquals(1.7f, HEADLINE_SIZE_FACTOR)
        assertEquals(47.6f, headline.fontSize.value, 0.001f)
        assertEquals(61.2f, headline.lineHeight.value, 0.001f)
    }

    @Test
    fun `the headline is at the heaviest weight whatever weight the style had`() {
        listOf(FontWeight.Normal, FontWeight.Bold, FontWeight.Black, null).forEach { weight ->
            val headline = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = weight).asHeadlineStyle()

            assertEquals(FontWeight.Black, headline.fontWeight, "from $weight")
        }
        assertEquals(900, HEADLINE_WEIGHT.weight)
    }

    @Test
    fun `the emoji sits 20 dp above the title at 100 percent text and further as the text grows, to twice that`() {
        val base = 20.dp
        val table = listOf(
            0.85f to 20.dp,
            1f to 20.dp,
            1.3f to 26.dp,
            1.5f to 30.dp,
            2f to 40.dp,
            3f to 40.dp,
        )

        table.forEach { (scale, expected) -> assertEquals(expected.value, introEmojiTitleGap(base, scale).value, 0.001f, "at $scale") }
        assertTrue(introEmojiTitleGap(base, 1f) >= 16.dp, "never under 16 dp")
    }

    @Test
    fun `the headline follows the system font scale up to the ceiling and no further`() {
        val table = listOf(
            0.85f to 0.85f,
            1f to 1f,
            1.05f to 1.05f,
            1.15f to HEADLINE_FONT_SCALE_CEILING,
            HEADLINE_FONT_SCALE_CEILING to HEADLINE_FONT_SCALE_CEILING,
            1.5f to HEADLINE_FONT_SCALE_CEILING,
            2f to HEADLINE_FONT_SCALE_CEILING,
            3f to HEADLINE_FONT_SCALE_CEILING,
        )

        table.forEach { (system, drawn) -> assertEquals(drawn, headlineFontScale(system), "at $system") }
        assertTrue(
            HEADLINE_FONT_SCALE_CEILING > 1f,
            "the ceiling must let the headline grow with a larger text size at all",
        )
    }

    @Test
    fun `in a room too narrow for the widest word the headline is held lower, and in any other room it is the ceiling alone`() {
        val narrow = 288.dp
        val heldAt = narrow.value / (WIDEST_HEADLINE_WORD_DP * HEADLINE_WORD_MARGIN)
        val table = listOf(
            // system scale, room, drawn
            Triple(1f, narrow, heldAt),
            Triple(2f, narrow, heldAt),
            Triple(0.85f, narrow, 0.85f),
            Triple(1f, 328.dp, 1f),
            Triple(2f, 328.dp, HEADLINE_FONT_SCALE_CEILING),
            Triple(2f, 379.dp, HEADLINE_FONT_SCALE_CEILING),
            Triple(2f, 480.dp, HEADLINE_FONT_SCALE_CEILING),
            Triple(2f, Dp.Unspecified, HEADLINE_FONT_SCALE_CEILING),
        )

        table.forEach { (system, room, drawn) ->
            assertEquals(drawn, headlineFontScale(system, room), "at $system in $room")
        }
        assertTrue(heldAt in 0.9f..1f, "the narrowest room holds the headline just under its style: $heldAt")
        assertTrue(
            WIDEST_HEADLINE_WORD_DP * headlineFontScale(2f, narrow) * HEADLINE_WORD_MARGIN <= narrow.value + 0.01f,
            "the widest word is not whole with a twelfth to spare in the narrowest room",
        )
    }
}
