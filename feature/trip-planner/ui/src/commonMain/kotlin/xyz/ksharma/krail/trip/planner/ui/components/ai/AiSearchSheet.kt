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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

// Tall enough that the sheet does not resize between stages (the listening waveform is the
// tallest), so opening it, speaking and being answered is one steady surface rather than a
// panel that grows and shrinks under the rider's thumb.
private val SheetStageMinHeight = 380.dp

// The sheet's own background is the theme colour laid over the page surface at low strength.
// A plain surface put a white field on an almost-white sheet with no edge between them; a
// tint keeps the field reading as a field without touching the field's own colours.
private const val SHEET_TINT_ALPHA = 0.14f

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
        containerColor = themeColor().copy(alpha = SHEET_TINT_ALPHA)
            .compositeOver(KrailTheme.colors.surface),
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
    val stage = state.stage()

    // One field, owned above the stages, so a transcript that came in by voice lands in the
    // same box the rider would have typed into and can be corrected there rather than
    // re-spoken. Only pushed in when the two actually differ: writing on every recomposition
    // would fight the rider's own cursor.
    val textFieldState = rememberTextFieldState()
    LaunchedEffect(state.typedText) {
        if (state.typedText != textFieldState.text.toString()) {
            textFieldState.setTextAndPlaceCursorAtEnd(state.typedText)
        }
    }

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
                textFieldState = textFieldState,
                onEvent = onEvent,
                onLabelClick = onLabelClick,
            )
        }
    }
}

/**
 * Three stages, not one per phase. Typing is not a mode to be switched into: the box is
 * always there, and a rider who does not want to talk simply does not tap the microphone.
 * Failure and a model still downloading are lines of text above that same box rather than
 * stages of their own, because both of them end with the rider trying again in it.
 */
internal enum class AiSheetStage { INPUT, LISTENING, THINKING }

private fun AiSearchInputUiState.stage(): AiSheetStage = when {
    isListening -> AiSheetStage.LISTENING
    phase == AiSearchInputPhase.EXTRACTING -> AiSheetStage.THINKING
    else -> AiSheetStage.INPUT
}

// region Previews

private val previewLabels = persistentListOf(
    StopLabel(emoji = "🏠", label = "Home", stopId = "2000338", stopName = "Seven Hills Station"),
    StopLabel(emoji = "💼", label = "Work", stopId = "200060", stopName = "Town Hall Station"),
    StopLabel(emoji = "🎓", label = "Uni", stopId = "2006133", stopName = "Redfern Station"),
)

@PreviewComponent
@Composable
private fun PreviewAiSearchSheetInput() {
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
private fun PreviewAiSearchSheetUnresolved() {
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
