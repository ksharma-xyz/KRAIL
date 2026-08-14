package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.components.AiListeningIndicator
import xyz.ksharma.krail.taj.components.AiThinkingIndicator
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * The AI input, rendered *in place of* [SearchStopRow]'s From/To fields — same slot, same
 * width, and [height] fixed to what the two fields occupy, so opening and closing the box
 * never changes the row's height. Typing, listening and extracting all render inside this one
 * box for the same reason.
 *
 * Nothing here confirms a trip: a resolved intent is written straight into the row's own
 * From/To fields (see `SavedTripsEntry`) and the rider taps Search exactly as they would
 * after picking stops by hand.
 */
@Composable
internal fun AiInlineSearchBox(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.boxContent(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        // One surface for every state. Nothing in this row draws directly on the theme band:
        // the fields and buttons each have their own surface, and the label tokens are defined
        // against that surface rather than a band whose colour follows the rider's theme. Text
        // straight on the band came out grey-on-orange.
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(KrailTheme.dimensions.spacingXL))
            .background(KrailTheme.colors.surface),
        label = "AiInlineSearchBox",
    ) { content ->
        when (content) {
            AiBoxContent.LISTENING -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AiListeningIndicator(active = true)
                // Partial transcript, so the rider can see it is hearing them correctly and
                // stop early if it isn't — a bare waveform proves only that the mic is live.
                if (state.speechTranscript.isNotBlank()) {
                    BoxMessage(text = state.speechTranscript)
                }
            }
            AiBoxContent.EXTRACTING -> Centered { AiThinkingIndicator() }
            AiBoxContent.DOWNLOADING -> Centered {
                BoxMessage(text = "Setting up on-device AI. Try again shortly.")
            }

            AiBoxContent.NO_MIC -> MessageWithTextEntry(
                message = "Mic needs permission. Type instead.",
                state = state,
                onEvent = onEvent,
            )

            AiBoxContent.MIC_FAILED -> MessageWithTextEntry(
                message = "Mic didn't start. Tap it again, or type.",
                state = state,
                onEvent = onEvent,
            )

            AiBoxContent.FAILED -> MessageWithTextEntry(
                message = "Couldn't work that out. Try rewording.",
                state = state,
                onEvent = onEvent,
            )

            AiBoxContent.INPUT -> AiTextEntry(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Collapses [AiSearchInputUiState] to what the box actually draws, so [AiInlineSearchBox]'s
 * `AnimatedContent` only re-runs its transition when the drawn content really changes — keying
 * it on the state itself would restart the animation (and rebuild the text field) on every
 * keystroke.
 */
private enum class AiBoxContent { INPUT, LISTENING, EXTRACTING, DOWNLOADING, FAILED, NO_MIC, MIC_FAILED }

private fun AiSearchInputUiState.boxContent(): AiBoxContent = when {
    isListening -> AiBoxContent.LISTENING
    phase == AiSearchInputPhase.EXTRACTING -> AiBoxContent.EXTRACTING
    phase == AiSearchInputPhase.DOWNLOADING -> AiBoxContent.DOWNLOADING
    phase == AiSearchInputPhase.UNRESOLVED -> AiBoxContent.FAILED
    // Speech going nowhere has to say so: the rider tapped the mic and would otherwise
    // watch the box do nothing at all. Which thing went wrong matters, though — see
    // [isPermissionFailure].
    speechUnavailableReason == null -> AiBoxContent.INPUT
    speechUnavailableReason.isPermissionFailure() -> AiBoxContent.NO_MIC
    else -> AiBoxContent.MIC_FAILED
}

/**
 * The speech service reports several unrelated failures as reason strings
 * (`permission_required`, `not_available`, `recognizer_error_<code>`, `no_result`), and only
 * some of them are about permission.
 *
 * Telling a rider who has already granted the microphone that they have not is worse than
 * saying nothing: it sends them to a settings screen where everything is already correct.
 * Tapping the mic while listening and again straight after is the easy way to hit this — the
 * platform recogniser is still winding down and returns a busy error, which is a retry, not a
 * permission problem.
 */
private fun String.isPermissionFailure(): Boolean =
    this == "permission_required" || this == "no_permission"

/** A short message above the text entry, on the box's own surface. */
@Composable
private fun MessageWithTextEntry(
    message: String,
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = Modifier.fillMaxSize().padding(top = dim.spacingS),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        BoxMessage(text = message)
        // weight, not fillMaxSize: a Column child that fills the parent's whole height would
        // push the message out of the fixed-height box.
        AiTextEntry(state = state, onEvent = onEvent, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun BoxMessage(text: String) {
    Text(
        text = text,
        style = KrailTheme.typography.bodySmall,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier.padding(horizontal = KrailTheme.dimensions.spacingM),
    )
}

/**
 * The rider's text survives a failed extraction ([AiSearchInputUiState.typedText] is never
 * cleared on [AiSearchInputPhase.UNRESOLVED]), so rewording means editing what's already
 * there rather than typing the whole sentence again.
 */
@Composable
private fun AiTextEntry(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Opening the box is already the rider saying "I want to type here", so focus and the
    // keyboard come with it rather than costing a second tap.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TextField(
        modifier = modifier.focusRequester(focusRequester),
        placeholder = "Tell me your trip, e.g. leaving home around 9, need to be at Central by 6:30pm",
        initialText = state.typedText,
        // Body, not the single-line field's titleLarge — several lines of a spoken sentence
        // have to fit the fixed box height.
        textStyle = KrailTheme.typography.bodyLarge.copy(color = KrailTheme.colors.onSurface),
        imeAction = ImeAction.Send,
        lineLimits = TextFieldLineLimits.MultiLine(),
        onSubmit = { onEvent(AiSearchInputEvent.Submit) },
        onTextChange = { onEvent(AiSearchInputEvent.TypedTextChanged(it.toString())) },
    )
}
