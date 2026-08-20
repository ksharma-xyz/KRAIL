package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * The status slot and the input bar have to coexist inside whatever height the host gives.
 *
 * `AiInputContent` is a `Column` of one weighted, scrollable status slot and one unweighted
 * bar. That ordering is the whole layout: the slot yields its space when the column is
 * squeezed, so the bar and its controls are never the thing that gets pushed out.
 *
 * Scope, stated honestly, and NARROWER than it used to be. These two tests used to compare
 * `getUnclippedBoundsInRoot()` against the host height to claim the bar sat in the bottom
 * half. That claim is gone, deliberately: bounds report where a node was laid out, not
 * whether any of it can be seen, and the one bug this file exists downstream of was a node
 * with perfectly legal bounds that an ancestor clip had removed from the screen. A probe that
 * cannot fail reads exactly like a probe that works. `scripts/check_layout_invariants.py` now
 * fails any `*LayoutTest` that reaches for bounds; see docs/LAYOUT_AND_INSETS.md §1.4.
 *
 * What is left is what a display assertion can actually prove: at an ordinary height both the
 * greeting and the bar are visible at once, and at a squeezed height the bar still is. Vertical
 * ORDER is no longer asserted here — nothing display-based can see it — and the reordering that
 * would break it is caught by eye and by the previews instead.
 *
 * The other bug in the same family, the window panning for the keyboard, cannot be reached from
 * a unit test at all: it needs a real window and a real IME.
 * `scripts/check_layout_invariants.py` guards it too. See
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
    fun greetingAndInputBarAreBothVisibleAtOnce() {
        setContent(hostHeight = HOST_HEIGHT)

        // Neither is allowed to push the other out. The greeting lives in a fixed-height slot
        // above the bar, so a slot that grew, or a bar that stopped being pinned, shows up as
        // one of these two disappearing.
        composeRule.onNodeWithText(GREETING).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(SPEAK).assertIsDisplayed()
    }

    @Test
    fun squeezedHost_theBarKeepsItsControls() {
        // The height a keyboard leaves on a phone. The status slot is the weighted child and
        // scrolls; the bar is not, so the squeeze has to come out of the slot. Swapping which
        // child carries the weight leaves the bar measured after there is any room left for it,
        // and its controls end up laid out past the column's bottom edge — visible to
        // assertIsDisplayed, invisible to any bounds comparison.
        setContent(hostHeight = SQUEEZED_HOST_HEIGHT)

        composeRule.onNodeWithContentDescription(SPEAK).assertIsDisplayed()
    }

    private fun setContent(hostHeight: Dp) {
        composeRule.setContent {
            PreviewTheme {
                Column(modifier = Modifier.fillMaxWidth().height(hostHeight)) {
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
    }

    private companion object {
        val HOST_HEIGHT = 800.dp

        // Roughly what is left of a phone screen with the keyboard up.
        val SQUEEZED_HOST_HEIGHT = 340.dp

        const val SUGGESTION = "Home to Work by 9am"

        // The greeting as the slot actually renders it: the suggestion is quoted and prefixed,
        // because the line is a demonstration rather than a prediction.
        const val GREETING = "Try “Home to Work by 9am”"

        // The mic's own description while idle. It is the one control that exists only inside
        // the bar, so finding it is finding the bar without a test tag in production code.
        const val SPEAK = "Speak"
    }
}
