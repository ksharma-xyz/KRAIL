package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * The bar's controls stay visible when the bar itself is squeezed.
 *
 * This is the probe for the incident in
 * docs/learning/2026-08-16-clipped-inside-its-own-parent.md. `AiInputBar`'s `Column` measures
 * its non-weighted children first, so the action `Row` only survives a squeeze because the
 * `TextField` above it carries `weight(1f, fill = false)`. Without that weight the field takes
 * its natural height up to six lines, the row is laid out past the bar's own bottom edge, and
 * the bar's `clip()` removes it from the screen while its unclipped bounds still read as
 * on screen.
 *
 * Scope, stated honestly. This guards ONE thing: that both controls are *visible* when the bar
 * is given less height than six lines of text plus its controls need. It says nothing about
 * where they sit, how they look, or what they do. It is deliberately not a bounds assertion —
 * see the companion note on [SEND_DESCRIPTION] and §1.4 of docs/LAYOUT_AND_INSETS.md.
 *
 * Mutation-checked in both directions, which is the only reason it is trustworthy: removing
 * `weight(weight = 1f, fill = false)` from the `TextField` in `AiInputBar.kt` fails both tests
 * here; restoring it passes them. A probe that cannot fail and a probe that cannot pass look
 * identical to a probe that works.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class AiInputBarLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shortHost_theBarsActionsStayInsideIt() {
        setContent(typed = SIX_LINES, hostHeight = SQUEEZED_HOST_HEIGHT)

        // assertIsDisplayed, never getUnclippedBoundsInRoot. Bounds report where a node was
        // laid out, not whether any of it can be seen; the whole shape of this bug is a node
        // with legal-looking bounds that an ancestor clip has removed. Only a display
        // assertion walks the clip chain.
        composeRule.onNodeWithContentDescription(MIC_DESCRIPTION).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(SEND_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun roomyHost_theBarsActionsAreStillThere() {
        // The other direction of the same claim. If the probe only ever ran squeezed, a fix
        // that hid the controls at every height would still read as green here.
        setContent(typed = SIX_LINES, hostHeight = ROOMY_HOST_HEIGHT)

        composeRule.onNodeWithContentDescription(MIC_DESCRIPTION).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(SEND_DESCRIPTION).assertIsDisplayed()
    }

    private fun setContent(typed: String, hostHeight: Dp) {
        composeRule.setContent {
            PreviewTheme {
                Column(modifier = Modifier.fillMaxWidth().height(hostHeight)) {
                    AiInputBar(
                        state = AiSearchInputUiState(isInputOpen = true, typedText = typed),
                        // Seeded from the same text the state claims. The bar measures the
                        // TextFieldState handed to it, NOT state.typedText: a fresh
                        // rememberTextFieldState leaves the field one line tall, nothing
                        // overflows, and the probe goes green against a live bug.
                        textFieldState = rememberTextFieldState(initialText = typed),
                        placeholder = PLACEHOLDER,
                        onEvent = {},
                    )
                }
            }
        }
    }

    private companion object {
        // What the bar actually gets on a Pixel with the keyboard open: the surface's column,
        // less the fixed status slot above the bar and the spacer between them. Measured
        // against a device, not picked to make the test pass.
        val SQUEEZED_HOST_HEIGHT = 180.dp

        // Comfortably more than six lines plus controls need, so nothing is clipped for any
        // reason.
        val ROOMY_HOST_HEIGHT = 800.dp

        // The field grows to six lines and then stops. Six is its worst case.
        val SIX_LINES = (1..6).joinToString("\n") { "line $it of a long sentence" }

        const val PLACEHOLDER = "Where to?"

        // The mic's own description. It is "Speak" only while idle — the same node reads
        // "Stop listening" once a rider is talking — so the state above must stay idle for
        // this finder to resolve.
        const val MIC_DESCRIPTION = "Speak"

        // The send button's own description, which is "Continue" and not "Send". Worth naming
        // rather than inlining: a probe asserting on a description no node carries fails
        // whatever the layout does, and the original one did, for three rounds, while reading
        // as a real clipping bug. Resolve a finder against the tree, not against memory.
        const val SEND_DESCRIPTION = "Continue"
    }
}
