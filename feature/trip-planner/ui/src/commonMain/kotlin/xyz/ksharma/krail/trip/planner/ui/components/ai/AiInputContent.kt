package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

// Past this scale the wheel and its rings stop being drawn and the status becomes plain text.
// They carry no information the word does not, and at these sizes the field, the message and
// the button all need the room more than an animation does.
private const val FONT_SCALE_WHERE_DECORATION_COSTS_MORE_THAN_IT_SAYS = 1.5f

internal const val ACTIONS_STACK_SCALE = 1.8f

// The greeting leaves slower than anything arrives. A welcome that snaps off the screen the
// instant a key is pressed reads as an error; one that dissolves reads as making way.
private const val STATUS_FADE_IN_MILLIS = 220
private const val STATUS_FADE_OUT_MILLIS = 180
private const val GREETING_FADE_OUT_MILLIS = 420

// Tall enough for the greeting to run to two lines without the slot changing size, because the
// slot changing size is the one thing this layout must never do.
private const val SUGGESTION_PREFIX = "Try"

private val StatusSlotHeight = 140.dp
private val StatusSlotHeightCompact = 88.dp

/**
 * The AI input surface's content, with no opinion about what contains it.
 *
 * Two rules hold this layout together and everything else follows from them.
 *
 * **The field never moves.** One fixed height slot above it holds the greeting, or the wheel, or
 * nothing at all. Whichever is showing, the slot is the same height, so the field cannot be
 * nudged by a message changing length or by the rider starting to speak. The greeting leaves by
 * fading, never by collapsing.
 *
 * **The stage is anchored from the top, not centred.** Centring looks better on an empty screen
 * and breaks the first rule the moment a problem message runs to a second line: everything above
 * it would lift. Anchored, a growing message pushes only the button, downward, and the stage
 * scrolls once the column runs out of room, which is what actually happens at the largest font
 * scales with the keyboard open.
 */
@Composable
internal fun AiInputContent(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    suggestion: String,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val fontScale = LocalDensity.current.fontScale
    val workingBorder = rememberWorkingBorder(isWorking = state.isWorking)
    val showDecoration = fontScale < FONT_SCALE_WHERE_DECORATION_COSTS_MORE_THAN_IT_SAYS
    val stackActions = fontScale >= ACTIONS_STACK_SCALE
    // A latch, not a condition. The greeting is a welcome, and a welcome is said once: tying it
    // to "the field is empty" brought it back every time a rider cleared what they had typed to
    // start again, which is exactly the moment they least need to be greeted.
    //
    // rememberSaveable so a rotation does not resurrect it either. It resets when this surface
    // is next opened from scratch, which is the only point at which arriving is happening again.
    var riderHasStarted by rememberSaveable { mutableStateOf(false) }
    val startedNow = textFieldState.text.isNotEmpty() || state.isBusy
    LaunchedEffect(startedNow) {
        if (startedNow) riderHasStarted = true
    }
    val greetingVisible = !riderHasStarted && state.problemMessage() == null

    // The same signal the border runs on, so the wheel and the border start and finish
    // together. Keyed on the border because it already outlives the work by design: a model
    // that answers in 400ms otherwise flashed the wheel for a frame and was gone, which reads
    // as a glitch rather than as the app having thought about it.
    val busyVisible = state.isListening || workingBorder.spinning

    Column(modifier = modifier.fillMaxSize()) {
        // The greeting takes the empty space; the field and its controls live at the bottom
        // where the thumb is and where the keyboard leaves them. Anchoring the field to the
        // top left it stranded under the title bar with a screen of nothing between it and the
        // buttons that act on it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            AiStatusSlot(
                state = state,
                busyVisible = busyVisible,
                suggestion = suggestion,
                greetingVisible = greetingVisible,
                showDecoration = showDecoration,
                showTitle = showTitle,
                height = if (showDecoration) StatusSlotHeight else StatusSlotHeightCompact,
            )
        }

        // Above the bar, not below it. The bar is pinned to the bottom edge, so a message that
        // grows to three lines would otherwise shove it and its controls upward. Above, it
        // takes room from the empty space, which is what that space is for.
        state.problemMessage()?.let { AiProblemBanner(message = it) }

        Spacer(modifier = Modifier.height(dim.spacingM))

        AiInputBar(
            state = state,
            textFieldState = textFieldState,
            placeholder = AI_INPUT_PLACEHOLDER,
            onEvent = onEvent,
        )

        // Only past the largest font scales, where a glyph in a column of worded controls is
        // the odd one out and there is width to spare for the words. Below that the bar holds
        // both controls itself.
        if (stackActions) {
            Spacer(modifier = Modifier.height(dim.spacingM))
            AiVoiceControl(state = state, onEvent = onEvent, labelled = true)
        }
    }
}

/**
 * The one thing above the field that changes, in a box whose height never does.
 *
 * Everything that could appear here (a greeting and its example, a wheel with rings and a word,
 * or nothing) lives inside a fixed height, and the greeting leaves by fading rather than by
 * being removed. That is the whole mechanism: a rider watching this screen never sees the field
 * move under their thumb.
 */
@Composable
private fun AiStatusSlot(
    state: AiSearchInputUiState,
    busyVisible: Boolean,
    suggestion: String,
    greetingVisible: Boolean,
    showDecoration: Boolean,
    showTitle: Boolean,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Box(
        modifier = modifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.Center,
    ) {
        // Crossfaded, never swapped. Both live in the same fixed box, so the greeting leaving
        // and the wheel arriving are one dissolve rather than one thing vanishing and another
        // appearing where it was. The greeting also outlasts its own trigger slightly, which
        // is what stops it blinking out the instant a key is pressed.
        AnimatedVisibility(
            visible = busyVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = STATUS_FADE_IN_MILLIS)),
            exit = fadeOut(animationSpec = tween(durationMillis = STATUS_FADE_OUT_MILLIS)),
        ) {
            AiBusyStatus(state = state, showDecoration = showDecoration)
        }

        AnimatedVisibility(
            visible = greetingVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = STATUS_FADE_IN_MILLIS)),
            exit = fadeOut(animationSpec = tween(durationMillis = GREETING_FADE_OUT_MILLIS)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dim.spacingS),
            ) {
                if (showTitle) {
                    Text(text = AI_INPUT_QUESTION, style = KrailTheme.typography.titleMedium)
                }
                // Large and regular weight, not small and bold. This line is the content of an
                // otherwise empty screen, so size is what gives it presence; bold at this size
                // reads as a heading for something that is not there.
                //
                // Steps down a scale where the decoration is already gone, since at those font
                // scales 28sp is most of the screen before the rider has typed anything.
                // "Try" and quotes, because this is a demonstration rather than a prediction.
                // Without them the line read as the app guessing today's journey, which is
                // mildly useful when right and looks broken when wrong. Framed as an example it
                // does the job this screen actually needs doing: showing that a whole trip and
                // a time go in one sentence, in the rider's own words.
                Text(
                    text = "$SUGGESTION_PREFIX \u201C$suggestion\u201D",
                    style = if (showDecoration) {
                        KrailTheme.typography.bodyLarge
                    } else {
                        KrailTheme.typography.titleMediumRegular
                    },
                    color = KrailTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
