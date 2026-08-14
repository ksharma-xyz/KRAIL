package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.speechtotext.rememberRequestRecordAudioPermission
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.MicIcon
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.StopIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

/**
 * The parts every stage of [AiSearchSheetContent] shares. They live here rather than beside
 * the stages so that adding a stage does not push that file past its function limit.
 */
@Composable
internal fun StageColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingL, Alignment.CenterVertically),
    ) {
        content()
    }
}

@Composable
internal fun StageHeading(title: String, body: String) {
    val dim = KrailTheme.dimensions
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingS),
    ) {
        Text(
            text = title,
            style = KrailTheme.typography.titleLarge,
            color = KrailTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A line above the box, never a stage of its own: every one of these ends in the same box. */
@Composable
internal fun StageProblem(message: String) {
    Text(
        text = message,
        style = KrailTheme.typography.bodySmall,
        color = KrailTheme.colors.softLabel,
        textAlign = TextAlign.Center,
    )
}

/**
 * A permission problem, a recogniser problem, a model still downloading and a sentence that
 * resolved to nothing are four different problems. Telling a rider who has already granted the
 * microphone that they need to grant it was the bug that made the previous version of this
 * untrustworthy, so they are kept apart rather than collapsed into one apology.
 */
internal fun AiSearchInputUiState.problemMessage(): String? = when {
    phase == AiSearchInputPhase.DOWNLOADING ->
        "KRAIL is still downloading the on device model. Try again in a moment."
    phase == AiSearchInputPhase.UNRESOLVED ->
        "I could not place that. Try a stop name, or pick one of your places."
    speechUnavailableReason == null -> null
    speechUnavailableReason.isPermissionProblem() ->
        "KRAIL needs microphone access to listen. You can still type."
    else -> "The microphone is not available right now. You can still type."
}

private fun String.isPermissionProblem(): Boolean =
    this == "permission_required" || this == "no_permission"

/**
 * Speaking is behind an explicit tap rather than the sheet opening straight into a live
 * microphone: taking the mic before the rider has said they want to speak is not something
 * that can be undone quietly, and the permission prompt reads as unprovoked when it appears
 * on open rather than on a button they pressed.
 */
@Composable
internal fun MicButton(onEvent: (AiSearchInputEvent) -> Unit) {
    val dim = KrailTheme.dimensions
    val requestMicPermission = rememberRequestRecordAudioPermission()
    val coroutineScope = rememberCoroutineScope()

    RoundIconButton(
        content = {
            Image(
                imageVector = MicIcon,
                contentDescription = "Speak",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
        onClick = {
            coroutineScope.launch {
                // A denial has to be said out loud. Starting the recogniser anyway just fails
                // silently and the rider watches the sheet do nothing.
                val granted = requestMicPermission()
                onEvent(
                    if (granted) {
                        AiSearchInputEvent.StartListening
                    } else {
                        AiSearchInputEvent.MicPermissionDenied
                    },
                )
            }
        },
    )
}

@Composable
internal fun StopListeningButton(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions

    RoundIconButton(
        content = {
            Image(
                imageVector = StopIcon,
                contentDescription = "Stop listening",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
        onClick = onClick,
    )
}
