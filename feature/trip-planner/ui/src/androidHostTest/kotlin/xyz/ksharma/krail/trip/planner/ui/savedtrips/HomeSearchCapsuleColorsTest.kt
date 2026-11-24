package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_modal_sheet_background
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface

/**
 * The capsule's colours, which are the same in every theme colour and differ only by mode: white in light mode and taj's raised dark
 * surface (`bottomSheetBackground`) in dark mode, at 92 percent, with the page's full text colour for the question, the glyph, the
 * microphone and the caret, never dimmed. The question must read at 4.5 to 1 or more on the fill, and the fill is translucent, so it
 * is measured over what can be behind it: the page, the page's own text (a line of a card scrolling under the capsule) and the rider's
 * theme colour (a card tinted with it).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeSearchCapsuleColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Read(val colors: TextFieldColors, val page: Color, val text: Color, val theme: Color)

    private val reads = mutableMapOf<Pair<ThemeMode, KrailThemeStyle>, Read>()

    /** Both modes and every theme colour in one composition, since a rule sets its content once. */
    private fun readAll() {
        composeRule.setContent {
            MODES.forEach { mode ->
                KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                    KrailThemeStyle.entries.forEach { style ->
                        val themeColor = remember(style) { mutableStateOf(style.hexColorCode) }
                        CompositionLocalProvider(LocalThemeColor provides themeColor) {
                            reads[mode to style] = Read(
                                colors = homeCapsuleColors(),
                                page = KrailTheme.colors.surface,
                                text = KrailTheme.colors.onSurface,
                                theme = style.hexColorCode.hexToComposeColor(),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun read(mode: ThemeMode, style: KrailThemeStyle): Read = requireNotNull(reads[mode to style])

    private fun assertQuestionReads(mode: ThemeMode) {
        KrailThemeStyle.entries.forEach { style ->
            val read = read(mode, style)
            val fill = read.colors.containerColor
            listOf("the page" to read.page, "the page's own text" to read.text, "the theme colour" to read.theme).forEach { (name, behind) ->
                val seen = fill.compositeOver(behind)
                val question = read.colors.placeholderColor.contrastRatio(seen)
                val glyph = read.colors.contentColor.contrastRatio(seen)
                assertTrue(question >= MIN_TEXT_CONTRAST, "$mode $style: the question is $question to 1 on the capsule over $name")
                assertTrue(glyph >= MIN_TEXT_CONTRAST, "$mode $style: the glyph and microphone are $glyph to 1 on the capsule over $name")
            }
        }
    }

    private fun assertColours(mode: ThemeMode, base: Color, text: Color) {
        KrailThemeStyle.entries.forEach { style ->
            val colors = read(mode, style).colors
            assertEquals(base.copy(alpha = HOME_CAPSULE_FILL_ALPHA), colors.containerColor, "$mode $style fill")
            assertEquals(text, colors.contentColor, "$mode $style content")
            assertEquals(colors.contentColor, colors.placeholderColor, "$mode $style: the question is the full text colour, not dimmed")
            assertEquals(1f, colors.placeholderColor.alpha, "$mode $style: no opacity on the question")
            assertEquals(colors.contentColor, colors.cursorColor, "$mode $style caret")
        }
        assertQuestionReads(mode)
    }

    @Test
    fun `in light mode the capsule is white at 92 percent with the page's full text colour, in every theme`() {
        readAll()

        assertColours(ThemeMode.LIGHT, md_theme_light_surface, md_theme_light_onSurface)
    }

    @Test
    fun `in dark mode the capsule is taj's raised dark surface at 92 percent with the page's full text colour, in every theme`() {
        readAll()

        assertColours(ThemeMode.DARK, md_theme_dark_modal_sheet_background, md_theme_dark_onSurface)
    }

    @Test
    fun `the fill is the same in every theme colour, so none of them reaches it`() {
        readAll()

        MODES.forEach { mode ->
            val fills = KrailThemeStyle.entries.map { read(mode, it).colors.containerColor }.distinct()
            assertEquals(1, fills.size, "$mode: one fill across the themes, found $fills")
        }
    }

    @Test
    fun `in dark mode the capsule is one step lighter than the page, so the fill itself stands off it`() {
        readAll()

        val read = read(ThemeMode.DARK, KrailThemeStyle.Train)
        assertTrue(
            read.colors.containerColor.compositeOver(read.page).luminance() > md_theme_dark_surface.luminance(),
            "The capsule's fill is lighter than the page.",
        )
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        val MODES = listOf(ThemeMode.LIGHT, ThemeMode.DARK)
    }
}
