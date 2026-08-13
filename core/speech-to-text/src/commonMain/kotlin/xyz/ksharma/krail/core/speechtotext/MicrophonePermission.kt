package xyz.ksharma.krail.core.speechtotext

import androidx.compose.runtime.Composable

/**
 * Requests the platform's microphone permission, suspending until the user answers (or
 * resolving immediately, without a dialog, if already granted/handled).
 *
 * Android needs this as a separate Composable-layer step: [AndroidSpeechToTextService] is a
 * plain singleton with no `Activity` to show the system `RECORD_AUDIO` dialog from, so it
 * can only *check* permission status (see that class's own doc comment). This function is
 * the "trigger the actual request... at the Compose/Activity layer" half of that plan.
 *
 * iOS needs no equivalent step: [IosSpeechToTextService][xyz.ksharma.krail.core.speechtotext.IosSpeechToTextService]
 * already *requests* (not just inspects) both speech and microphone authorization directly
 * inside [xyz.ksharma.krail.core.speechtotext.SpeechToTextService.checkAvailability] itself,
 * per this project's "always request, never just check" permission rule - so the iOS
 * implementation of this function is a no-op passthrough.
 */
@Composable
expect fun rememberRequestRecordAudioPermission(): suspend () -> Boolean
