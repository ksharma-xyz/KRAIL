package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntroHeadlineDisplayTest {

    private class Row(val line: String, val drawn: String)

    @Test
    fun `every line is drawn in capitals, from the words as they are written`() {
        val table = listOf(
            Row("Pick", "PICK"),
            Row("your ride", "YOUR RIDE"),
            Row("colour", "COLOUR"),
            Row("Driving", "DRIVING"),
            Row("to the", "TO THE"),
            Row("station", "STATION"),
            Row("Where do", "WHERE DO"),
            Row("you go", "YOU GO"),
            Row("often", "OFTEN"),
            Row("you start", "YOU START"),
            Row("Save", "SAVE"),
            Row("this trip", "THIS TRIP"),
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
    fun `the headline is 1_4 times the size of the display style, in size and line height`() {
        val base = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)

        val headline = base.asHeadlineStyle()

        assertEquals(1.4f, HEADLINE_SIZE_FACTOR)
        assertEquals(39.2f, headline.fontSize.value, 0.001f)
        assertEquals(50.4f, headline.lineHeight.value, 0.001f)
    }

    @Test
    fun `the headline follows the system font scale up to the ceiling and no further`() {
        val table = listOf(
            0.85f to 0.85f,
            1f to 1f,
            1.15f to 1.15f,
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
}
