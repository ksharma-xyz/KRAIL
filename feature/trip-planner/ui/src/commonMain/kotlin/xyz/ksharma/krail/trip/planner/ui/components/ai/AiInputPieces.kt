package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.speechtotext.MicPermissionOutcome
import xyz.ksharma.krail.core.speechtotext.SpeechUnavailableReasons
import xyz.ksharma.krail.core.speechtotext.rememberOpenAppSettings
import xyz.ksharma.krail.core.speechtotext.rememberRequestMicrophonePermission
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiActivity
import xyz.ksharma.krail.taj.components.AiListeningIndicator
import xyz.ksharma.krail.taj.components.AlertIcon
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.MicIcon
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.SendIcon
import xyz.ksharma.krail.taj.components.StopIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.MIC_NEEDS_SETTINGS
import xyz.ksharma.krail.trip.planner.ui.search.ai.MIC_UNSUPPORTED
import xyz.ksharma.krail.trip.planner.ui.search.ai.UnresolvedReason
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelSynonyms

/**
 * What the slot shows while the surface is busy. Two states, two motions, one word each.
 *
 * Past the largest font scales the wheel and rings are not drawn at all: the word carries the
 * same information in a fraction of the height, and at those sizes height is what the field and
 * the message are short of.
 */
@Composable
internal fun AiBusyStatus(
    state: AiSearchInputUiState,
    showDecoration: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val listening = state.isListening

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingS),
    ) {
        if (showDecoration) {
            AiListeningIndicator(
                active = true,
                activity = if (listening) AiActivity.Listening else AiActivity.Working,
                colors = AiThemeGradientTokens.stopsFor(themeColorHex),
            )
        }
        Text(
            text = if (listening) LISTENING_WORD else WORKING_WORD,
            // Same weight whether or not the wheel is beside it. It is the only word on an
            // otherwise empty half of the screen and was reading as a caption.
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.onSurface,
        )
    }
}

private const val LISTENING_WORD = "Listening"

// Same word as the dialog's status line. "Working it out" read as labouring; thinking is
// what the surface is doing.
private const val WORKING_WORD = "Thinking…"

// Kept for the dialog, which has no title bar of its own to be dismissed from and so still
// names itself.
internal const val AI_INPUT_QUESTION = "Ask KRAIL"

// The full screen has no title, so this is the only instruction on it. "Ask KRAIL" named the
// feature without telling anybody what to do with it; a rider who has just opened this has no
// idea it takes a time as well as two stops, which is the only thing it does that the search
// row does not. So the placeholder asks for both.
internal const val AI_INPUT_PLACEHOLDER = "Where to, and when?"

/**
 * Speaking is behind an explicit tap rather than the surface opening into a live microphone:
 * taking the mic before the rider has said they want to speak is not something that can be
 * undone quietly.
 *
 * A refusal is answered, not swallowed. The first tap raises the system prompt; once the
 * system has stopped asking, the same button opens this app's settings page instead, so the
 * button never silently does nothing.
 */
@Composable
internal fun AiVoiceControl(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    labelled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val requestMicPermission = rememberRequestMicrophonePermission()
    val openAppSettings = rememberOpenAppSettings()
    val coroutineScope = rememberCoroutineScope()
    // Listening counts as busy everywhere else, but this control is how a rider stops.
    val enabled = (state.isListening || !state.isBusy) && !state.isSpeechUnsupported

    val onClick: () -> Unit = {
        if (state.isListening) {
            // The same control, so stopping is where starting was rather than somewhere new.
            onEvent(AiSearchInputEvent.StopListening)
        } else if (state.needsSettingsForMic) {
            // The system will not prompt again, so the mic's job changes rather than the
            // button doing nothing a second time.
            openAppSettings()
        } else {
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

    if (labelled) {
        Button(enabled = enabled, onClick = onClick, modifier = modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = MicIcon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier.size(dim.iconDefault),
                )
                Text(text = "Speak")
            }
        }
        return
    }

    RoundIconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        // Same round button and same diameter as send, so the pair reads as two controls of
        // equal standing. Only the fill differs: this one is an option, send finishes the job.
        content = {
            Image(
                imageVector = if (state.isListening) StopIcon else MicIcon,
                contentDescription = if (state.isListening) "Stop listening" else "Speak",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
    )
}

/**
 * A problem gets its own colour and its own container, sitting above the input bar.
 *
 * Uses the theme's own error container pair rather than the error colour at low alpha. A
 * translucent fill takes its contrast from whatever happens to be behind it, and behind this is
 * a gradient that changes down the screen, so the same message was readable in one place and
 * muddy in another. In dark mode it came out grey with pink text; in light mode, lavender with
 * dark red. The container pair is designed for exactly this and is already tuned for both.
 */
@Composable
internal fun AiProblemBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = KrailTheme.colors.errorContainer,
                shape = RoundedCornerShape(dim.radiusM),
            )
            .padding(horizontal = dim.spacingL, vertical = dim.spacingL),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        // Top aligned, not centred. The message runs to three lines often enough that a centred
        // icon drifts into the middle of a paragraph and reads as punctuation rather than as a
        // marker for the whole thing.
        Image(
            imageVector = AlertIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.onErrorContainer),
            modifier = Modifier
                .padding(top = dim.spacingXXS)
                .size(dim.iconSmall),
        )

        // Smaller than the rider's own sentence. This is the app talking, and it should not
        // compete for weight with the words they wrote.
        Text(
            text = message,
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onErrorContainer,
        )
    }
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
    phase == AiSearchInputPhase.UNRESOLVED -> unresolvedMessage()
    speechUnavailableReason == null -> null
    needsSettingsForMic ->
        "Microphone is off for KRAIL. Tap the mic again to open Settings."
    isSpeechUnsupported ->
        "Speaking is not available on this device. You can still type."
    else -> "KRAIL needs the microphone to hear you. You can still type."
}

