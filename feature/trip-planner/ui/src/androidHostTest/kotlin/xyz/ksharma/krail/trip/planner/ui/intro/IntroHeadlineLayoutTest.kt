package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
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
 * The headline as drawn, read for real from its text layouts: ALL UPPER CASE and bigger, and at the narrowest window at
 * 200 percent text, at a phone width and at a wide window, no word of any headline is split, cut or wider than its room.
 * The longest word is what sets the ceiling the headline grows to with the system text size.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroHeadlineLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val everyHeadline: List<List<String>> =
        listOf(PICK_THEME_HEADLINE, PARK_RIDE_HEADLINE, DESTINATION_HEADLINE, ORIGIN_HEADLINE, CONFIRM_HEADLINE) +
            KrailThemeStyle.entries.map { taglineLines(it.tagLine) }

    private val lines = mutableStateOf(PICK_THEME_HEADLINE)
    private var contentSet = false

    // The text size is the real configuration's, set before the content is, so the layout reads it as a phone would.
    private fun start(widthDp: Int, fontScale: Float) {
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.width(widthDp.dp)) {
                    IntroHeadline(lines = lines.value, emphasisIndex = lines.value.lastIndex, emoji = "")
                }
            }
        }
        contentSet = true
        composeRule.waitForIdle()
    }

    private fun show(headline: List<String>) {
        check(contentSet) { "start() first" }
        lines.value = headline
        composeRule.waitForIdle()
    }

    private fun drawnLayouts(): List<TextLayoutResult> {
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

    private fun assertEveryWordWhole(widthDp: Int, fontScale: Float) {
        start(widthDp, fontScale)
        everyHeadline.forEach { headline ->
            show(headline)

            val layouts = drawnLayouts()
            assertEquals(
                headline.map { introHeadlineDisplay(it) },
                layouts.map { it.layoutInput.text.text },
                "$headline",
            )
            layouts.forEach { layout ->
                val text = layout.layoutInput.text.text
                val room = layout.layoutInput.constraints.maxWidth
                assertTrue(!layout.didOverflowWidth, "\"$text\" overflows at $widthDp dp, font $fontScale")
                assertTrue(layout.size.width <= room, "\"$text\" is ${layout.size.width} px wide in $room")
                for (line in 0 until layout.lineCount - 1) {
                    val end = layout.getLineEnd(line)
                    assertTrue(
                        text[end - 1].isWhitespace() || text[end].isWhitespace(),
                        "\"$text\" breaks inside a word at $widthDp dp, font $fontScale: line ${line + 1} is " +
                            "\"${text.substring(layout.getLineStart(line), end)}\"",
                    )
                }
                for (line in 0 until layout.lineCount) {
                    assertTrue(layout.getLineRight(line) <= room + 1f, "\"$text\" line ${line + 1} is cut at $room")
                }
            }
        }
    }

    @Test
    fun `at 320 dp wide, 288 dp of room, every word of every headline is whole and in capitals`() {
        assertEveryWordWhole(widthDp = NARROW_ROOM_DP, fontScale = 1.0f)
    }

    @Test
    fun `at a phone width every word of every headline is whole`() {
        assertEveryWordWhole(widthDp = PHONE_ROOM_DP, fontScale = 1.0f)
    }

    @Test
    fun `at a wide window every word of every headline is whole`() {
        assertEveryWordWhole(widthDp = WIDE_ROOM_DP, fontScale = 1.0f)
    }

    @Test
    fun `with text at 200 percent the headline is drawn at the ceiling, and at 100 percent at the real scale`() {
        start(NARROW_ROOM_DP, 2.0f)
        show(PICK_THEME_HEADLINE)

        assertEquals(HEADLINE_FONT_SCALE_CEILING, drawnLayouts().first().layoutInput.density.fontScale)
    }

    @Test
    fun `the headline is 1_7 times the size the display style was, at the heaviest weight, on every line`() {
        start(PHONE_ROOM_DP, 1.0f)
        show(PICK_THEME_HEADLINE)

        drawnLayouts().forEach {
            assertEquals(28f * 1.7f, it.layoutInput.style.fontSize.value, 0.01f)
            assertEquals(FontWeight.Black, it.layoutInput.style.fontWeight)
        }
        assertEquals(PICK_THEME_HEADLINE.size, drawnLayouts().size)
        assertEquals(1.0f, drawnLayouts().first().layoutInput.density.fontScale)
    }

    // The test environment's text layout does not follow the font scale, so the width at a larger text size is measured
    // by asking for the size itself: the headline's size at that scale, drawn at scale 1.
    private fun widestWordDp(scale: Float): Float {
        val density = Density(DENSITY, 1f)
        val measurer = TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(ApplicationProvider.getApplicationContext<Context>()),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
        val style = TextStyle(fontSize = (BASE_SP * HEADLINE_SIZE_FACTOR * scale).sp, fontWeight = FontWeight.Black)
        return everyHeadline.flatten().flatMap { it.split(" ") }.map { introHeadlineDisplay(it) }.distinct()
            .maxOf { word ->
                measurer.measure(
                    text = AnnotatedString(word),
                    style = style,
                    softWrap = false,
                    constraints = Constraints(),
                ).size.width / DENSITY
            }
    }

    @Test
    fun `at the ceiling the widest word of any headline fits 320 dp with a twelfth to spare, so a font that is a little wider still does`() {
        val widest = widestWordDp(headlineFontScale(2.0f))

        assertTrue(widest * FONT_MARGIN <= NARROW_ROOM_DP, "the widest word is $widest dp in $NARROW_ROOM_DP dp")
    }

    @Test
    fun `without a ceiling the widest word would not fit at 200 percent, which is why there is one`() {
        assertTrue(
            widestWordDp(2.0f) > NARROW_ROOM_DP,
            "200 percent fits without a ceiling, so the ceiling is not needed",
        )
        assertTrue(widestWordDp(HEADLINE_FONT_SCALE_CEILING) < NARROW_ROOM_DP)
    }

    @Test
    fun `when the words change the old and the new headline overlap for the change, then only the new one is left`() {
        start(PHONE_ROOM_DP, 1.0f)
        val metro = taglineLines(KrailThemeStyle.Metro.tagLine)
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread {
            lines.value = metro
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeBy(100)

        assertEquals(
            PICK_THEME_HEADLINE.size + metro.size,
            drawnLayouts().size,
            "both are on screen part way: ${drawnLayouts().map { it.layoutInput.text.text }}",
        )

        composeRule.mainClock.advanceTimeBy(HEADLINE_CHANGE_ENTER_MS + 100L)

        assertEquals(metro.map { introHeadlineDisplay(it) }, drawnLayouts().map { it.layoutInput.text.text })
    }

    private companion object {
        const val NARROW_ROOM_DP = 288
        const val DENSITY = 2.625f
        const val BASE_SP = 28f
        const val FONT_MARGIN = 1.08f
        const val PHONE_ROOM_DP = 379
        const val WIDE_ROOM_DP = 480
    }
}
