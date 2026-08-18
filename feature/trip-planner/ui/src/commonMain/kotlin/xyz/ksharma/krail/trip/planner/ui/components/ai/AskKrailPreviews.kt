package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.UnresolvedReason

// Previews cover the dialog's static states only. The busy state runs the wheel's infinite
// spin, which a snapshot recording waits on until it times out (see DiscoverChip's disabled
// screenshot tests for the same trap), so Thinking/Listening stay device-verified.

/**
 * The card body, without the Dialog window: a ModalBottomSheet-style window cannot render on
 * the preview surface, so the content is previewed directly, the same split every sheet in
 * this repo uses.
 */
@Composable
private fun AskKrailDialogPreviewSurface(
    state: AiSearchInputUiState,
    typed: String = "",
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = Modifier
            .background(KrailTheme.colors.surface)
            .padding(dim.spacingXL),
    ) {
        AiDialogContent(
            state = state,
            textFieldState = rememberTextFieldState(initialText = typed),
            suggestion = "Home to Work by 9am",
            busyVisible = false,
            onEvent = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAskKrailDialogIdle() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AskKrailDialogPreviewSurface(state = AiSearchInputUiState(isInputOpen = true))
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAskKrailDialogFoundIt() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AskKrailDialogPreviewSurface(
            state = AiSearchInputUiState(
                isInputOpen = true,
                phase = AiSearchInputPhase.RESOLVED,
            ),
            typed = "seven hills to central by 9am",
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAskKrailDialogStopNotFound() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        AskKrailDialogPreviewSurface(
            state = AiSearchInputUiState(
                isInputOpen = true,
                phase = AiSearchInputPhase.UNRESOLVED,
                unresolvedReason = UnresolvedReason.STOP_NOT_FOUND,
                unmatchedPlace = "Hogwarts",
                typedText = "get me to hogwarts by 9",
            ),
            typed = "get me to hogwarts by 9",
        )
    }
}