/**
 * Written here rather than asked of the model. A model that has just failed to understand a
 * sentence is the worst possible narrator of that failure, it would cost a second on-device
 * call right after the rider already waited, and the case where the model is unavailable still
 * needs words. What makes these better than one catch-all line is that each names the thing the
 * rider can act on.
 */
private fun AiSearchInputUiState.unresolvedMessage(): String = when (unresolvedReason) {
    // The sentence read fine and had no place in it. An example is more use than an apology.
    UnresolvedReason.NO_PLACE_MENTIONED ->
        "No place in that one. Name a stop, like Central Station, and where you are going."

    // We know exactly which word failed, so it is quoted back rather than described.
    //
    // A label word gets different advice, because "try the name as it appears on the sign" is
    // useless for it: there is no sign that says Work. The rider does not want a stop called
    // work, they want their Work stop, and the thing they can act on is setting it.
    UnresolvedReason.STOP_NOT_FOUND ->
        unmatchedPlace?.let { place ->
            if (LabelSynonyms.isLabelWord(place)) {
                "No stop saved as \"$place\" yet. Search for the stop, save it under that " +
                    "name, and you can say it here."
            } else {
                "No stop called \"$place\". Try the name as it appears on the sign."
            }
        } ?: "That is not a stop I know. Try the name as it appears on the sign."

    // The model, not the sentence. Nothing about the wording is worth changing.
    UnresolvedReason.COULD_NOT_READ ->
        "That did not come through. Have another go, or use fewer words."

    // Neither the sentence nor anything the rider can do. Saying so and pointing at the way
    // that does work beats an apology, and beats the advice this used to give, which was to
    // reword a sentence nothing had read.
    UnresolvedReason.MODEL_UNAVAILABLE ->
        "This phone cannot run the on device AI that Ask KRAIL needs. Tap the stops to plan a " +
            "trip instead."

    // The one model problem with a fix the rider owns, so it names the switch.
    UnresolvedReason.MODEL_NEEDS_SETTING ->
        "Ask KRAIL needs Apple Intelligence turned on. Switch it on in Settings, then try again."

    null -> "Something is missing there. Name where you are going, and where from."
}

/**
 * The unmatched place is a word the rider names places with, and they have not set it yet.
 *
 * Distinct from an ordinary unknown stop, because the fix is different: there is no sign that
 * says Work, and telling them to check one is advice they cannot follow.
 */
internal val AiSearchInputUiState.missingStopLabel: String?
    get() = unmatchedPlace?.takeIf {
        unresolvedReason == UnresolvedReason.STOP_NOT_FOUND && LabelSynonyms.isLabelWord(it)
    }

internal val AiSearchInputUiState.needsSettingsForMic: Boolean
    get() = speechUnavailableReason == MIC_NEEDS_SETTINGS

internal val AiSearchInputUiState.isSpeechUnsupported: Boolean
    get() = speechUnavailableReason == MIC_UNSUPPORTED ||
        speechUnavailableReason == SpeechUnavailableReasons.NOT_AVAILABLE

internal val AiSearchInputUiState.isWorking: Boolean
    get() = phase == AiSearchInputPhase.EXTRACTING

internal val AiSearchInputUiState.isBusy: Boolean
    get() = phase == AiSearchInputPhase.EXTRACTING || isListening

/**
 * Send. Filled with the theme colour, so it is the one thing on the row that reads as the end
 * of the task rather than another option.
 *
 * A glyph rather than a word, because it sits beside a mic and two worded buttons in a row make
 * the rider read to find out which one finishes. It only ever exists when there is something to
 * send, so it never has to explain itself as disabled.
 */
@Composable
internal fun AiSendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    RoundIconButton(
        enabled = enabled,
        onClick = onClick,
        // Drawn at 90%, still measured at 100%. Modifier.scale is a draw-time transform, so
        // the touch target keeps the full round-button diameter while the fill reads slightly
        // lighter next to the mic. Shrinking the layout instead would have taken the tap
        // target below the minimum with it.
        modifier = modifier.scale(SEND_BUTTON_DRAW_SCALE),
        color = themeColorHex.hexToComposeColor(),
        content = {
            Image(
                imageVector = SendIcon,
                contentDescription = "Continue",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
    )
}

private const val SEND_BUTTON_DRAW_SCALE = 0.9f
