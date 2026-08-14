package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.modifier.aiGradientBorder
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

private const val MIC_LEAVES_FIELD_SCALE = 1.3f

/**
 * Font scale at which the row becomes a column.
 *
 * Past this, a mic, a Cancel and a Find trip cannot sit on one line without either wrapping
 * mid-label or shrinking below a 44dp target. Neither is acceptable, so the arrangement gives
 * way instead: one full width control per row, ordered by how likely it is to be used.
 */
internal const val ACTIONS_STACK_SCALE = 1.8f

// This button fills the two fields on the screen behind and closes. It does not load a
// timetable: the rider still decides when to do that. "Find trip" promised results they then
// had to ask for a second time.
private const val PRIMARY_ACTION = "Continue"

/**
 * The AI input surface's content, with no opinion about what contains it. `AiInputSurface`
 * wraps this in a screen on a phone and a dialog on a tablet; both hand it the same state.
 *
 * Everything that adapts, adapts to the rider's font scale rather than to the window: a phone
 * at 2x and a tablet at 2x have the same problem, which is that text and touch targets need
 * room the layout has to give back.
 */
@Composable
internal fun AiInputContent(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Full screen pushes the actions to the bottom edge, where they sit directly above the
    // keyboard and land under the thumb. A dialog packs to its content instead: a card with a
    // stretch of empty space in the middle of it looks broken rather than roomy.
    actionsAtBottom: Boolean = false,
    // Full screen has the question in its title bar already; the dialog has no bar to put it
    // in, so it draws its own.
    showTitle: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val fontScale = LocalDensity.current.fontScale
    // A 44dp control inside the field costs every line of text the same 60dp of width. At
    // default size that is about three characters; by MIC_LEAVES_FIELD_SCALE the text is large
    // enough that those characters matter and the control is large enough to crowd them, so it
    // moves out to the action row. Past ACTIONS_STACK_SCALE that row cannot hold three controls
    // on one line without wrapping a label or shrinking a touch target, so it becomes a column.
    val workingBorder = rememberWorkingBorder(isWorking = state.isWorking)
    val micInField = fontScale < MIC_LEAVES_FIELD_SCALE
    val stackActions = fontScale >= ACTIONS_STACK_SCALE

    // The line and the field are one group and sit close together; the actions are a separate
    // group and get the wider gap. A single uniform gap made all three read as unrelated.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        AiInputHeader(state = state, showTitle = showTitle)

        // The waveform takes the field's own box rather than appearing above or below it, so
        // starting and stopping the microphone never changes the surface's height.
        if (state.isListening) {
            AiListeningBox(
                state = state,
                showSlot = micInField,
                minHeight = TextFieldDefaults.MultiLineMinHeight,
                onEvent = onEvent,
            )
        } else {
            TextField(
                state = textFieldState,
                // Named stops and a real time, prefixed so it reads as an example rather than
                // as something already typed. "Home to work, leaving at nine" showed the shape
                // of a sentence while teaching nothing about what this understands.
                placeholder = "Try: Town Hall to Bondi Junction by 6pm",
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 6),
                imeAction = ImeAction.Search,
                onSubmit = { onEvent(AiSearchInputEvent.Submit) },
                enabled = !state.isBusy,
                trailingIcon = if (micInField) {
                    { AiStateSlot(state = state, onEvent = onEvent) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TextFieldDefaults.MultiLineMinHeight)
                    // Working puts the app's own rotating gradient around the field, the same
                    // treatment the alert summary card uses while it thinks. A dimmed sentence
                    // and a 24dp wheel in the corner were too quiet to read as working at all.
                    .aiGradientBorder(
                        spinning = workingBorder.spinning,
                        cornerRadius = dim.radiusL,
                        colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                        alpha = workingBorder.alpha,
                    )
                    // Dimmed rather than hidden: the rider can still read what is being worked
                    // out, and nothing is added or removed to say so.
                    .graphicsLayer { alpha = state.workingTextAlpha },
                onTextChange = { onEvent(AiSearchInputEvent.TypedTextChanged(it.toString())) },
            )
        }

        state.problemMessage()?.let { StageProblem(message = it) }

        if (actionsAtBottom) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(dim.spacingM))
        }

        AiInputActions(
            state = state,
            hasText = textFieldState.text.isNotBlank(),
            showMic = !micInField,
            stacked = stackActions,
            // The full screen already has a back arrow and the system back gesture. A Cancel
            // beside them is a third way out of the same screen. The dialog has neither, so it
            // keeps one.
            showCancel = showTitle,
            onEvent = onEvent,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun AiInputActions(
    state: AiSearchInputUiState,
    hasText: Boolean,
    showMic: Boolean,
    stacked: Boolean,
    showCancel: Boolean,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val dim = KrailTheme.dimensions

    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            // Labelled rather than an icon: at this scale a bare glyph in a row of worded
            // buttons is the odd one out, and there is width to spare for the word.
            AiSpeakButton(state = state, onEvent = onEvent, labelled = true)
            Button(
                enabled = hasText && !state.isBusy,
                onClick = { onEvent(AiSearchInputEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = PRIMARY_ACTION)
            }
            if (showCancel) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Cancel")
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showMic) {
            AiSpeakButton(state = state, onEvent = onEvent, labelled = false)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingL, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCancel) {
                TextButton(onClick = onDismiss) { Text(text = "Cancel") }
            }
            Button(
                enabled = hasText && !state.isBusy,
                onClick = { onEvent(AiSearchInputEvent.Submit) },
            ) {
                Text(text = PRIMARY_ACTION)
            }
        }
    }
}
