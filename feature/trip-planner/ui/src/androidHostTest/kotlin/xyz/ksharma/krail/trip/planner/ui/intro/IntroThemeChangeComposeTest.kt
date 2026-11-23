package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The colour screen's change of theme, played for real under [IntroThemeBlend] with the clock stepped by hand: a tap on a badge,
 * a second tap in the middle of the first, the settled state, and reduced motion. Taps are real clicks on the badges, so the badge
 * that was tapped, and where it is, reach the sequence as they do on a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class IntroThemeChangeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val selected = mutableStateOf<Int?>(null)
    private val app = mutableStateOf(KrailThemeStyle.Train.hexColorCode)
    private var change: IntroThemeChange? = null

    private fun show() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewTheme {
                    CompositionLocalProvider(LocalThemeColor provides app) {
                        IntroThemeBlend(selectedThemeId = selected.value) {
                            Box {
                                Capture()
                                IntroFlowScreen(
                                    state = IntroState(step = IntroStep.THEME, selectedThemeId = selected.value),
                                    suggestedStops = DEFAULT_SUGGESTED_STOPS,
                                    onEvent = { event, _ -> if (event is IntroUiEvent.PickTheme) selected.value = event.themeId },
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Composable
    private fun Capture() {
        val current = LocalIntroThemeChange.current
        SideEffect { change = current }
    }

    private fun tap(style: KrailThemeStyle) {
        composeRule.onNodeWithTag(introThemeChoices().first { it.style == style }.tag).performClick()
    }

    private fun step(ms: Long) = composeRule.mainClock.advanceTimeBy(ms)

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    private fun running(): IntroThemeChange = assertNotNull(change, "the Intro shell provides the sequence")

    @Test
    fun `the Intro shell provides the sequence, and it opens settled on the question with nothing playing`() {
        show()

        val sequence = running()
        assertEquals(false, sequence.isRunning)
        assertNull(sequence.spec.headline.outgoing)
        assertEquals(PICK_THEME_HEADLINE, sequence.spec.headline.incoming.lines)
        composeRule.onNodeWithText(PICK_THEME_HEADING).assertIsDisplayed()
    }

    @Test
    fun `a tap plays the sequence from the badge, and when it is over everything is settled on the new theme`() {
        show()
        tap(KrailThemeStyle.Metro)
        step(FRAME_MS)
        val sequence = running()

        assertTrue(sequence.isRunning, "nothing is playing after a tap")
        assertEquals(introThemeChoices().indexOfFirst { it.style == KrailThemeStyle.Metro }, sequence.spec.taps.tapped)
        assertEquals(false, sequence.buttonRevealed, "the button is last")
        step(THEME_CHANGE_MS.toLong() + SETTLE_MS)

        assertEquals(false, sequence.isRunning)
        assertNull(sequence.spec.headline.outgoing, "the old headline is gone")
        assertEquals(KrailThemeStyle.Metro.hexColorCode, sequence.spec.target.hex)
        assertEquals(true, sequence.buttonRevealed)
        composeRule.onNodeWithText(KrailThemeStyle.Metro.tagLine).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsDisplayed()
    }

    private fun distance(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color) =
        abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

    // How much of the headline can be seen, whichever of the two layers it is.
    private fun IntroThemeChange.visibleHeadline(): Float {
        val time = clock.value
        val leaving = spec.headline.outgoing?.let { outgoingLooks(spec.headline.outgoingLooks, time, spec.reduceMotion).visibility } ?: 0f
        val arriving = incomingLooks(spec.headline.incoming.lines.size, time, spec.reduceMotion).visibility
        return maxOf(leaving, arriving)
    }

    @Test
    fun `a second tap in the middle of the first carries on from where it was, with no jump in the colours, the emoji or the headline`() {
        show()
        tap(KrailThemeStyle.Metro)
        step(FRAME_MS)
        step(MID_MS)
        val sequence = running()
        val colourBefore = sequence.colourNow()
        val textBefore = sequence.textNow()
        val emojiBefore = sequence.emojiScaleNow()
        val headlineBefore = sequence.visibleHeadline()
        val firstPress = sequence.pressNow(introThemeChoices().indexOfFirst { it.style == KrailThemeStyle.Metro })
        assertTrue(sequence.isRunning, "the first sequence is still playing")

        tap(KrailThemeStyle.Bus)
        step(FRAME_MS)
        step(FRAME_MS)

        assertTrue(sequence.isRunning, "the second sequence is playing")
        assertEquals(introThemeChoices().indexOfFirst { it.style == KrailThemeStyle.Bus }, sequence.spec.taps.tapped)
        assertTrue(distance(colourBefore, sequence.colourNow()) < STEP_COLOUR, "the theme colour jumped")
        assertTrue(distance(textBefore, sequence.textNow()) < STEP_COLOUR, "the text colour jumped")
        assertTrue(abs(emojiBefore - sequence.emojiScaleNow()) < STEP_SCALE, "the emoji jumped from $emojiBefore to ${sequence.emojiScaleNow()}")
        assertTrue(abs(headlineBefore - sequence.visibleHeadline()) < STEP_HEADLINE, "the headline jumped")
        assertTrue(
            abs(firstPress - sequence.pressNow(introThemeChoices().indexOfFirst { it.style == KrailThemeStyle.Metro })) < STEP_SCALE,
            "the first badge's press jumped",
        )
        step(THEME_CHANGE_MS.toLong() + SETTLE_MS)
        assertEquals(KrailThemeStyle.Bus.hexColorCode, sequence.spec.target.hex)
        assertEquals(false, sequence.isRunning)
        composeRule.onNodeWithText(KrailThemeStyle.Bus.tagLine).assertIsDisplayed()
    }

    @Test
    fun `tapping the same badge again starts nothing`() {
        show()
        tap(KrailThemeStyle.Metro)
        step(THEME_CHANGE_MS.toLong() + SETTLE_MS)
        val sequence = running()
        val spec = sequence.spec

        tap(KrailThemeStyle.Metro)
        step(FRAME_MS * 2)

        assertEquals(false, sequence.isRunning)
        assertEquals(spec, sequence.spec)
    }

    @Test
    fun `with reduced motion a tap is a plain 200 ms cross-fade, with no wash and no pop, and settles on the new theme`() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        show()
        tap(KrailThemeStyle.Ferry)
        step(FRAME_MS)
        step(REDUCED_MID_MS)
        val sequence = running()

        assertTrue(sequence.spec.reduceMotion)
        assertTrue(sequence.isRunning, "a cross-fade is playing")
        val layers = mutableListOf<Float>()
        sequence.spec.forEachGroundLayer(sequence.clock.value) { _, _, share -> layers += share }
        assertEquals(listOf(1f), layers, "there is no growing circle")
        assertEquals(1f, sequence.emojiScaleNow(), "there is no pop")
        assertEquals(1f, sequence.pressNow(introThemeChoices().indexOfFirst { it.style == KrailThemeStyle.Ferry }), "there is no press")
        val old = sequence.outgoingAlphaNow(0)
        val new = sequence.incomingAlphaNow(0)
        assertTrue(old in 0.05f..0.95f && new in 0.05f..0.95f, "both headlines are part way: $old and $new")
        assertEquals(0f, sequence.incomingRiseNowDp(0), "no rise")

        step(THEME_CHANGE_REDUCED_MS.toLong() + SETTLE_MS)

        assertEquals(false, sequence.isRunning)
        assertNull(sequence.spec.headline.outgoing)
        composeRule.onNodeWithText(KrailThemeStyle.Ferry.tagLine).assertIsDisplayed()
    }

    private companion object {
        const val FRAME_MS = 16L
        const val MID_MS = 184L
        const val REDUCED_MID_MS = 84L
        const val SETTLE_MS = 100L
        const val STEP_COLOUR = 0.12f
        const val STEP_SCALE = 0.2f
        const val STEP_HEADLINE = 0.5f
    }
}
