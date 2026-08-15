package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.speechtotext.MicPermissionOutcome
import xyz.ksharma.krail.core.speechtotext.SpeechUnavailableReasons
import xyz.ksharma.krail.core.speechtotext.rememberOpenAppSettings
import xyz.ksharma.krail.core.speechtotext.rememberRequestMicrophonePermission
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.MicIcon
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.StopIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.MIC_NEEDS_SETTINGS
import xyz.ksharma.krail.trip.planner.ui.search.ai.MIC_UNSUPPORTED
import xyz.ksharma.krail.trip.planner.ui.search.ai.UnresolvedReason

private const val WORKING_TEXT_ALPHA = 0.55f

/**
 * The question lives in the title bar, so all this carries is the one line under it, and that
 * line is the only thing in the header that changes with state. Nothing is added or removed
 * between states: the words change inside a block that is always exactly one line tall.
 *
 * The dialog has no title bar of its own, so it asks for [showTitle] and gets the question
 * back at the top of the card.
 */
@Composable
internal fun AiInputHeader(
    state: AiSearchInputUiState,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        if (showTitle) {
            Text(
                text = AI_INPUT_QUESTION,
                style = KrailTheme.typography.titleLarge,
                color = KrailTheme.colors.onSurface,
            )
        }
        Text(
            text = state.descriptionLine(),
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.secondaryLabel,
        )
    }
}

internal const val AI_INPUT_QUESTION = "Where are you going?"

private fun AiSearchInputUiState.descriptionLine(): String = when {
    isListening -> "Go ahead, I am listening."
    phase == AiSearchInputPhase.EXTRACTING -> "Working it out."
    // What the rider needs to know is that plain words are enough and that there are two ways
    // in. "Say it the way you would to a friend" was a nice line that told them neither.
    else -> "Plain words are fine. Type it, or tap the mic."
}

/**
 * One control, three jobs. Idle it is a microphone, listening it is stop, working it is the
 * wheel turning. It never moves, never resizes, and nothing else is added to say the same
 * thing: a spinner beside a spinning mark would be two indicators for one state.
 */
@Composable
internal fun AiStateSlot(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current

    AnimatedContent(
        targetState = state.slotContent(),
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 150))
        },
        modifier = modifier,
        label = "AiStateSlot",
    ) { slot ->
        when (slot) {
            AiSlotContent.MIC -> AiSpeakButton(state = state, onEvent = onEvent, labelled = false)

            AiSlotContent.STOP -> RoundIconButton(
                onClick = { onEvent(AiSearchInputEvent.StopListening) },
                content = {
                    Image(
                        imageVector = StopIcon,
                        contentDescription = "Stop listening",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(dim.iconDefault),
                    )
                },
            )

            AiSlotContent.WHEEL -> RoundIconButton(
                enabled = false,
                onClick = {},
                content = {
                    AiWheelMark(
                        spinning = true,
                        colors = AiThemeGradientTokens.stopsFor(themeColorHex),
                        markSize = dim.iconDefault,
                    )
                },
            )
        }
    }
}

internal enum class AiSlotContent { MIC, STOP, WHEEL }

private fun AiSearchInputUiState.slotContent(): AiSlotContent = when {
    isListening -> AiSlotContent.STOP
    phase == AiSearchInputPhase.EXTRACTING -> AiSlotContent.WHEEL
    else -> AiSlotContent.MIC
}

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
internal fun AiSpeakButton(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    labelled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val requestMicPermission = rememberRequestMicrophonePermission()
    val openAppSettings = rememberOpenAppSettings()
    val coroutineScope = rememberCoroutineScope()
    val enabled = !state.isBusy && !state.isSpeechUnsupported

    val onClick: () -> Unit = {
        if (state.needsSettingsForMic) {
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
        content = {
            Image(
                imageVector = MicIcon,
                contentDescription = "Speak",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.iconDefault),
            )
        },
    )
}

/** A line above the actions, never a state of its own: all of these end in the same field. */
@Composable
internal fun StageProblem(message: String) {
    Text(
        text = message,
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.secondaryLabel,
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
    UnresolvedReason.STOP_NOT_FOUND ->
        unmatchedPlace
            ?.let { place -> "No stop called \"$place\". Try the name as it appears on the sign." }
            ?: "That is not a stop I know. Try the name as it appears on the sign."

    // The model, not the sentence. Nothing about the wording is worth changing.
    UnresolvedReason.COULD_NOT_READ ->
        "That did not come through. Have another go, or use fewer words."

    null -> "Something is missing there. Name where you are going, and where from."
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

/** Working state dims the sentence rather than replacing it, so the words stay readable. */
internal val AiSearchInputUiState.workingTextAlpha: Float
    get() = if (phase == AiSearchInputPhase.EXTRACTING) WORKING_TEXT_ALPHA else 1f
