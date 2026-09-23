package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * What the rider said, as words rather than as a field, with stop or speak again and Send.
 *
 * Words, because Ask KRAIL is spoken: an empty text box on open said "type here", and the
 * keyboard it invited covered the rings telling them they were being heard. The sentence only
 * becomes a field when the rider taps it to fix a word the recogniser got wrong, which is the
 * one job typing still has here.
 *
 * Send appears once there is a sentence and the rider has stopped. Speech never presses it.
 */
@Composable
internal fun AiSpokenSentence(
    state: AiSearchInputUiState,
    text: String,
    suggestion: String,
    onStartEditing: () -> Unit,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val hasText = text.isNotBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        if (hasText) {
            Text(
                text = text,
                style = KrailTheme.typography.titleMediumRegular,
                color = KrailTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(dim.radiusM))
                    // Not while it is being heard or worked out: the words are still arriving,
                    // or already on their way to a search.
                    .klickable(enabled = !state.isBusy, onClick = onStartEditing)
                    .padding(dim.spacingS),
            )
        } else if (state.isListening) {
            // The status line says Listening, so the example moves down here for the few
            // seconds before the first word arrives. "Try" and quotes: a demonstration.
            Text(
                text = "Try “$suggestion”",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }

        // A speech problem brings its own single action (AiSpeechProblemAction), and a mic
        // beside it would be a second button for the same thing.
        if (state.speechUnavailableReason == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingXL, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiVoiceControl(state = state, onEvent = onEvent, labelled = false)
                if (hasText && !state.isListening) {
                    AiSendButton(
                        enabled = !state.isBusy,
                        onClick = { onEvent(AiSearchInputEvent.Submit) },
                    )
                }
            }
        }
    }
}

