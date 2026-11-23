package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colour screen's question in three lines, as drawn: ALL UPPER CASE, at the largest size at which its widest line fits the
 * window, never above the size of the other headlines and never below the floor, and a line that still does not fit wraps at a
 * word, so it is four lines and never a split word. A screen reader hears the whole question. The room is the window less the
 * 16 dp each side the Intro's frame leaves, so 411 dp is 379 and 320 dp is 288.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroThemeQuestionFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roomNow = mutableIntStateOf(PHONE_ROOM)

    private fun start(room: Int, fontScale: Float, lines: List<String> = PICK_THEME_HEADLINE, fit: Boolean = true) {
        RuntimeEnvironment.setFontScale(fontScale)
        roomNow.intValue = room
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalHeadlineRoom provides roomNow.intValue.dp) {
                    Box(modifier = Modifier.width(roomNow.intValue.dp)) {
                        IntroHeadline(
                            lines = lines,
                            emphasisIndex = lines.lastIndex,
                            emoji = "",
                            spoken = if (fit) PICK_THEME_HEADING else introHeadlinePhrase(lines),
                            fitToRoom = fit,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun drawn(): List<TextLayoutResult> {
        val nodes = composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        return nodes.map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single()
        }
    }

    private fun lineCount() = drawn().sumOf { it.lineCount }

    private fun scale() = drawn().first().layoutInput.density.fontScale

    private fun assertNoWordSplit() {
        drawn().forEach { layout ->
            val text = layout.layoutInput.text.text
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                assertTrue(
                    text[end - 1].isWhitespace() || text[end].isWhitespace(),
                    "\"$text\" breaks inside a word: line ${line + 1} is " +
                        "\"${text.substring(layout.getLineStart(line), end)}\"",
                )
            }
            assertTrue(!layout.didOverflowWidth, "\"$text\" is cut")
        }
    }

    @Test
    fun `at 411 dp and 100 percent the question is three lines in capitals, each whole, at the largest size that fits`() {
        start(room = PHONE_ROOM, fontScale = 1.0f)

        assertEquals(PICK_THEME_HEADLINE.map { introHeadlineDisplay(it) }, drawn().map { it.layoutInput.text.text })
        assertEquals(3, lineCount())
        assertNoWordSplit()
        // The widest line, TRANSPORT MODE, is about 421 dp at the normal size, so 379 dp of room is about 0.90 of it.
        assertEquals(0.89f, scale(), 0.02f)
        assertTrue(scale() < 1.0f && scale() >= headlineFitFloor(1.0f))
    }

    @Test
    fun `at 320 dp and 100 percent it is still three lines, smaller, above the floor`() {
        start(room = NARROW_ROOM, fontScale = 1.0f)

        assertEquals(3, lineCount())
        assertNoWordSplit()
        assertEquals(0.68f, scale(), 0.02f)
        assertTrue(scale() >= headlineFitFloor(1.0f), "the scale ${scale()} is below the floor")
    }

    @Test
    fun `at 320 dp and 200 percent it holds at the floor and takes four lines, with no word split`() {
        start(room = NARROW_ROOM, fontScale = 2.0f)

        assertEquals(headlineFitFloor(2.0f), scale(), 0.0001f)
        assertEquals(0.75f, scale(), 0.0001f)
        assertEquals(4, lineCount(), "TRANSPORT MODE wraps at its space, and the other two lines still fit")
        assertNoWordSplit()
        assertEquals(PICK_THEME_HEADLINE.map { introHeadlineDisplay(it) }, drawn().map { it.layoutInput.text.text })
    }

    @Test
    fun `in a window too narrow for the floor a line wraps and the scale stays at the floor`() {
        start(room = FOLD_COVER_ROOM, fontScale = 2.0f)

        assertEquals(0.75f, scale(), 0.0001f)
        assertTrue(lineCount() > 3)
        assertNoWordSplit()
    }

    @Test
    fun `the question is never drawn larger than the other headlines are, in any room or at any text size`() {
        start(room = NARROW_ROOM, fontScale = 1.0f)

        listOf(NARROW_ROOM, PHONE_ROOM, FULL_ROOM).forEach { room ->
            roomNow.intValue = room
            composeRule.waitForIdle()
            assertTrue(
                scale() <= headlineFontScale(1.0f, room.dp) + 0.0001f,
                "in $room dp the question is ${scale()}, over ${headlineFontScale(1.0f, room.dp)}",
            )
        }
    }

    @Test
    fun `with room to spare, as with smaller text, it is drawn at the normal size, in three lines`() {
        start(room = PHONE_ROOM, fontScale = 0.85f)

        assertEquals(headlineFontScale(0.85f, PHONE_ROOM.dp), scale())
        assertEquals(3, lineCount())
    }

    @Test
    fun `a screen reader hears the whole question as one heading, not the three drawn lines`() {
        start(room = PHONE_ROOM, fontScale = 1.0f)

        val heading = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        assertEquals(listOf(PICK_THEME_HEADING), heading.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        assertEquals("What's your favourite transport mode in Sydney?", PICK_THEME_HEADING)
    }

    @Test
    fun `a tagline is drawn at the ordinary headline size, whatever the question is fitted to`() {
        val tagline = taglineLines(KrailThemeStyle.Train.tagLine)
        start(room = NARROW_ROOM, fontScale = 1.0f, lines = tagline, fit = false)

        assertEquals(headlineFontScale(1.0f, NARROW_ROOM.dp), scale())
        assertNoWordSplit()
    }

    private companion object {
        const val NARROW_ROOM = 288
        const val PHONE_ROOM = 379
        const val FULL_ROOM = 411
        const val FOLD_COVER_ROOM = 248
    }
}
