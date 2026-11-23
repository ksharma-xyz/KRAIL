package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The confirm screen: the headline, the real saved trip tile with both names, and the bar with Not now on the left and
 * Yes, save it on the right. The tile is not a button, so the only things that can be pressed are the two answers.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroConfirmStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<IntroUiEvent>()

    private fun show() {
        composeRule.setContent {
            PreviewTheme {
                IntroFlowScreen(
                    state = PreviewConfirm,
                    suggestedStops = DEFAULT_SUGGESTED_STOPS,
                    onEvent = { events += it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the screen shows the headline and the trip as the saved trip tile`() {
        show()

        composeRule.onNodeWithText(CONFIRM_HEADING).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_CONFIRM_TILE).assertIsDisplayed()
    }

    @Test
    fun `a screen reader reads the tile as one trip from the start to the end`() {
        show()

        val tile = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_CONFIRM_TILE).fetchSemanticsNode()
        val description = tile.config.getOrNull(SemanticsProperties.ContentDescription)

        assertEquals(listOf("From Tallawong Station to Central Station"), description)
        assertEquals("From A to B", confirmTileDescription("A", "B"))
    }

    @Test
    fun `the tile is not a button, so nothing but the two answers can be pressed`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_CONFIRM_TILE).assertHasNoClickAction()
    }

    @Test
    fun `the bar says Not now on the left and Yes save it on the right, each sending its own event`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertTextEquals(CONFIRM_NOT_NOW)
            .performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertTextEquals(CONFIRM_SAVE)
            .performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AnswerSaveTrip(yes = false), IntroUiEvent.AnswerSaveTrip(yes = true)),
            events,
        )
    }

    @Test
    fun `neither answer is disabled, because the answer only records it and waits for nothing`() {
        show()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsEnabled()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertIsEnabled()
    }

    @Test
    fun `the burst draws shapes in its colour part way through and nothing at the start or the end`() {
        fun redPixels(progress: Float): Int {
            val bitmap = ImageBitmap(SCENE_PX, SCENE_PX)
            CanvasDrawScope().draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = Canvas(bitmap),
                size = Size(SCENE_PX.toFloat(), SCENE_PX.toFloat()),
            ) {
                drawRect(Color.White)
                drawBurst(progress, Color.Red)
            }
            val pixels = IntArray(SCENE_PX * SCENE_PX)
            bitmap.readPixels(pixels)
            return pixels.count { argb -> (argb shr GREEN_SHIFT and BYTE) < MAX_OTHER && (argb and BYTE) < MAX_OTHER }
        }

        assertEquals(0, redPixels(0f), "nothing is drawn before the burst starts")
        assertTrue(redPixels(0.15f) > 0, "shapes are drawn part way through")
        assertTrue(redPixels(0.15f) > redPixels(0.9f), "the burst has faded by the end")
        assertEquals(0, redPixels(1f), "nothing is left when the burst is over")
    }

    private companion object {
        const val SCENE_PX = 240
        const val GREEN_SHIFT = 8
        const val BYTE = 0xFF
        const val MAX_OTHER = 120
    }
}
