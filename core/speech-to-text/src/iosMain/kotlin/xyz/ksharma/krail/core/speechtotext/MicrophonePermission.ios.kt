package xyz.ksharma.krail.core.speechtotext

import androidx.compose.runtime.Composable

// IosSpeechToTextService already requests (not just checks) both speech and microphone
// authorization inline as part of checkAvailability()/startListening(), so there is no
// separate Activity/UIViewController-layer step to perform here.
@Composable
actual fun rememberRequestRecordAudioPermission(): suspend () -> Boolean = { true }
