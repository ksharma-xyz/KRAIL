package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
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
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The colour the primary button takes from the theme, read for real: it is `ButtonDefaults.buttonColors().containerColor`,
 * the colour the taj button fills with, read from inside [IntroThemeBlend] while the app's theme colour changes under it.
 * After each pick it settles on exactly the theme's own colour, on the way it is a blend on the one clock of the theme change
 * sequence ([THEME_COLOUR_MS]) and never a jump, from any colour part way through, and with reduced motion it is a plain
 * [THEME_CHANGE_REDUCED_MS] cross-fade.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroThemeBlendTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val theme = mutableStateOf(KrailThemeStyle.Train.hexColorCode)
    private val seen = mutableListOf<Color>()
    private var contentSet = false

    private fun show() {
        if (contentSet) return
        contentSet = true
        // The clock is stepped by hand, so a blend can be read part way through.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalThemeColor provides theme) {
                    IntroThemeBlend { Probe() }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Composable
    private fun Probe() {
        val container = ButtonDefaults.buttonColors().containerColor
        SideEffect { seen += container }
    }

    private fun pick(style: KrailThemeStyle) {
        theme.value = style.hexColorCode
        // The write is on the test thread, so it is handed to the composition by hand, as the app's own write is.
        Snapshot.sendApplyNotifications()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(THEME_CHANGE_MS.toLong() + SETTLE_EXTRA_MS)
    }

    private fun KrailThemeStyle.colour() = hexColorCode.hexToComposeColor()

    private fun distance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    @Test
    fun `after each pick the button's colour is exactly the picked theme's, switching between all four in turn`() {
        show()
        settle()
        assertEquals(KrailThemeStyle.Train.colour(), seen.last(), "before any pick")

        listOf(
            KrailThemeStyle.Metro,
            KrailThemeStyle.Bus,
            KrailThemeStyle.Ferry,
            KrailThemeStyle.Train,
            KrailThemeStyle.Bus,
        ).forEach { style ->
            pick(style)
            settle()

            assertEquals(style.colour(), seen.last(), "after picking ${style.name}")
        }
    }

    @Test
    fun `a pick blends over 600 ms, with colours between the two and none off the line`() {
        show()
        settle()
        val from = KrailThemeStyle.Train.colour()
        val to = KrailThemeStyle.Metro.colour()
        seen.clear()

        pick(KrailThemeStyle.Metro)
        composeRule.mainClock.advanceTimeBy(THEME_COLOUR_MS.toLong() / 2L)

        val middle = seen.last()
        assertNotEquals(from, middle, "still the old colour half way through")
        assertNotEquals(to, middle, "already the new colour half way through")
        assertTrue(
            distance(from, middle) < distance(from, to) && distance(middle, to) < distance(from, to),
            "half way is not between the two: $from $middle $to",
        )
        settle()
        assertEquals(to, seen.last())
    }

    @Test
    fun `no frame jumps further than a share of the distance, so the change is never a flash`() {
        show()
        settle()
        seen.clear()

        pick(KrailThemeStyle.Ferry)
        repeat(FRAMES) { composeRule.mainClock.advanceTimeBy(FRAME_MS) }
        settle()

        val whole = distance(KrailThemeStyle.Train.colour(), KrailThemeStyle.Ferry.colour())
        val steps = seen.zipWithNext { a, b -> distance(a, b) }
        assertTrue(steps.size > 4, "only ${steps.size} steps, so it was not a blend")
        assertTrue(steps.max() < whole * MAX_STEP_SHARE, "one frame moved ${steps.max()} of $whole")
    }

    @Test
    fun `a pick during a blend carries on from where the colour is, with no jump back to the old one`() {
        show()
        settle()
        seen.clear()
        pick(KrailThemeStyle.Metro)
        composeRule.mainClock.advanceTimeBy(THEME_COLOUR_MS.toLong() / 2L)
        val partway = seen.last()

        pick(KrailThemeStyle.Bus)
        composeRule.mainClock.advanceTimeBy(FRAME_MS)

        val whole = distance(KrailThemeStyle.Train.colour(), KrailThemeStyle.Bus.colour())
        assertTrue(distance(partway, seen.last()) < whole * MAX_STEP_SHARE, "jumped from $partway to ${seen.last()}")
        settle()
        assertEquals(KrailThemeStyle.Bus.colour(), seen.last())
    }

    @Test
    fun `with reduced motion the colour is a plain 200 ms cross-fade, straight from the old to the new and not a snap`() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        show()
        settle()
        seen.clear()
        val from = KrailThemeStyle.Train.colour()
        val to = KrailThemeStyle.Ferry.colour()

        pick(KrailThemeStyle.Ferry)
        composeRule.mainClock.advanceTimeBy(THEME_CHANGE_REDUCED_MS.toLong() / 2L)
        val middle = seen.last()

        assertNotEquals(from, middle, "still the old colour half way through")
        assertNotEquals(to, middle, "already the new colour half way through")
        assertTrue(distance(from, middle) < distance(from, to) && distance(middle, to) < distance(from, to), "$from $middle $to")
        composeRule.mainClock.advanceTimeBy(THEME_CHANGE_REDUCED_MS.toLong())
        assertEquals(to, seen.last())
    }

    @Test
    fun `the colour window is 600 ms and the cross-fade 200 ms, and the sequence that holds them is 700 ms`() {
        assertEquals(600f, THEME_COLOUR_MS)
        assertEquals(200f, THEME_CHANGE_REDUCED_MS)
        assertEquals(700f, THEME_CHANGE_MS)
        assertTrue(THEME_COLOUR_MS <= THEME_CHANGE_MS)
    }

    private companion object {
        const val SETTLE_EXTRA_MS = 120L
        const val FRAME_MS = 16L
        const val FRAMES = 24
        const val MAX_STEP_SHARE = 0.35f
    }
}
