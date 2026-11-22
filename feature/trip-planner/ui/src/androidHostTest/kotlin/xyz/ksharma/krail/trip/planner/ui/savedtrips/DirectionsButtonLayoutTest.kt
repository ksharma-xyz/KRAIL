package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.park.ride.ui.components.DIRECTIONS_BUTTON_STYLE
import xyz.ksharma.krail.park.ride.ui.components.DirectionsButton
import xyz.ksharma.krail.park.ride.ui.components.DirectionsButtonStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The one Directions button every car park carries, in both of its looks: a 48 dp target whatever the
 * shape, spoken with the car park's name, one tap for one hand off, and the diamond's corners still a
 * touch. Run at density 1 so a dp is a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [DIRECTIONS_TEST_SDK], manifest = Config.NONE, qualifiers = "w411dp-h891dp")
class DirectionsButtonLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var taps = 0
    private var style by mutableStateOf(DirectionsButtonStyle.Diamond)

    @Test
    fun `the button ships as the diamond`() {
        assertEquals(DirectionsButtonStyle.Diamond, DIRECTIONS_BUTTON_STYLE)
    }

    @Test
    fun `both looks are 48 dp square, one button, spoken with the car park`() {
        show()
        DirectionsButtonStyle.entries.forEach { look ->
            style = look
            composeRule.waitForIdle()

            val bounds = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            assertNear("$look width", 48.dp.value, bounds.width.value)
            assertNear("$look height", 48.dp.value, bounds.height.value)
            val node = composeRule.onNodeWithContentDescription(LABEL).fetchSemanticsNode()
            assertEquals(Role.Button, node.config[SemanticsProperties.Role], "$look is a button")
        }
    }

    @Test
    fun `a tap hands off once in either look`() {
        show()
        DirectionsButtonStyle.entries.forEach { look ->
            style = look
            composeRule.waitForIdle()
            taps = 0

            composeRule.onNodeWithTag(TAG).performClick()

            assertEquals(1, taps, "$look")
        }
    }

    @Test
    fun `the diamond's corners are still part of the 48 dp touch target`() {
        show()
        style = DirectionsButtonStyle.Diamond
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG).performTouchInput { click(Offset(2f, 2f)) }

        assertEquals(1, taps, "A tap on the corner of the 48 dp box missed the button")
    }

    private fun show() {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    DirectionsButton(
                        carParkName = CAR_PARK,
                        onClick = { taps++ },
                        modifier = Modifier.testTag(TAG),
                        style = style,
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
