package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
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
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay
import kotlin.test.assertEquals

/**
 * The weight of the text on the Intro's cards, read for real from the text layouts of the drawn screens. In the Intro the lines that
 * are Regular elsewhere are Medium, 500: the car park name, `spots available` and `full`, `Last updated` (still the soft label colour),
 * and the stop names of the trip card. The stop rows are Medium already. The figures stay Bold and no colour or size changes. The
 * same two cards drawn outside the Intro, as Saved Trips and home draw them, keep the weights they had.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroTextWeightTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroState>(PreviewCardContentState)
    private var softLabel = Color.Unspecified
    private var ink = Color.Unspecified

    private fun startIntro(mode: ThemeMode) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        softLabel = KrailTheme.colors.secondaryLabel
                        ink = KrailTheme.colors.onSurface
                        IntroFlowScreen(state = shown.value, suggestedStops = DEFAULT_SUGGESTED_STOPS, onEvent = { _, _ -> })
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun layouts(): List<TextLayoutResult> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().mapNotNull { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.singleOrNull()
            }

    private fun weightOf(text: String, startsWith: Boolean = false): FontWeight? =
        layouts().firstOrNull { if (startsWith) it.layoutInput.text.text.startsWith(text) else it.layoutInput.text.text == text }
            ?.layoutInput?.style?.fontWeight

    private fun colourOf(text: String): Color =
        layouts().first { it.layoutInput.text.text.startsWith(text) }.layoutInput.style.color

    @Test
    fun `on the Intro card the name, the words by the figures and last updated are Medium and the figures stay Bold`() {
        startIntro(ThemeMode.LIGHT)

        assertEquals(FontWeight.Medium, weightOf("Bella Vista"), "the car park name")
        assertEquals(FontWeight.Medium, weightOf("spots available"))
        assertEquals(FontWeight.Medium, weightOf("full"))
        assertEquals(FontWeight.Medium, weightOf("Last updated at", startsWith = true))
        assertEquals(FontWeight.Bold, weightOf("120"), "the figure of spots")
        assertEquals(FontWeight.Bold, weightOf("40%"), "the figure of the share full")
    }

    @Test
    fun `last updated keeps the soft label colour at the new weight and the other lines keep the ink, in dark mode too`() {
        startIntro(ThemeMode.DARK)

        assertEquals(softLabel, colourOf("Last updated at"))
        assertEquals(FontWeight.Medium, weightOf("Last updated at", startsWith = true))
        assertEquals(ink, colourOf("spots available"))
        assertEquals(ink, colourOf("Bella Vista"))
    }

    @Test
    fun `on the Intro trip card the stop names are Medium`() {
        startIntro(ThemeMode.LIGHT)

        shown.value = PreviewConfirm
        composeRule.waitForIdle()

        assertEquals(FontWeight.Medium, weightOf("Tallawong Station"))
        assertEquals(FontWeight.Medium, weightOf("Central Station"))
    }

    @Test
    fun `the stop rows are Medium, as they already were`() {
        startIntro(ThemeMode.LIGHT)

        shown.value = PreviewDestinationSelected
        composeRule.waitForIdle()

        assertEquals(FontWeight.Medium, weightOf("Wynyard Station"))
    }

    @Test
    fun `the same two cards outside the Intro keep their weights, Regular for the lines and Bold for the figures`() {
        val card = (PreviewCardContentState.parkRideCard as ParkRideCardPhase.Card).station.toParkRideUiState()
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        Column(modifier = Modifier) {
                            ParkRideCard(parkRideUiState = card, isExpanded = true, onClick = null, keepReadingSpace = true)
                            SavedTripCard(
                                fromDisplay = StopDisplay(stopId = "1", name = "Tallawong Station"),
                                toDisplay = StopDisplay(stopId = "2", name = "Central Station"),
                                onCardClick = {},
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(FontWeight.Normal, weightOf("Bella Vista"))
        assertEquals(FontWeight.Normal, weightOf("spots available"))
        assertEquals(FontWeight.Normal, weightOf("full"))
        assertEquals(FontWeight.Normal, weightOf("Last updated at", startsWith = true))
        assertEquals(FontWeight.Bold, weightOf("120"))
        assertEquals(FontWeight.Normal, weightOf("Tallawong Station"))
        assertEquals(FontWeight.Normal, weightOf("Central Station"))
    }
}
