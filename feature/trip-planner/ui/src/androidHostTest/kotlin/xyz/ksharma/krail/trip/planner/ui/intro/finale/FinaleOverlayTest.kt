package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.test.assertEquals

/**
 * The finale layer drawn from a frame: what a screen reader hears and what a tap does. What it paints is a plan of
 * plain numbers (`backdropPlan`), held by `FinaleBackdropTest` in the common tests, because a screenshot of a layer
 * is not something a host test can take.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class FinaleOverlayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val theme = KrailThemeStyle.Metro.hexColorCode.hexToComposeColor()
    private val shown = mutableStateOf(finaleFrameAt(0, reduceMotion = false))
    private var contentSet = false
    private var taps = 0

    private fun show(elapsedMs: Long) {
        shown.value = finaleFrameAt(elapsedMs, reduceMotion = false)
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
                    FinaleOverlay(frame = shown, themeColour = theme, groundColour = theme, onTap = { taps++ })
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `a screen reader hears the one phrase and not the letters`() {
        show(elapsedMs = 1000)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).assertContentDescriptionEquals("Let's KRAIL")
        listOf("K", "R", "A", "I", "#").forEach { letter ->
            composeRule.onAllNodesWithText(letter).assertCountEquals(0)
        }
    }

    @Test
    fun `a tap anywhere on the layer is one skip each time`() {
        show(elapsedMs = 1000)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).performTouchInput { click(center) }
        assertEquals(1, taps)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).performTouchInput { click(topLeft) }
        assertEquals(2, taps)
    }
}
