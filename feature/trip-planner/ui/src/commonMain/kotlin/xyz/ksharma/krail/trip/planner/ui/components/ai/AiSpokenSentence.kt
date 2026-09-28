package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
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
        // No spacedBy. The sentence slot is always composed now, so it can crossfade, and an
        // empty slot still took a gap above the mic. The gap belongs to the mic row instead, and
        // only when there is something above it.
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Crossfaded rather than swapped: the hint dissolving into the first heard word is the
        // moment the rider learns they are being understood, and a hard cut there read as the
        // screen resetting. Keyed on which thing is showing, not on the words, so the sentence
        // growing as words arrive updates in place instead of fading on every partial.
        val showing = when {
            hasText -> SentenceSlot.WORDS
            state.isListening -> SentenceSlot.HINT
            else -> SentenceSlot.NONE
        }
        AnimatedContent(
            targetState = showing,
            transitionSpec = { swapInPlace() },
            contentAlignment = Alignment.Center,
            label = "aiSpokenSentence",
        ) { slot ->
            when (slot) {
                SentenceSlot.WORDS -> Text(
                    text = text,
                    style = KrailTheme.typography.titleMediumRegular,
                    color = KrailTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dim.radiusM))
                        // Not while it is being heard or worked out: the words are still
                        // arriving, or already on their way to a search.
                        .klickable(enabled = !state.isBusy, onClick = onStartEditing)
                        .padding(dim.spacingS),
                )

                // The status line says Listening, so the example moves down here for the few
                // seconds before the first word arrives. "Try" and quotes: a demonstration.
                SentenceSlot.HINT -> Text(
                    text = "Try \u201C$suggestion\u201D",
                    style = KrailTheme.typography.bodySmall,
                    color = KrailTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )

                SentenceSlot.NONE -> Unit
            }
        }

        // A speech problem brings its own single action (AiSpeechProblemAction), and a mic
        // beside it would be a second button for the same thing. It folds away on the same
        // clock as the banner arriving above it, so the two read as one change.
        AnimatedVisibility(
            visible = state.speechUnavailableReason == null,
            enter = foldIn(),
            exit = foldOut(),
            modifier = Modifier.padding(top = if (showing == SentenceSlot.NONE) 0.dp else dim.spacingM),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingXL, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiVoiceControl(state = state, onEvent = onEvent, labelled = false)
                // Opens its own width as it springs in, so the mic slides aside to make room
                // rather than jumping when the rider stops speaking.
                AnimatedVisibility(
                    visible = hasText && !state.isListening,
                    enter = sendButtonEnter(expandFrom = Alignment.Start),
                    exit = sendButtonExit(shrinkTowards = Alignment.Start),
                ) {
                    AiSendButton(
                        enabled = !state.isBusy,
                        onClick = { onEvent(AiSearchInputEvent.Submit) },
                    )
                }
            }
        }
    }
}

private enum class SentenceSlot { WORDS, HINT, NONE }
