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

private const val EARLIEST_THE_RECOGNISER_MAY_FINISH_MILLIS = 3_000L

// Both widened by roughly a second and a half after a rider reported being cut off while still
// thinking. The phrase window is the one that does it: a trip said out loud usually ends on
// something that sounds finished ("...on Monday morning"), and the recogniser treats a
// complete-sounding phrase as licence to close much sooner than an unfinished one.
//
// Erring long is the cheap direction here. A rider who has finished has a stop button in front
// of them and pays nothing for a generous window; a rider who was drawing breath loses their
// sentence and has to start again. The ceiling in AiSearchInputViewModel is what stops these
// from running forever.
private const val SILENCE_THAT_ENDS_THE_SESSION_MILLIS = 4_000L
private const val SILENCE_AFTER_A_COMPLETE_SOUNDING_PHRASE_MILLIS = 3_000L

/**
 * `android.speech.SpeechRecognizer`, not ML Kit — see this module's README for why.
 *
 * A plain singleton service has no `Activity` to show the `RECORD_AUDIO` system permission
 * dialog from, so unlike iOS's [IosSpeechToTextService] this class only *checks* permission
 * status — [SpeechToTextAvailability.Unavailable] with `reason = SpeechUnavailableReasons.PERMISSION_REQUIRED] tells
 * the caller to trigger the actual request via an `ActivityResultContracts.RequestPermission`
 * launcher at the Compose/Activity layer, then retry.
 */
internal class AndroidSpeechToTextService(private val context: Context) : SpeechToTextService {

    private var activeRecognizer: SpeechRecognizer? = null

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val result = when {
            !SpeechRecognizer.isRecognitionAvailable(context) ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.NOT_AVAILABLE)

            !hasRecordAudioPermission() ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.PERMISSION_REQUIRED)

            else -> SpeechToTextAvailability.Available
        }
        log("SpeechToTextService: checkAvailability -> $result")
        return result
    }

    override fun startListening(): Flow<SpeechToTextResult> = callbackFlow {
        if (!hasRecordAudioPermission()) {
            trySend(SpeechToTextResult.Error(reason = SpeechUnavailableReasons.PERMISSION_REQUIRED))
            close()
            return@callbackFlow
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechToTextResult.Error(reason = SpeechUnavailableReasons.NOT_AVAILABLE))
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
                            SpeechToTextResult.Error(reason = SpeechUnavailableReasons.NO_RESULT)
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
                // Without these the recogniser ends the session on the platform's own default
                // pause, which on several devices is short enough to cut a rider off mid
                // sentence ("from Central to..." <thinking> and it has already stopped). A
                // trip said out loud has thinking pauses in it, so the silence windows are
                // widened and a floor is set on how soon it may decide the rider is done.
                // These are hints: an OEM recogniser is free to ignore them, which is why
                // AiSearchInputViewModel also caps the session on its own side.
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    EARLIEST_THE_RECOGNISER_MAY_FINISH_MILLIS,
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    SILENCE_THAT_ENDS_THE_SESSION_MILLIS,
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    SILENCE_AFTER_A_COMPLETE_SOUNDING_PHRASE_MILLIS,
                )
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

// A blank candidate counts as no candidate, so a result of "" is reported as the session having
// no result rather than being written into the rider's field as an empty sentence. An OEM
// recogniser returning one is rare; iOS returns one routinely, and the two platforms answer the
// same shape the same way.
private fun Bundle.firstRecognitionCandidate(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
