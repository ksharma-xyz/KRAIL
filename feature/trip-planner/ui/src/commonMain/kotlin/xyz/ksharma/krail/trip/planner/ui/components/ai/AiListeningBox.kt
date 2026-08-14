package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiListeningIndicator
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * What the field becomes while the microphone is live: the same box, the same corner radius,
 * the same minimum height, with the waveform where the sentence was and the words underneath
 * as they are heard.
 *
 * The state slot stays exactly where it was, holding stop instead of the microphone, so the
 * control the rider just pressed does not move out from under their thumb.
 */
@Composable
internal fun AiListeningBox(
    state: AiSearchInputUiState,
    showSlot: Boolean,
    minHeight: Dp,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(
                color = KrailTheme.colors.surface,
                shape = RoundedCornerShape(dim.radiusL),
            )
            .padding(dim.spacingM),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            AiListeningIndicator(
                active = true,
                colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                // The waveform is the whole signal here. A spinning mark above it says the
                // same word twice and adds height the text field does not have.
                showMark = false,
            )
            if (state.speechTranscript.isNotBlank()) {
                Text(
                    text = state.speechTranscript,
                    style = KrailTheme.typography.bodyMedium,
                    color = KrailTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (showSlot) {
            AiStateSlot(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
