package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.collections.immutable.persistentListOf
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
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colours of the home label pills, which sit on the theme coloured sheet above the search bar, so every ratio is
 * against the sheet's colour (the rider's theme colour, the same in light and dark mode) and not against the page.
 *
 * A set label is the in-app near black pill: its text and icon (`onStopLabelSurface`) are 16.04 to 1 on its fill
 * (`stopLabelSurface`), and its edge against the sheet is the fill, 4.88 to 1 on Metro up to 7.25 on Bus. The 1 dp
 * `outlineSubtle` hairline is drawn inside the fill's edge and is lighter than the fill, so in dark mode it reads
 * 1.56 to 2.32 to 1 on the sheet, softer than the fill beside it, and the fill is what carries the edge there.
 *
 * An unset label is taj's outlined button. In taj's default colours (the page's text colour) it is fine in light mode
 * (5.92 to 8.80, near black) and fails in dark mode, where that colour is cream: 2.30 on Train, 3.29 on Metro, 2.21
 * on Bus and 2.55 on Ferry. So the pill passes [homeUnsetPillColors], taj's own `colors` parameter with
 * `getForegroundColor` of the sheet, near black on all four, and its text and outline are the same colour, 5.92 to
 * 8.80 to 1, in both modes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class HomeLabelPillColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Read(
        val sheet: Color,
        val setFill: Color,
        val setInk: Color,
        val hairline: Color,
        val unset: ButtonColors,
        val tajDefault: ButtonColors,
    )

    private val reads = mutableMapOf<KrailThemeStyle, Read>()

    private fun readAll(mode: ThemeMode) {
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                THEMES.forEach { style ->
                    val themeColor = remember(style) { mutableStateOf(style.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColor) {
                        reads[style] = Read(
                            sheet = homeSheetColor(),
                            setFill = KrailTheme.colors.stopLabelSurface,
                            setInk = KrailTheme.colors.onStopLabelSurface,
                            hairline = KrailTheme.colors.outlineSubtle,
                            unset = homeUnsetPillColors(),
                            tajDefault = ButtonDefaults.outlinedButtonColors(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertContrast(style: KrailThemeStyle, mode: ThemeMode) {
        val read = requireNotNull(reads[style])
        val expected = requireNotNull(EXPECTED[mode]?.get(style))

        val setText = read.setInk.contrastRatio(read.setFill)
        assertEquals(expected.setText, setText, TOLERANCE, "$style $mode: the set pill's text on its fill")
        assertTrue(setText >= MIN_TEXT_CONTRAST, "$style $mode: the set pill's text is $setText to 1 on its fill")

        // The hairline is translucent, and the ratio ignores alpha, so it is measured as what it draws: over the pill.
        val fillEdge = read.setFill.contrastRatio(read.sheet)
        val hairlineEdge = read.hairline.compositeOver(read.setFill).contrastRatio(read.sheet)
        assertEquals(expected.setFillEdge, fillEdge, TOLERANCE, "$style $mode: the set pill's fill on the sheet")
        assertEquals(expected.setHairlineEdge, hairlineEdge, TOLERANCE, "$style $mode: the hairline on the sheet")
        assertTrue(
            maxOf(fillEdge, hairlineEdge) >= MIN_EDGE_CONTRAST,
            "$style $mode: the set pill's edge is ${maxOf(fillEdge, hairlineEdge)} to 1 on the sheet",
        )

        assertEquals(Color.Transparent, read.unset.containerColor, "$style $mode: the outlined button has no fill")
        assertEquals(getForegroundColor(read.sheet), read.unset.contentColor, "$style $mode: the unset ink")
        val unsetText = read.unset.contentColor.contrastRatio(read.sheet)
        assertEquals(
            expected.unset,
            unsetText,
            TOLERANCE,
            "$style $mode: the unset pill's text and outline on the sheet",
        )
        assertTrue(
            unsetText >= MIN_TEXT_CONTRAST,
            "$style $mode: the unset pill's text is $unsetText to 1 on the sheet",
        )
        assertTrue(
            unsetText >= MIN_EDGE_CONTRAST,
            "$style $mode: the unset pill's outline is $unsetText to 1 on the sheet",
        )

        val tajDefault = read.tajDefault.contentColor.contrastRatio(read.sheet)
        assertEquals(expected.tajDefault, tajDefault, TOLERANCE, "$style $mode: taj's default outline on the sheet")
        assertTrue(
            unsetText >= tajDefault,
            "$style $mode: the unset pill is never worse than taj's default ($tajDefault)",
        )
    }

    @Test
    fun `in light mode both pills clear their floors on the sheet in every theme`() {
        readAll(ThemeMode.LIGHT)
        THEMES.forEach { assertContrast(it, ThemeMode.LIGHT) }
    }

    @Test
    fun `in dark mode both pills clear their floors on the sheet in every theme`() {
        readAll(ThemeMode.DARK)
        THEMES.forEach { assertContrast(it, ThemeMode.DARK) }
    }

    /**
     * The numbers above are of the colours the pill is given. This reads what the real pill draws, in dark mode on a
     * Train sheet, the pairing where taj's default cream is 2.30 to 1: the pill's text is the near black ink of
     * [homeUnsetPillColors], which `OutlinedButton` also draws its outline in.
     */
    @Test
    fun `the unset pill draws its text in the near black ink on the sheet in dark mode`() {
        var sheet = Color.Unspecified
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = ThemeMode.DARK, setThemeMode = {})) {
                val themeColor = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                CompositionLocalProvider(LocalThemeColor provides themeColor) {
                    sheet = homeSheetColor()
                    HomeSearchSheet {
                        HomeLabelPills(
                            pills = persistentListOf(HomeChip.Label(StopLabel(emoji = "H", label = "Home"))),
                            onLabelClick = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNode(hasText("+ Home"), useUnmergedTree = true).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val ink = layouts.first().layoutInput.style.color

        assertEquals(getForegroundColor(sheet), ink, "The pill's text is the ink picked for the sheet.")
        assertTrue(
            ink.contrastRatio(sheet) >= MIN_TEXT_CONTRAST,
            "The text is ${ink.contrastRatio(sheet)} to 1 on the sheet.",
        )
    }

    /** What each pair measures to, so a change to a taj token or to a theme colour shows up here, not on a phone. */
    private data class Expected(
        val unset: Float,
        val setText: Float,
        val setFillEdge: Float,
        val setHairlineEdge: Float,
        val tajDefault: Float,
    )

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val MIN_EDGE_CONTRAST = 3f
        const val TOLERANCE = 0.02f
        val THEMES = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)

        val EXPECTED = mapOf(
            ThemeMode.LIGHT to mapOf(
                KrailThemeStyle.Train to Expected(8.47f, 16.04f, 6.97f, 7.72f, 8.47f),
                KrailThemeStyle.Metro to Expected(5.92f, 16.04f, 4.88f, 5.40f, 5.92f),
                KrailThemeStyle.Bus to Expected(8.80f, 16.04f, 7.25f, 8.03f, 8.80f),
                KrailThemeStyle.Ferry to Expected(7.64f, 16.04f, 6.30f, 6.97f, 7.64f),
            ),
            ThemeMode.DARK to mapOf(
                KrailThemeStyle.Train to Expected(8.47f, 16.04f, 6.97f, 2.24f, 2.30f),
                KrailThemeStyle.Metro to Expected(5.92f, 16.04f, 4.88f, 1.56f, 3.29f),
                KrailThemeStyle.Bus to Expected(8.80f, 16.04f, 7.25f, 2.32f, 2.21f),
                KrailThemeStyle.Ferry to Expected(7.64f, 16.04f, 6.30f, 2.02f, 2.55f),
            ),
        )
    }
}
