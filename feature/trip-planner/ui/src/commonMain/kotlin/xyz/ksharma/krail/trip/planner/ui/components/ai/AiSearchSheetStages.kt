package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

private val MarkSize = 72.dp

private const val INPUT_MIN_LINES = 3
private const val INPUT_MAX_LINES = 6

/**
 * One stage of [AiSearchSheetContent]. Each is a whole screenful of sheet rather than a
 * variation on a shared skeleton: what the rider is doing in each of these is genuinely
 * different, and a single layout with a set of conditional bits was what made the previous
 * in-row version so hard to read.
 */
@Composable
internal fun AiSearchSheetStage(
    stage: AiSheetStage,
    state: AiSearchInputUiState,
    gradient: List<Color>,
    stopLabels: ImmutableList<StopLabel>,
    textFieldState: TextFieldState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (stage) {
        AiSheetStage.INPUT -> InputStage(
            state = state,
            gradient = gradient,
            stopLabels = stopLabels,
            textFieldState = textFieldState,
            onEvent = onEvent,
            onLabelClick = onLabelClick,
            modifier = modifier,
        )

        AiSheetStage.LISTENING -> ListeningStage(
            gradient = gradient,
            transcript = state.speechTranscript,
            onStop = { onEvent(AiSearchInputEvent.StopListening) },
            modifier = modifier,
        )

        AiSheetStage.THINKING -> ThinkingStage(gradient = gradient, modifier = modifier)
    }
}

/**
 * The box is always here and the microphone sits beside it. Nothing asks the rider to choose
 * between speaking and typing first: one of the two is already in front of them, and the other
 * is one tap away in the same place every time.
 */
@Composable
private fun InputStage(
    state: AiSearchInputUiState,
    gradient: List<Color>,
    stopLabels: ImmutableList<StopLabel>,
    textFieldState: TextFieldState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    StageColumn(modifier = modifier) {
        AiWheelMark(spinning = false, colors = gradient, markSize = MarkSize)
        StageHeading(title = "Where to?", body = "Say it the way you would say it to a friend.")

        state.problemMessage()?.let { StageProblem(message = it) }

        TextField(
            state = textFieldState,
            placeholder = "Home to work, leaving at nine",
            lineLimits = TextFieldLineLimits.MultiLine(
                minHeightInLines = INPUT_MIN_LINES,
                maxHeightInLines = INPUT_MAX_LINES,
            ),
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { onEvent(AiSearchInputEvent.TypedTextChanged(it.toString())) },
        )

        AiLabelChipRow(stopLabels = stopLabels, onLabelClick = onLabelClick)

        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MicButton(onEvent = onEvent)
            Button(
                enabled = textFieldState.text.isNotBlank(),
                onClick = { onEvent(AiSearchInputEvent.Submit) },
            ) {
                Text(text = "Find trip")
            }
        }
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
        StopListeningButton(onClick = onStop)
    }
}

@Composable
private fun ThinkingStage(gradient: List<Color>, modifier: Modifier = Modifier) {
    StageColumn(modifier = modifier) {
        AiThinkingIndicator(active = true, colors = gradient)
        StageHeading(title = "Working it out", body = "")
    }
}
