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
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.themeBackgroundColor
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The two looks of a label pill on the page, in every theme colour and in both modes: a label with a stop wears the saved trip
 * card's own fill and ink ([homeSetPillColors]), and a label with no stop is the outline version of it, no fill, its text and its
 * outline in the same ink ([homeUnsetPillColors]).
 *
 * The card's fill is `themeBackgroundColor()` and its text is `KrailTheme.colors.onSurface` (`SavedTripCard` draws the first as its
 * background and takes the second as the default of its `Text`), so those are read here the same way and the pill must be equal to
 * them (the fill laid over the page, since taj's `Button` draws its container at full alpha), not to a copy of their numbers. A
 * solid theme colour fill, which taj's automatic ink turns black in both modes, is not the card's fill and fails. The ink on the
 * fill is 4.5 to 1 or more, and an unset pill's ink is 4.5 to 1 or more on the page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeLabelRowColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Read(
        val set: ButtonColors,
        val unset: ButtonColors,
        val page: Color,
        val theme: Color,
        val cardFill: Color,
        val cardInk: Color,
    )

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
                                set = homeSetPillColors(),
                                unset = homeUnsetPillColors(),
                                page = KrailTheme.colors.surface,
                                theme = style.hexColorCode.hexToComposeColor(),
                                cardFill = themeBackgroundColor(),
                                cardInk = KrailTheme.colors.onSurface,
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
            val where = "$mode $style"

            // What the rider sees on a card: its translucent fill over the page. The pill's fill is that colour, laid down once.
            val seen = read.cardFill.compositeOver(read.page)
            assertEquals(
                seen,
                read.set.containerColor,
                "$where: a set pill has the saved trip card's fill, as seen over the page",
            )
            assertEquals(read.cardInk, read.set.contentColor, "$where: a set pill has the saved trip card's ink")
            assertNotEquals(read.theme, read.set.containerColor, "$where: the set fill is not the solid theme colour")
            val setInk = read.set.contentColor.contrastRatio(read.set.containerColor)
            assertTrue(
                setInk >= MIN_TEXT_CONTRAST,
                "$where: the set pill's ink is $setInk to 1 on its fill over the page",
            )

            assertEquals(Color.Transparent, read.unset.containerColor, "$where: an unset pill has no fill")
            assertEquals(
                read.cardInk,
                read.unset.contentColor,
                "$where: an unset pill's text and outline are the card's ink",
            )
            val unsetInk = read.unset.contentColor.contrastRatio(read.page)
            assertTrue(unsetInk >= MIN_TEXT_CONTRAST, "$where: the unset pill's ink is $unsetInk to 1 on the page")
        }
    }

    @Test
    fun `in light mode a set pill is the card's tint with the card's ink and an unset pill is its outline, in every theme`() {
        readAll()

        assertLooks(ThemeMode.LIGHT)
    }

    @Test
    fun `in dark mode a set pill is the card's tint with the card's ink and an unset pill is its outline, in every theme`() {
        readAll()

        assertLooks(ThemeMode.DARK)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        val MODES = listOf(ThemeMode.LIGHT, ThemeMode.DARK)
    }
}
