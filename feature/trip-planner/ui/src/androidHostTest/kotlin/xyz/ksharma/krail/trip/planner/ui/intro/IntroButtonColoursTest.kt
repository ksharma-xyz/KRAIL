package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
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
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colours of the decision bar, read for real from the drawn buttons in light and in dark mode: the primary button of the Park and
 * Ride question and card, the destination and origin choose buttons and the confirm save button is the screen's deep fill with a
 * white label, and the negative button stays the outline in the page's own label colour. The colour screen's button is the
 * theme colour the rider picked, as it was, not the purple of its question.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroButtonColoursTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val originSelected = PreviewOriginNearby.copy(selectedStop = PreviewOriginNearby.nearbyStops.first())
    private val themeSelected = IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Train.id)
    private val shown = mutableStateOf(PreviewAskState)
    private var label = Color.Unspecified

    private fun start(mode: ThemeMode) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        label = KrailTheme.colors.label
                        IntroFlowScreen(
                            state = shown.value,
                            suggestedStops = DEFAULT_SUGGESTED_STOPS,
                            onEvent = { _, _ -> },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun show(state: IntroState) {
        shown.value = state
        composeRule.waitForIdle()
    }

    private fun labelOf(text: String): Color {
        val layouts = composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().mapNotNull { node ->
            // A text with no layout yet, such as one on a screen that is still leaving, has no result and is not a button's label.
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.singleOrNull()
        }
        return layouts.single { it.layoutInput.text.text == text }.layoutInput.style.color
    }

    private fun assertDeepFill(state: IntroState, step: IntroStep, primaryLabel: String) {
        show(state)

        // The label is white only when the button was given the deep fill with it, as one call of the one colours parameter.
        assertEquals(Color.White, labelOf(primaryLabel), "$step: the label of the primary button is white")
    }

    private fun assertAll() {
        assertDeepFill(PreviewAskState, IntroStep.PARK_RIDE, PARK_RIDE_YES)
        assertEquals(label, labelOf(PARK_RIDE_NO), "the outlined button keeps the page's label colour")
        assertDeepFill(PreviewCardContentState, IntroStep.PARK_RIDE, PARK_RIDE_ADD)
        assertEquals(label, labelOf(PARK_RIDE_NOT_NOW))
        assertDeepFill(PreviewNoStationState, IntroStep.PARK_RIDE, PARK_RIDE_NO_STATION_OK)
        assertDeepFill(PreviewDestinationSelected, IntroStep.DESTINATION, STOP_CHOOSE)
        assertDeepFill(originSelected, IntroStep.ORIGIN, STOP_CHOOSE)
        assertDeepFill(PreviewConfirm, IntroStep.CONFIRM, CONFIRM_SAVE)
        assertEquals(label, labelOf(CONFIRM_NOT_NOW))
    }

    @Test
    fun `in light mode the primary button of each screen is its deep fill with a white label`() {
        start(ThemeMode.LIGHT)

        assertAll()
    }

    @Test
    fun `in dark mode the primary button of each screen is the same deep fill with a white label`() {
        start(ThemeMode.DARK)

        assertAll()
    }

    @Test
    fun `the colour screen's button keeps the theme the rider picked, with taj's own label, and is not the purple of its question`() {
        start(ThemeMode.LIGHT)

        show(themeSelected)

        val pick = KrailThemeStyle.Train.hexColorCode.hexToComposeColor()
        assertEquals(
            getForegroundColor(pick),
            labelOf(THEME_CONFIRM),
            "the label is taj's, which is not forced to white",
        )
        assertTrue(labelOf(THEME_CONFIRM) != Color.White)
    }

    @Test
    fun `the primary colours are the deep fill and a white label, or taj's own for the colour screen`() {
        var deep: ButtonColors? = null
        var own: ButtonColors? = null
        var plain: ButtonColors? = null
        composeRule.setContent {
            val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
            CompositionLocalProvider(LocalThemeColor provides themeColour) {
                deep = introPrimaryButtonColors(introButtonFill(introAccentTheme(IntroStep.PARK_RIDE)))
                own = introPrimaryButtonColors(introPrimaryFill(IntroStep.THEME))
                plain = ButtonDefaults.buttonColors()
            }
        }
        composeRule.waitForIdle()

        assertEquals(introButtonFill(introAccentTheme(IntroStep.PARK_RIDE)), deep?.containerColor)
        assertEquals(Color.White, deep?.contentColor)
        assertEquals(plain?.containerColor, own?.containerColor)
        assertEquals(plain?.contentColor, own?.contentColor)
    }
}
