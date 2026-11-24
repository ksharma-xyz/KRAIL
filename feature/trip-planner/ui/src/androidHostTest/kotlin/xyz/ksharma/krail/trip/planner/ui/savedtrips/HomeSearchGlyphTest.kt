package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * The search glyph the home capsule starts with: the default 24 dp icon size at 100 percent text, growing with the text as taj's
 * decorative icon size does (times the font scale, and times 0.8 of it past 150 percent), and square.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w411dp-h891dp")
class HomeSearchGlyphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun size(fontScale: Float): Pair<Dp, Dp> {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalTextColor provides Color.Black,
            ) {
                PreviewTheme { HomeSearchGlyph() }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_SEARCH_GLYPH).assertIsDisplayed()
        val bounds = composeRule.onNodeWithTag(TripPlannerTestTags.HOME_SEARCH_GLYPH).getVisibleBoundsInRoot()
        return bounds.width to bounds.height
    }

    @Test
    fun `the glyph is 24 dp square at 100 percent text`() {
        val (width, height) = size(fontScale = 1f)

        assertNear("width", 24.dp, width)
        assertNear("height", 24.dp, height)
    }

    @Test
    fun `the glyph grows with the text up to 150 percent as taj's decorative icon size does`() {
        val (width, height) = size(fontScale = 1.3f)

        assertNear("width at 130 percent", 31.2.dp, width)
        assertNear("height at 130 percent", 31.2.dp, height)
    }

    @Test
    fun `past 150 percent the glyph grows at 0 point 8 of the text, so it does not outgrow the capsule`() {
        val (width, height) = size(fontScale = 2f)

        assertNear("width at 200 percent", 38.4.dp, width)
        assertNear("height at 200 percent", 38.4.dp, height)
    }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "The glyph's $what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val TOLERANCE_DP = 1f
    }
}
