package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.UnresolvedReason

/**
 * The dialog's status line has exactly one voice at a time, and the order of precedence is
 * the contract: resolved beats busy, busy beats the hint, and the hint only ever holds the
 * stage when nothing is happening.
 *
 * The regression that motivates this: the working border keeps turning for a beat after the
 * answer, then stops — and the busy word used to leave with it, so the "Try …" hint flashed
 * back for the last half-second before the dialog closed. The rider saw three states in two
 * seconds, ending on an invitation to type just as the surface left. RESOLVED must therefore
 * pin the line to "Found it" REGARDLESS of whether the border is still in its beat.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class AskKrailStatusLineTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: AiSearchInputUiState, busyVisible: Boolean) {
        composeRule.setContent {
            PreviewTheme {
                AiDialogContent(
                    state = state,
                    textFieldState = rememberTextFieldState(),
                    suggestion = SUGGESTION,
                    busyVisible = busyVisible,
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun idle_showsTheHintAndNothingElse() {
        setContent(AiSearchInputUiState(isInputOpen = true), busyVisible = false)

        composeRule.onNodeWithText(SUGGESTION, substring = true).assertExists()
        composeRule.onNodeWithText(THINKING).assertDoesNotExist()
        composeRule.onNodeWithText(FOUND).assertDoesNotExist()
    }

    @Test
    fun extracting_saysThinkingAndHidesTheHint() {
        setContent(
            AiSearchInputUiState(isInputOpen = true, phase = AiSearchInputPhase.EXTRACTING),
            busyVisible = true,
        )

        composeRule.onNodeWithText(THINKING).assertExists()
        composeRule.onNodeWithText(SUGGESTION, substring = true).assertDoesNotExist()
    }

    @Test
    fun listening_saysListening() {
        setContent(
            AiSearchInputUiState(isInputOpen = true, isListening = true),
            busyVisible = true,
        )

        composeRule.onNodeWithText(LISTENING).assertExists()
        composeRule.onNodeWithText(THINKING).assertDoesNotExist()
    }

    @Test
    fun resolvedAfterTheBordersBeat_holdsFoundIt_neverTheHint() {
        // busyVisible = false is the moment the old bug fired: border settled, busy word gone,
        // hint flashing back while the dialog was already on its way out.
        setContent(
            AiSearchInputUiState(isInputOpen = true, phase = AiSearchInputPhase.RESOLVED),
            busyVisible = false,
        )

        composeRule.onNodeWithText(FOUND).assertExists()
        composeRule.onNodeWithText(SUGGESTION, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(THINKING).assertDoesNotExist()
    }

    @Test
    fun resolvedDuringTheBordersBeat_alreadySaysFoundIt_notThinking() {
        setContent(
            AiSearchInputUiState(isInputOpen = true, phase = AiSearchInputPhase.RESOLVED),
            busyVisible = true,
        )

        composeRule.onNodeWithText(FOUND).assertExists()
        composeRule.onNodeWithText(THINKING).assertDoesNotExist()
    }

    @Test
    fun unresolved_bringsTheHintBackAlongsideTheProblem() {
        setContent(
            AiSearchInputUiState(
                isInputOpen = true,
                phase = AiSearchInputPhase.UNRESOLVED,
                unresolvedReason = UnresolvedReason.COULD_NOT_READ,
            ),
            busyVisible = false,
        )

        composeRule.onNodeWithText("did not come through", substring = true).assertExists()
        composeRule.onNodeWithText(SUGGESTION, substring = true).assertExists()
    }

    private companion object {
        const val SUGGESTION = "Home to Work by 9am"
        const val THINKING = "Thinking…"
        const val FOUND = "Found it"
        const val LISTENING = "Listening"
    }
}
