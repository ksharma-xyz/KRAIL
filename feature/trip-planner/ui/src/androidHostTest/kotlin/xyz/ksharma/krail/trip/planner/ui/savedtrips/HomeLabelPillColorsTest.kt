package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colours of the home label pills. A set label is the in-app near black pill: its text and icon
 * (`onStopLabelSurface`) clear 4.5 to 1 on its fill (`stopLabelSurface`), and its edge reads against the page, by the
 * fill in light mode and by the 1 dp `outlineSubtle` hairline in dark mode, where the fill is the same near black as
 * the page. An unset label is taj's outlined button in taj's default colours: its text and its edge clear 4.5 to 1
 * on the page. Each in light and dark mode, in each of the four transport themes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeLabelPillColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Read(
        val page: Color,
        val setFill: Color,
        val setInk: Color,
        val hairline: Color,
        val outlined: ButtonColors,
    )

    private val reads = mutableMapOf<KrailThemeStyle, Read>()

    private fun readAll(mode: ThemeMode) {
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                THEMES.forEach { style ->
                    val themeColor = remember(style) { mutableStateOf(style.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColor) {
                        reads[style] = Read(
                            page = KrailTheme.colors.surface,
                            setFill = KrailTheme.colors.stopLabelSurface,
                            setInk = KrailTheme.colors.onStopLabelSurface,
                            hairline = KrailTheme.colors.outlineSubtle,
                            outlined = ButtonDefaults.outlinedButtonColors(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertContrast(style: KrailThemeStyle, mode: ThemeMode) {
        val read = requireNotNull(reads[style])

        val setText = read.setInk.contrastRatio(read.setFill)
        assertTrue(setText >= MIN_TEXT_CONTRAST, "$style $mode: the set pill's text is $setText to 1 on its fill")

        // The hairline is translucent, and the ratio ignores alpha, so it is measured as what it draws: over the pill.
        val hairline = read.hairline.compositeOver(read.setFill)
        val edge = maxOf(read.setFill.contrastRatio(read.page), hairline.contrastRatio(read.page))
        assertTrue(edge >= MIN_EDGE_CONTRAST, "$style $mode: the set pill's edge is $edge to 1 on the page")

        assertEquals(Color.Transparent, read.outlined.containerColor, "$style $mode: the outlined button has no fill")
        val unsetText = read.outlined.contentColor.contrastRatio(read.page)
        assertTrue(unsetText >= MIN_TEXT_CONTRAST, "$style $mode: the unset pill's text and edge are $unsetText to 1")
    }

    @Test
    fun `in light mode both pills clear their floors on the page in every theme`() {
        readAll(ThemeMode.LIGHT)
        THEMES.forEach { assertContrast(it, ThemeMode.LIGHT) }
    }

    @Test
    fun `in dark mode both pills clear their floors on the page in every theme`() {
        readAll(ThemeMode.DARK)
        THEMES.forEach { assertContrast(it, ThemeMode.DARK) }
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val MIN_EDGE_CONTRAST = 3f
        val THEMES = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)
    }
}
