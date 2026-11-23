package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The wordmark at 320 dp, the smallest window the app supports, at 100 percent text and at 200 percent text, both
 * drawn in one screen so they can be compared. Each is two lines, 6 letters and 5, with every letter in place.
 *
 * What it holds is that the mark is scaled down to the room and never squeezed: no row is as wide as the room it
 * was given (a row that did not fit would be cut to exactly that), no two letters of a line overlap, no letter is cut
 * by the window, and the letters at 200 percent are the same size as at 100 percent, where without the fit they
 * would be twice as large.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h1280dp-xxhdpi",
    manifest = Config.NONE,
)
class FinaleWordmarkNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settled = mutableStateOf(finaleFrameAt(FINALE_ENTER_END_MS, reduceMotion = false))

    @Composable
    private fun WordmarkAt(fontScale: Float) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale)) {
            FinaleWordmark(
                frame = settled,
                themeColour = KrailThemeStyle.Train.hexColorCode.hexToComposeColor(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = SIDE_PADDING),
            )
        }
    }

    // Four rows of letters, top to bottom: the two lines at 100 percent text, then the two at 200 percent.
    private fun showBoth(): List<List<Rect>> {
        composeRule.setContent {
            PreviewTheme(themeStyle = KrailThemeStyle.Train) {
                Column {
                    WordmarkAt(fontScale = 1f)
                    WordmarkAt(fontScale = LARGE_TEXT)
                }
            }
        }
        composeRule.waitForIdle()
        val letters = composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        val byLine = letters.groupBy { it.top.toInt() / LINE_BUCKET_PX }
        return byLine.keys.sorted().map { key -> byLine.getValue(key).sortedBy { it.left } }
    }

    @Test
    fun `at both text sizes the wordmark is two whole lines of 6 and 5 letters`() {
        val rows = showBoth()

        assertEquals(listOf(6, 5, 6, 5), rows.map { it.size })
    }

    @Test
    fun `no row fills the room it was given, so none was cut, and none is cut by the window`() {
        val rows = showBoth()

        rows.forEach { row ->
            val span = row.last().right - row.first().left
            assertTrue(span <= ROOM_PX - 2f, "a row is as wide as its room, so it did not fit: $span of $ROOM_PX")
            row.forEach { letter ->
                assertTrue(letter.left >= 0f && letter.right <= WINDOW_PX, "a letter is cut by the window: $letter")
            }
        }
    }

    @Test
    fun `no two letters of a line overlap`() {
        showBoth().forEach { row ->
            row.zipWithNext().forEach { (left, right) ->
                assertTrue(right.left >= left.right - OVERLAP_PX, "letters overlap: $left and $right")
            }
        }
    }

    @Test
    fun `the letters at 200 percent text are the size of those at 100 percent, scaled to fit and not larger`() {
        val rows = showBoth()

        val normal = rows[0].first().height
        val large = rows[2].first().height
        assertTrue(
            abs(large - normal) <= normal * SIZE_TOLERANCE,
            "$large at 200 percent against $normal at 100 percent",
        )
    }

    private companion object {
        val SIDE_PADDING = 16.dp
        const val LARGE_TEXT = 2f
        const val WINDOW_PX = 960f // 320 dp at xxhdpi
        const val ROOM_PX = 864f // the window less the 16 dp of padding on each side
        const val LINE_BUCKET_PX = 20
        const val OVERLAP_PX = 1f
        const val SIZE_TOLERANCE = 0.05f
    }
}
