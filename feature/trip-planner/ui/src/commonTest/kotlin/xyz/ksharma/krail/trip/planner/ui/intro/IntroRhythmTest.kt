package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntroRhythmTest {

    private val dim = KrailDimensions()

    @Test
    fun `the rhythm is made of the dimension tokens and nothing of its own`() {
        val rhythm = introRhythm(dim)

        assertEquals(dim.spacingXL, rhythm.top)
        assertEquals(dim.spacingXXXL, rhythm.titleToMiddle)
        assertEquals(dim.spacingXL, rhythm.middleBottom)
        assertEquals(dim.spacingXL, rhythm.fieldToList)
    }

    @Test
    fun `the numbers are 16 dp above the headline, 24 dp under it, and 16 dp under the middle zone and under the search field`() {
        assertEquals(IntroRhythm(top = 16.dp, titleToMiddle = 24.dp, middleBottom = 16.dp, fieldToList = 16.dp), introRhythm(dim))
    }

    @Test
    fun `every gap is clear, at least 16 dp, so no screen has two things touching`() {
        val rhythm = introRhythm(dim)

        listOf(rhythm.top, rhythm.titleToMiddle, rhythm.middleBottom, rhythm.fieldToList).forEach {
            assertTrue(it >= 16.dp, "a gap is only $it")
        }
        assertTrue(rhythm.titleToMiddle >= rhythm.fieldToList, "the title is further from what it asks than the field from its list")
    }

    @Test
    fun `the room under the emoji is never less than the room between the headline's own lines`() {
        listOf(0.85f, 1f, 1.3f, 2f).forEach { scale ->
            assertTrue(introEmojiTitleGap(dim.spacingXXL, scale) > dim.spacingS, "at $scale")
        }
    }
}
