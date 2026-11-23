package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
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
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The decision bar is one layer over every screen, so its primary button is the one accent colour that is not part of a sliding screen.
 * It used to jump from green to blue to pink in the frame the step changed. It now moves from one screen's fill to the next over
 * [BAR_FILL_CROSSFADE_MS], read here from the drawn pixels of the real button on a clock that is stepped by hand, and with reduced
 * motion it jumps. The first screen starts at its own fill, and the colour screen, whose button is the picked theme's, is not animated.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroBarFillCrossfadeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroState>(PreviewAskState)

    private val green = introButtonFill(introAccentTheme(IntroStep.PARK_RIDE))
    private val blue = introButtonFill(introAccentTheme(IntroStep.DESTINATION))

    private fun start(reduceMotion: Boolean) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        // The clock is stepped by hand, so the colour can be read part way through.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        IntroFlowScreen(state = shown.value, suggestedStops = DEFAULT_SUGGESTED_STOPS, onEvent = { _, _ -> })
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 2)
    }

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    // A pixel inside the left end of the filled button, where its label is not.
    private fun buttonFill(): Color {
        val bounds = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val inset = with(composeRule.density) { 14.dp.toPx() }
        return IntroPixels(composeRule).of(TripPlannerTestTags.INTRO_SCREEN).colourAt(bounds.left + inset, bounds.center.y)
    }

    private fun close(a: Color, b: Color, tolerance: Float = 0.015f) =
        abs(a.red - b.red) <= tolerance && abs(a.green - b.green) <= tolerance && abs(a.blue - b.blue) <= tolerance

    private fun distance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

    @Test
    fun `the first screen's button starts at its own fill, with no fade from nothing`() {
        start(reduceMotion = false)

        assertTrue(close(green, buttonFill()), "the first frame is $green, drew ${buttonFill()}")
    }

    @Test
    fun `the fill moves from green to blue over 300 ms, between the two at 150 ms and the new one at the end`() {
        start(reduceMotion = false)

        shown.value = PreviewDestinationSelected
        composeRule.mainClock.advanceTimeBy(BAR_FILL_CROSSFADE_MS / 2L)
        val middle = buttonFill()

        assertTrue(!close(green, middle), "still green half way: $middle")
        assertTrue(!close(blue, middle), "already blue half way: $middle")
        assertTrue(
            distance(green, middle) < distance(green, blue) && distance(middle, blue) < distance(green, blue),
            "half way is not between the two: $green $middle $blue",
        )
        composeRule.mainClock.advanceTimeBy(BAR_FILL_CROSSFADE_MS.toLong() + FRAME_MS * 2)
        assertTrue(close(blue, buttonFill()), "the end is $blue, drew ${buttonFill()}")
    }

    @Test
    fun `with reduced motion the fill jumps to the new colour in the frame the step changes`() {
        start(reduceMotion = true)
        assertTrue(close(green, buttonFill()), "before: ${buttonFill()}")

        shown.value = PreviewDestinationSelected
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 2)

        assertTrue(close(blue, buttonFill()), "after two frames it is ${buttonFill()}, not $blue")
    }

    @Test
    fun `the spec is 300 ms with the Intro's easing, and a jump with reduced motion`() {
        assertEquals(300, BAR_FILL_CROSSFADE_MS)
        assertTrue(introBarFillSpec(reduceMotion = false) != introBarFillSpec(reduceMotion = true))
    }

    @Test
    fun `the colour screen keeps the taj button, so getting there from the pink screen does not fail and the button is there`() {
        start(reduceMotion = false)

        shown.value = PreviewConfirm
        composeRule.mainClock.advanceTimeBy(BAR_FILL_CROSSFADE_MS.toLong() + FRAME_MS * 2)
        shown.value = IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Bus.id)
        composeRule.mainClock.advanceTimeBy(BAR_FILL_CROSSFADE_MS.toLong() + FRAME_MS * 2)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION, useUnmergedTree = true).assertExists()
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
