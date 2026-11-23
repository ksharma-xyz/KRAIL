package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.park.ride.ui.components.DirectionsButton
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The one Directions button every car park carries, taj's round icon button: a 48 dp target, spoken with the
 * car park's name, and one tap for one hand off. Run at density 1 so a dp is a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [DIRECTIONS_TEST_SDK], manifest = Config.NONE, qualifiers = "w411dp-h891dp")
class DirectionsButtonLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var taps = 0

    @Test
    fun `the button is 48 dp square, one button, spoken with the car park`() {
        show()

        val bounds = composeRule.onNodeWithTag(TAG).getVisibleBoundsInRoot()
        assertNear("width", 48.dp.value, bounds.width.value)
        assertNear("height", 48.dp.value, bounds.height.value)
        val node = composeRule.onNodeWithContentDescription(LABEL).fetchSemanticsNode()
        assertEquals(Role.Button, node.config[SemanticsProperties.Role], "it is a button")
    }

    @Test
    fun `a tap hands off once`() {
        show()

        composeRule.onNodeWithTag(TAG).performClick()

        assertEquals(1, taps)
    }

    private fun show() {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    DirectionsButton(
                        carParkName = CAR_PARK,
                        onClick = { taps++ },
                        modifier = Modifier.testTag(TAG),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertNear(what: String, expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) <= 1.5f, "$what should be $expected but is $actual.")
    }

    private companion object {
        const val TAG = "directions"
        const val CAR_PARK = "Tallawong P1"
        const val LABEL = "Directions to $CAR_PARK"
    }
}

private const val DIRECTIONS_TEST_SDK = 34
