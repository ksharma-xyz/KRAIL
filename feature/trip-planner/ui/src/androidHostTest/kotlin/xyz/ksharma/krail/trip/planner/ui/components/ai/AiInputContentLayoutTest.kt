package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * The input bar has to sit at the BOTTOM of whatever height it is given.
 *
 * Scope, stated honestly. This guards the CHILD ORDER and anchoring of the content: that the
 * status slot takes the empty space above and the bar stays pinned below it, inside whatever
 * height the host gives. Reordering the column, or letting the status slot grow unbounded,
 * fails here.
 *
 * It does NOT distinguish `weight(1f)` from `fillMaxSize()` at the call site. That was checked
 * by mutation: reverting the call site to `fillMaxSize()` leaves both tests green, because a
 * `Column` hands a non-weighted child the remaining bounded height and the two are equivalent.
 * That result is what disproved the original diagnosis of the incident below.
 *
 * The actual bug in that incident, the window panning for the keyboard, cannot be reached from
 * a unit test at all: it needs a real window and a real IME.
 * `scripts/check_layout_invariants.py` guards it. See
 * docs/learning/2026-08-16-ime-pan-and-unbounded-column.md.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class AiInputContentLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inputBar_sitsInTheBottomPortionOfTheAvailableHeight() {
        composeRule.setContent {
            PreviewTheme {
                Column(modifier = Modifier.fillMaxWidth().height(HOST_HEIGHT)) {
                    AiInputContent(
                        state = AiSearchInputUiState(isInputOpen = true),
                        textFieldState = rememberTextFieldState(),
                        suggestion = SUGGESTION,
                        onEvent = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // The mic is the one control that exists only inside the bar, so its position is the
        // bar's position without needing a test tag in production code.
        val mic = composeRule.onNodeWithContentDescription(SPEAK).getUnclippedBoundsInRoot()

        assertTrue(
            "Input bar should sit below the halfway line of a $HOST_HEIGHT host, " +
                "but its mic was at ${mic.top}. A bar near the top means the content wrapped " +
                "its height instead of filling the space it was given.",
            mic.top > HOST_HEIGHT / 2,
        )
    }

    @Test
    fun statusSlot_leavesTheBarAtTheBottomWhenTheGreetingIsShowing() {
        composeRule.setContent {
            PreviewTheme {
                Column(modifier = Modifier.fillMaxWidth().height(HOST_HEIGHT)) {
                    AiInputContent(
                        state = AiSearchInputUiState(isInputOpen = true),
                        textFieldState = rememberTextFieldState(),
                        suggestion = SUGGESTION,
                        onEvent = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // The greeting takes the empty space above; it must not push the bar off the bottom
        // or be pushed off itself. Both are inside the host.
        val mic = composeRule.onNodeWithContentDescription(SPEAK).getUnclippedBoundsInRoot()

        assertTrue(
            "Input bar should stay within the host, but its mic bottom was ${mic.bottom}.",
            mic.bottom <= HOST_HEIGHT,
        )
    }

    private companion object {
        val HOST_HEIGHT = 800.dp
        const val SUGGESTION = "Home to Work by 9am"
        const val SPEAK = "Speak"
    }
}
