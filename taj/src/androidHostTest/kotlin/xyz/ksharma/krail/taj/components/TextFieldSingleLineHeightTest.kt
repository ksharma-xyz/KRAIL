package xyz.ksharma.krail.taj.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * The single line height of taj's [TextField] is 48 dp for every caller that does not ask for anything else, at every text
 * size, and a minimum of the caller's own value when `minHeight` is passed (the home search capsule, whose height is one text
 * line plus 15 dp above and below). Nothing that already uses the field may change, so the default is pinned at 100 and at 200
 * percent text, and the opt in is pinned to grow with the text and to leave a short field at its floor.
 *
 * The field is composed in a plain Robolectric activity the way the snapshot tests do it, and its height is read from the
 * layout (`onSizeChanged`), so the module needs no Compose test manifest on its test classpath. One activity is built for each
 * measurement, because a font scale is fixed for the life of a composition here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class TextFieldSingleLineHeightTest {

    @Test
    fun `with no minimum a single line field is exactly 48 dp at 100 percent text`() {
        val height = heightOf(fontScale = 1f)

        assertNear("height at 100 percent", 48.dp, height)
    }

    @Test
    fun `with no minimum a single line field stays 48 dp at 200 percent text, however much padding it is given`() {
        val height = heightOf(fontScale = 2f, padding = TALL_PADDING)

        assertNear("height at 200 percent with 15 dp above and below", 48.dp, height)
    }

    @Test
    fun `with a minimum of 48 dp a field that fits is exactly 48 dp, so a caller that opts in looks the same at 100 percent`() {
        val height = heightOf(fontScale = 1f, minHeight = TextFieldDefaults.SingleLineHeight)

        assertNear("height at 100 percent", 48.dp, height)
    }

    @Test
    fun `with a minimum a field shorter than it is held at the minimum`() {
        // Half size text and 15 dp above and below is 12 + 30, under the 48 dp floor.
        val height = heightOf(fontScale = 0.5f, padding = TALL_PADDING, minHeight = TextFieldDefaults.SingleLineHeight)

        assertNear("height at 50 percent", 48.dp, height)
    }

    @Test
    fun `with a minimum a field grows with its text and keeps its padding constant`() {
        val heights = listOf(1f, 1.3f, 2f).map { scale ->
            val field =
                heightOf(fontScale = scale, padding = TALL_PADDING, minHeight = TextFieldDefaults.SingleLineHeight)
            // The same text with no padding and no floor is the line itself, so the field less the line is the padding.
            val line = heightOf(fontScale = scale, padding = NO_PADDING, minHeight = 0.dp)
            assertNear("padding above and below the line at $scale", 30.dp, field - line)
            field
        }

        assertTrue(heights[0] < heights[1] && heights[1] < heights[2], "The field grows with the text: $heights")
        assertTrue(heights.all { it > 48.dp }, "Each is taller than the 48 dp floor: $heights")
    }

    /** A field alone in a fresh activity at [fontScale], and the height the layout gave it, in dp. */
    private fun heightOf(fontScale: Float, padding: PaddingValues? = null, minHeight: Dp? = null): Dp {
        var heightPx = 0
        var density = 1f
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        controller.setup()
        val activity = controller.get()
        val view = ComposeView(activity).apply {
            setContent {
                val base = LocalDensity.current
                density = base.density
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                        Field(padding = padding, minHeight = minHeight, onHeight = { heightPx = it })
                    }
                }
            }
        }
        activity.setContentView(view)
        // Frames run on a fixed clock, as in the snapshot tests, so the layout has run once this returns.
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_DELAY_MS))
        shadowOf(Looper.getMainLooper()).idleFor(SETTLE_MS, TimeUnit.MILLISECONDS)
        controller.pause().stop().destroy()
        assertTrue(heightPx > 0, "The field was never laid out")
        return (heightPx / density).dp
    }

    @Composable
    private fun Field(padding: PaddingValues?, minHeight: Dp?, onHeight: (Int) -> Unit) {
        val state = remember { TextFieldState() }
        // The window's constraints are tight, which would stretch any height the field asks for. The Box loosens them.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            TextField(
                state = state,
                placeholder = PLACEHOLDER,
                readOnly = true,
                modifier = Modifier.onSizeChanged { onHeight(it.height) },
                minHeight = minHeight,
                contentPadding = padding,
            )
        }
    }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= TOLERANCE_DP, "$what should be $expected but is $actual.")
    }

    private companion object {
        const val PLACEHOLDER = "Where are you going?"
        const val TOLERANCE_DP = 1.5f
        const val FRAME_DELAY_MS = 16L
        const val SETTLE_MS = 200L
        val TALL_PADDING = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
        val NO_PADDING = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    }
}
