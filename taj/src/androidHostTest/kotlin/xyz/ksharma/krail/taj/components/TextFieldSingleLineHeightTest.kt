package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * The single line height of taj's [TextField] is 48 dp for every caller that does not ask for anything else, at every text
 * size, and a minimum of the caller's own value when `minHeight` is passed (the home search capsule, whose height is one text
 * line plus 15 dp above and below). Nothing that already uses the field may change, so the default is pinned at 100 and at 200
 * percent text, and the opt in is pinned to grow with the text and to leave a short field at its floor.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class TextFieldSingleLineHeightTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `with no minimum a single line field is exactly 48 dp at 100 percent text`() {
        setField(fontScale = 1f)

        assertNear("height at 100 percent", 48.dp, fieldHeight())
    }

    @Test
    fun `with no minimum a single line field stays 48 dp at 200 percent text, however much padding it is given`() {
        setField(fontScale = 2f, padding = TALL_PADDING)

        assertNear("height at 200 percent with 15 dp above and below", 48.dp, fieldHeight())
    }

    @Test
    fun `with a minimum of 48 dp a field that fits is exactly 48 dp, so a caller that opts in looks the same at 100 percent`() {
        setField(fontScale = 1f, minHeight = TextFieldDefaults.SingleLineHeight)

        assertNear("height at 100 percent", 48.dp, fieldHeight())
    }

    @Test
    fun `with a minimum a field shorter than it is held at the minimum`() {
        // Half size text and 15 dp above and below is 12 + 30, under the 48 dp floor.
        setField(fontScale = 0.5f, padding = TALL_PADDING, minHeight = TextFieldDefaults.SingleLineHeight)

        assertNear("height at 50 percent", 48.dp, fieldHeight())
    }

    @Test
    fun `with a minimum a field grows with its text and keeps its padding constant`() {
        val fontScale = mutableFloatStateOf(1f)
        setField(fontScaleState = fontScale, padding = TALL_PADDING, minHeight = TextFieldDefaults.SingleLineHeight)

        val heights = listOf(1f, 1.3f, 2f).map { scale ->
            fontScale.floatValue = scale
            composeRule.waitForIdle()
            assertNear("padding above and below the line at $scale", 30.dp, fieldHeight() - placeholderHeight())
            fieldHeight()
        }

        assertTrue(heights[0] < heights[1] && heights[1] < heights[2], "The field grows with the text: $heights")
        assertTrue(heights.all { it > 48.dp }, "Each is taller than the 48 dp floor: $heights")
    }

    private fun setField(
        fontScale: Float = 1f,
        padding: PaddingValues? = null,
        minHeight: Dp? = null,
        // A text size that changes while the field is up, in place of [fontScale]: a rule sets its content once.
        fontScaleState: State<Float>? = null,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScaleState?.value ?: fontScale)) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    Field(padding = padding, minHeight = minHeight)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Field(padding: PaddingValues?, minHeight: Dp?) {
        val state = remember { TextFieldState() }
        TextField(
            state = state,
            placeholder = PLACEHOLDER,
            readOnly = true,
            modifier = Modifier.testTag(FIELD_TAG),
            minHeight = minHeight,
            contentPadding = padding,
        )
    }

    private fun fieldHeight(): Dp = composeRule.onNodeWithTag(FIELD_TAG, useUnmergedTree = true).getBoundsInRoot().height

    private fun placeholderHeight(): Dp =
        composeRule.onNodeWithText(PLACEHOLDER, useUnmergedTree = true).getBoundsInRoot().height

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= TOLERANCE_DP, "$what should be $expected but is $actual.")
    }

    private companion object {
        const val FIELD_TAG = "textfield.test"
        const val PLACEHOLDER = "Where are you going?"
        const val TOLERANCE_DP = 1.5f
        val TALL_PADDING = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
    }
}
