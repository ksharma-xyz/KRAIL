package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

// Tall enough that the sheet does not resize between stages (the waveform stage is the
// tallest), so opening it, speaking and being answered is one steady surface rather than a
// panel that grows and shrinks under the rider's thumb.
private val SheetStageMinHeight = 280.dp

/**
 * The AI search surface: a sheet of its own, rather than an AI mode folded into the search
 * row's two text fields.
 *
 * It is an *input method*, not a destination. Whatever it resolves is written into the row's
 * ordinary From/To fields behind it and, when both ends are known, the rider lands on the
 * timetable they would have reached by filling those fields and tapping Search. Nothing
 * downstream of this sheet knows the AI exists.
 *
 * Content is split into [AiSearchSheetContent] because `ModalBottomSheet` renders through a
 * real `Dialog`/`Popup`, which the IDE's preview surface cannot show. Previews call the
 * content directly; the sheet function is what real usage calls.
 */
@Composable
fun AiSearchSheet(
    state: AiSearchInputUiState,
    stopLabels: ImmutableList<StopLabel>,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        AiSearchSheetContent(
            state = state,
            stopLabels = stopLabels,
            onEvent = onEvent,
            onLabelClick = onLabelClick,
        )
    }
}

@Composable
internal fun AiSearchSheetContent(
    state: AiSearchInputUiState,
    stopLabels: ImmutableList<StopLabel>,
    onEvent: (AiSearchInputEvent) -> Unit,
    onLabelClick: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val gradient = AiThemeGradientTokens.stopsFor(themeColorHex)

    // Typing is a mode the rider chooses, not something the pipeline knows about, so it lives
    // here rather than in the ViewModel. rememberSaveable so a rotation mid-sentence does not
    // drop them back to the speak prompt with their words still in the field.
    var isTyping by rememberSaveable { mutableStateOf(false) }
    val stage = state.stage(isTyping)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding)
            .padding(bottom = dim.spacingXL)
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(durationMillis = 220)) +
                        slideInVertically(
                            initialOffsetY = { it / 6 },
                            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                        )
                    ) togetherWith fadeOut(animationSpec = tween(durationMillis = 140))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = SheetStageMinHeight),
            label = "AiSheetStage",
        ) { currentStage ->
            AiSearchSheetStage(
                stage = currentStage,
                state = state,
                gradient = gradient,
                stopLabels = stopLabels,
                onEvent = onEvent,
                onLabelClick = onLabelClick,
                onTypingChange = { isTyping = it },
            )
        }
    }
}

internal enum class AiSheetStage { READY, TYPING, LISTENING, THINKING, DOWNLOADING, FAILED }

/**
 * Listening beats every other signal: the rider is mid-sentence and the surface has to show
 * that before anything else it might also be true of.
 */
private fun AiSearchInputUiState.stage(isTyping: Boolean): AiSheetStage = when {
    isListening -> AiSheetStage.LISTENING
    phase == AiSearchInputPhase.EXTRACTING -> AiSheetStage.THINKING
    phase == AiSearchInputPhase.DOWNLOADING -> AiSheetStage.DOWNLOADING
    phase == AiSearchInputPhase.UNRESOLVED -> AiSheetStage.FAILED
    isTyping -> AiSheetStage.TYPING
    else -> AiSheetStage.READY
}

// region Previews

private val previewLabels = persistentListOf(
    StopLabel(emoji = "🏠", label = "Home", stopId = "2000338", stopName = "Seven Hills Station"),
    StopLabel(emoji = "💼", label = "Work", stopId = "200060", stopName = "Town Hall Station"),
    StopLabel(emoji = "🎓", label = "Uni", stopId = "2006133", stopName = "Redfern Station"),
)

@PreviewComponent
@Composable
private fun PreviewAiSearchSheetReady() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiSearchSheetContent(
            state = AiSearchInputUiState(),
            stopLabels = previewLabels,
            onEvent = {},
            onLabelClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewAiSearchSheetListening() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink) {
        AiSearchSheetContent(
            state = AiSearchInputUiState(isListening = true, speechTranscript = "leaving home around nine"),
            stopLabels = previewLabels,
            onEvent = {},
            onLabelClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewAiSearchSheetFailed() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AiSearchSheetContent(
            state = AiSearchInputUiState(
                typedText = "the usual place",
                phase = AiSearchInputPhase.UNRESOLVED,
            ),
            stopLabels = previewLabels,
            onEvent = {},
            onLabelClick = {},
        )
    }
}

// endregion
