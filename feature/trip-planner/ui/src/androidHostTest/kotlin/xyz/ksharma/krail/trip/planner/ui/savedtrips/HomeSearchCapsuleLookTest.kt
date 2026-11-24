package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.md_theme_dark_modal_sheet_background
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.intro.IntroPixels
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the home capsule looks like, read back as pixels from four capsules, one in each of four theme colours, drawn on the page.
 *
 * The fill is white in light mode and taj's raised dark surface in dark mode at 92 percent, laid over the page: the one neutral
 * colour in every theme, so no theme colour reaches it. It has no border and no highlight: the top and bottom rows of the pill are
 * its straight edge, so a stroke of any colour would show in them and not in the row a few pixels inside, and they must read the
 * same. The soft shadow that sets it apart is drawn behind the whole shape, so 8 percent of it shows through the 92 percent fill,
 * about 3 levels of 255 in light mode and under 2 in dark. That is the tolerance on the fill, and it is far under what a theme
 * colour or a border would add: the four themes must read the same to 1.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w411dp-h891dp")
class HomeSearchCapsuleLookTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Read(val page: Color, val base: Color)

    private fun drawCapsules(mode: ThemeMode): Read {
        var read: Read? = null
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                read = Read(page = KrailTheme.colors.surface, base = homeCapsuleBase())
                Column(
                    modifier = Modifier.fillMaxWidth().background(KrailTheme.colors.surface).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    THEMES.forEachIndexed { index, style ->
                        val themeColor = remember(style) { mutableStateOf(style.hexColorCode) }
                        CompositionLocalProvider(LocalThemeColor provides themeColor) {
                            Box(modifier = Modifier.testTag("$CAPSULE_TAG$index")) {
                                HomeSearchCapsule(onClick = {}, isAiAvailable = true, onAiEvent = {})
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        return requireNotNull(read)
    }

    /**
     * The pixels of capsule number [index], as `[row][channel]` triples sampled on the middle column: edge, inside, inside, edge.
     * Read through the Intro tests' Roborazzi capture, because the compose test's own `captureToImage` waits for a frame that a
     * Robolectric run never delivers.
     */
    private fun sample(index: Int): List<IntArray> {
        val image = IntroPixels(composeRule).of("$CAPSULE_TAG$index")
        val x = image.width / 2
        return listOf(0, INSIDE_ROWS, image.height - 1 - INSIDE_ROWS, image.height - 1).map { y ->
            val argb = image.getRGB(x, y)
            intArrayOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
        }
    }

    private fun assertLook(mode: ThemeMode, expectedBase: Color) {
        val read = drawCapsules(mode)
        assertEquals(expectedBase, read.base, "$mode: the capsule's own fill colour")
        val expected = read.base.copy(alpha = HOME_CAPSULE_FILL_ALPHA).compositeOver(read.page)
        val want = intArrayOf(
            (expected.red * 255).toInt(),
            (expected.green * 255).toInt(),
            (expected.blue * 255).toInt(),
        )

        val perTheme = THEMES.indices.map { sample(it) }
        THEMES.forEachIndexed { index, style ->
            val rows = perTheme[index]
            val (topEdge, topInside) = rows
            val (bottomInside, bottomEdge) = rows.drop(2)
            assertClose("$mode $style: the top row has no stroke, it is the fill", topInside, topEdge, 1)
            assertClose("$mode $style: the bottom row has no stroke, it is the fill", bottomInside, bottomEdge, 1)
            assertClose("$mode $style: the fill over the page", want, topInside, FILL_TOLERANCE)
            assertClose("$mode $style: the fill over the page, lower", want, bottomInside, FILL_TOLERANCE)
            assertTrue(
                (topInside.max() - topInside.min()) <= NEUTRAL_SPREAD,
                "$mode $style: the fill is neutral, but reads ${topInside.toList()}",
            )
        }
        perTheme.drop(1).forEachIndexed { index, rows ->
            assertClose("$mode ${THEMES[index + 1]}: no theme colour reaches the fill", perTheme.first()[1], rows[1], 1)
        }
    }

    private fun assertClose(what: String, expected: IntArray, actual: IntArray, tolerance: Int) {
        val off = expected.indices.maxOf { abs(expected[it] - actual[it]) }
        assertTrue(off <= tolerance, "$what: expected ${expected.toList()} but read ${actual.toList()}.")
    }

    @Test
    fun `in light mode the capsule is white, with no border and no theme colour`() {
        assertLook(ThemeMode.LIGHT, md_theme_light_surface)
    }

    @Test
    fun `in dark mode the capsule is the raised dark surface taj's sheets use, with no border and no theme colour`() {
        assertLook(ThemeMode.DARK, md_theme_dark_modal_sheet_background)
    }

    private companion object {
        const val CAPSULE_TAG = "test.capsule."
        const val INSIDE_ROWS = 4

        // The shadow is drawn behind the whole shape, so 8 percent of it shows through the 92 percent fill: about 3 levels of 255 in light mode.
        const val FILL_TOLERANCE = 5
        const val NEUTRAL_SPREAD = 2
        val THEMES = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)
    }
}
