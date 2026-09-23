package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.speechtotext.MicPermissionOutcome
import xyz.ksharma.krail.core.speechtotext.SpeechUnavailableReasons
import xyz.ksharma.krail.core.speechtotext.rememberOpenAppSettings
import xyz.ksharma.krail.core.speechtotext.rememberRequestMicrophonePermission
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.MIC_DENIED

/**
 * Asks for the microphone, then turns the answer into the one event it means.
 *
 * One path for every way a rider can start speaking: the surface opening, the mic in the bar,
 * and the button under a problem. The permission request has to be made from composition on
 * both platforms, so this is the seam, and the ViewModel only ever hears the outcome.
 */
@Composable
internal fun rememberStartListening(onEvent: (AiSearchInputEvent) -> Unit): () -> Unit {
    val requestMicPermission = rememberRequestMicrophonePermission()
    val coroutineScope = rememberCoroutineScope()
    return {
        coroutineScope.launch {
            onEvent(
                when (requestMicPermission()) {
                    MicPermissionOutcome.Granted -> AiSearchInputEvent.StartListening
                    MicPermissionOutcome.Denied -> AiSearchInputEvent.MicPermissionDenied
                    MicPermissionOutcome.NeedsSettings -> AiSearchInputEvent.MicPermissionBlocked
                    MicPermissionOutcome.Restricted -> AiSearchInputEvent.SpeechUnsupported
                },
            )
        }
    }
}

/**
 * Starts listening as the surface opens. The mic the rider tapped to get here was the request
 * to speak, and making them find a second mic inside the surface asked the same question twice.
 *
 * Keyed on the ViewModel's pending flag rather than on entering composition, so it runs once
 * per opening: a rotation after the rider has finished speaking recomposes this surface, and
 * starting the microphone again then would be taking it without being asked.
 */
@Composable
internal fun ListenOnOpenEffect(state: AiSearchInputUiState, onEvent: (AiSearchInputEvent) -> Unit) {
    val startListening = rememberStartListening(onEvent)
    LaunchedEffect(state.listenOnOpenPending) {
        if (state.listenOnOpenPending) startListening()
    }
}

/**
 * The action under a speech problem, when there is one the rider can take here.
 *
 * Ask KRAIL only listens, so none of these fall back to typing: a rider who wants to type has
 * the search screen. A refused microphone asks again, one the system will no longer ask about
 * goes to Settings, and anything else (a session that heard nothing, a recogniser error)
 * listens again.
 */
@Composable
internal fun AiSpeechProblemAction(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startListening = rememberStartListening(onEvent)
    val openAppSettings = rememberOpenAppSettings()
    val action: Pair<String, () -> Unit> = when {
        state.isListening || state.isSpeechUnsupported -> null
        state.needsSettingsForMic -> OPEN_SETTINGS_LABEL to openAppSettings
        state.needsMicPermission -> ALLOW_MIC_LABEL to startListening
        // Heard nothing, or any recogniser failure that is not a verdict on the phone. The mic
        // row is hidden while a speech problem shows, so this button is the only way to speak
        // again: an unrecognised error code with no action here left the rider stuck.
        state.speechUnavailableReason != null -> TRY_AGAIN_LABEL to startListening
        else -> null
    } ?: return

    Button(onClick = action.second, modifier = modifier.fillMaxWidth()) {
        Text(text = action.first)
    }
}

/** Refused, but the system will still ask. Android reports the same thing from the service. */
internal val AiSearchInputUiState.needsMicPermission: Boolean
    get() = speechUnavailableReason == MIC_DENIED ||
        speechUnavailableReason == SpeechUnavailableReasons.PERMISSION_REQUIRED

private const val ALLOW_MIC_LABEL = "Allow microphone"
private const val OPEN_SETTINGS_LABEL = "Open Settings"
private const val TRY_AGAIN_LABEL = "Try again"
