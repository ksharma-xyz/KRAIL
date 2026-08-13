package xyz.ksharma.krail.core.speechtotext

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.ksharma.krail.core.log.log

/**
 * `android.speech.SpeechRecognizer`, not ML Kit — see this module's README for why.
 *
 * A plain singleton service has no `Activity` to show the `RECORD_AUDIO` system permission
 * dialog from, so unlike iOS's [IosSpeechToTextService] this class only *checks* permission
 * status — [SpeechToTextAvailability.Unavailable] with `reason = "permission_required"] tells
 * the caller to trigger the actual request via an `ActivityResultContracts.RequestPermission`
 * launcher at the Compose/Activity layer, then retry.
 */
internal class AndroidSpeechToTextService(private val context: Context) : SpeechToTextService {

    private var activeRecognizer: SpeechRecognizer? = null

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val result = when {
            !SpeechRecognizer.isRecognitionAvailable(context) ->
                SpeechToTextAvailability.Unavailable(reason = "not_available")

            !hasRecordAudioPermission() ->
                SpeechToTextAvailability.Unavailable(reason = "permission_required")

            else -> SpeechToTextAvailability.Available
        }
        log("SpeechToTextService: checkAvailability -> $result")
        return result
    }

    override fun startListening(): Flow<SpeechToTextResult> = callbackFlow {
        if (!hasRecordAudioPermission()) {
            trySend(SpeechToTextResult.Error(reason = "permission_required"))
            close()
            return@callbackFlow
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechToTextResult.Error(reason = "not_available"))
            close()
            return@callbackFlow
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        activeRecognizer = speechRecognizer

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    trySend(SpeechToTextResult.Error(reason = "recognizer_error_$error"))
                    close()
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.firstRecognitionCandidate()
                    trySend(
                        if (text != null) {
                            SpeechToTextResult.Final(text = text)
                        } else {
                            SpeechToTextResult.Error(reason = "no_result")
                        },
                    )
                    close()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.firstRecognitionCandidate()?.let { text ->
                        trySend(SpeechToTextResult.Partial(text = text))
                    }
                }
            },
        )

        speechRecognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            },
        )

        awaitClose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
            if (activeRecognizer === speechRecognizer) activeRecognizer = null
        }
    }

    override fun stopListening() {
        activeRecognizer?.stopListening()
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}

private fun Bundle.firstRecognitionCandidate(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
