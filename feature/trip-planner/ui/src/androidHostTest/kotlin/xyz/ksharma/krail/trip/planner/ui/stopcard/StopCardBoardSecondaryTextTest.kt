package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals

/**
 * The stop card is drawn on the soft tint, where the board's usual secondary colour (`softLabel`) is under 4.5 to 1, so the
 * card gives the board its own text colour. The board's text is the colour the card reads in, not `softLabel`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class StopCardBoardSecondaryTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the open board on the stop card draws its secondary text in the card's own text colour`() {
        val cardInk = Color(0xFF0A1B2C)
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalContentColor provides cardInk) {
                    StopCardBoardSection(
                        stopId = "stop_central",
                        stopName = "Central Station",
                        state = DeparturesState(isLoading = false),
                        onEvent = {},
                        boardOpenAtStart = true,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("No upcoming departures found.", useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertEquals(cardInk, results.first().layoutInput.style.color)
    }
}
