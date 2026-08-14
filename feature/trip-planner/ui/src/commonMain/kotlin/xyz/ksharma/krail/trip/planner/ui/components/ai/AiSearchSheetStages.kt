package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.components.AiListeningIndicator
import xyz.ksharma.krail.taj.components.AiThinkingIndicator
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

private val MarkSize = 84.dp

/**
 * One stage of [AiSearchSheetContent]. Each is a whole screenful of sheet rather than a
 * variation on a shared skeleton: what the rider is doing in each of these is genuinely
 * different, and a single layout with six sets of conditional bits was what made the previous
 * in-row version so hard to read.
 */
@Composable
internal fun AiSearchSheetStage(
    stage: AiSheetStage,
    state: AiSearchInputUiState,
    gradient: List<Color>,
    stopLabels: ImmutableList<StopLabel>,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (stage) {
        AiSheetStage.READY -> ReadyStage(
            gradient = gradient,
            stopLabels = stopLabels,
            speechUnavailableReason = state.speechUnavailableReason,
            onEvent = onEvent,
            onLabelClick = onLabelClick,
            onTypeInstead = { onTypingChange(true) },
            modifier = modifier,
        )

        AiSheetStage.LISTENING -> ListeningStage(
            gradient = gradient,
            transcript = state.speechTranscript,
            onStop = { onEvent(AiSearchInputEvent.StopListening) },
            modifier = modifier,
        )

        AiSheetStage.TYPING -> TypingStage(
            initialText = state.typedText,
            stopLabels = stopLabels,
            onEvent = onEvent,
            onLabelClick = onLabelClick,
            onSpeakInstead = { onTypingChange(false) },
            modifier = modifier,
        )

        AiSheetStage.THINKING -> ThinkingStage(gradient = gradient, modifier = modifier)

        AiSheetStage.DOWNLOADING -> MessageStage(
            gradient = gradient,
            title = "Getting ready",
            body = "KRAIL is still downloading the on device model. Try again in a moment.",
            primaryLabel = "Try again",
            onPrimary = { onEvent(AiSearchInputEvent.Submit) },
            stopLabels = stopLabels,
            onLabelClick = onLabelClick,
            modifier = modifier,
        )

        AiSheetStage.FAILED -> MessageStage(
            gradient = gradient,
            title = "I could not place that",
            body = failedBody(state.typedText),
            primaryLabel = "Try again",
            onPrimary = {
                onEvent(AiSearchInputEvent.StartOver)
                onTypingChange(false)
            },
            stopLabels = stopLabels,
            onLabelClick = onLabelClick,
            modifier = modifier,
        )
    }
}

private fun failedBody(typedText: String): String = if (typedText.isBlank()) {
    "Try a stop name, or pick one of your places."
} else {
    "I heard \"$typedText\". Try a stop name, or pick one of your places."
}

@Composable
private fun ReadyStage(
    gradient: List<Color>,
    stopLabels: ImmutableList<StopLabel>,
    speechUnavailableReason: String?,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StageColumn(modifier = modifier) {
        AiWheelMark(spinning = false, colors = gradient, markSize = MarkSize)
        StageHeading(title = "Where to?", body = "Say it the way you would say it to a friend.")
        AiLabelChipRow(stopLabels = stopLabels, onLabelClick = onLabelClick)
        speechUnavailableReason?.let { MicProblem(reason = it) }
        SpeakButton(onEvent = onEvent)
        TextButton(onClick = onTypeInstead) { Text(text = "Type instead") }
    }
}

@Composable
private fun ListeningStage(
    gradient: List<Color>,
    transcript: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StageColumn(modifier = modifier) {
        AiListeningIndicator(active = true, colors = gradient)
        StageHeading(
            title = transcript.ifBlank { "Listening" },
            // The partial transcript IS the heading once there is one, so the line underneath
            // stops repeating "listening" back at a rider who can already see the waveform.
            body = if (transcript.isBlank()) "Tell me where you are going." else "",
        )
        TextButton(onClick = onStop) { Text(text = "Stop") }
    }
}

@Composable
private fun ThinkingStage(gradient: List<Color>, modifier: Modifier = Modifier) {
    StageColumn(modifier = modifier) {
        AiThinkingIndicator(active = true, colors = gradient)
        StageHeading(title = "Working it out", body = "")
    }
}

@Composable
private fun TypingStage(
    initialText: String,
    stopLabels: ImmutableList<StopLabel>,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    onSpeakInstead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val textFieldState = rememberTextFieldState(initialText = initialText)

    StageColumn(modifier = modifier) {
        StageHeading(title = "Where to?", body = "")
        TextField(
            state = textFieldState,
            placeholder = "Home to work, leaving at nine",
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 4),
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { onEvent(AiSearchInputEvent.TypedTextChanged(it.toString())) },
        )
        AiLabelChipRow(stopLabels = stopLabels, onLabelClick = onLabelClick)
        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                enabled = textFieldState.text.isNotBlank(),
                onClick = { onEvent(AiSearchInputEvent.Submit) },
            ) {
                Text(text = "Find trip")
            }
            TextButton(onClick = onSpeakInstead) { Text(text = "Speak") }
        }
    }
}

@Composable
private fun MessageStage(
    gradient: List<Color>,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    stopLabels: ImmutableList<StopLabel>,
    onLabelClick: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    StageColumn(modifier = modifier) {
        AiWheelMark(spinning = false, colors = gradient, markSize = MarkSize)
        StageHeading(title = title, body = body)
        AiLabelChipRow(stopLabels = stopLabels, onLabelClick = onLabelClick)
        Button(onClick = onPrimary) { Text(text = primaryLabel) }
    }
}
