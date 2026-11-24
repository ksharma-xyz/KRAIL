package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The two looks of a label pill on the page, in every theme colour and in both modes, as taj ships them: a label with a stop is a
 * filled [ButtonDefaults.buttonColors] in the rider's theme colour, a label with no stop is [ButtonDefaults.outlinedButtonColors],
 * no fill, with its text and its outline in the page's text colour. The row passes no colours of its own, so these are taj's.
 * The label on a set pill is 4.5 to 1 or more on its fill, and the unset pill's text and outline are 4.5 to 1 or more on the page
 * (an outline only needs 3, and it is the same colour as the text).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeLabelRowColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Read(val set: ButtonColors, val unset: ButtonColors, val page: Color, val theme: Color)

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
                                set = ButtonDefaults.buttonColors(),
                                unset = ButtonDefaults.outlinedButtonColors(),
                                page = KrailTheme.colors.surface,
                                theme = style.hexColorCode.hexToComposeColor(),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertLooks(mode: ThemeMode) {
        KrailThemeStyle.entries.forEach { style ->
            val read = requireNotNull(reads[mode to style])

            assertEquals(read.theme, read.set.containerColor, "$mode $style: a set label is filled in the theme colour")
            val setText = read.set.contentColor.contrastRatio(read.set.containerColor)
            assertTrue(setText >= MIN_TEXT_CONTRAST, "$mode $style: the set label's text is $setText to 1 on its fill")

            assertEquals(Color.Transparent, read.unset.containerColor, "$mode $style: an unset label has no fill")
            val unsetText = read.unset.contentColor.contrastRatio(read.page)
            assertTrue(
                unsetText >= MIN_TEXT_CONTRAST,
                "$mode $style: the unset label's text and outline are $unsetText to 1 on the page",
            )
        }
    }

    @Test
    fun `in light mode a set pill is the theme colour and an unset pill is an outline, both readable in every theme`() {
        readAll()

        assertLooks(ThemeMode.LIGHT)
    }

    @Test
    fun `in dark mode a set pill is the theme colour and an unset pill is an outline, both readable in every theme`() {
        readAll()

        assertLooks(ThemeMode.DARK)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        val MODES = listOf(ThemeMode.LIGHT, ThemeMode.DARK)
    }
}
