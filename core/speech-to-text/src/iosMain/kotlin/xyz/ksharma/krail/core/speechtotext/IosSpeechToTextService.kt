package xyz.ksharma.krail.core.speechtotext

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume

/**
 * `SFSpeechRecognizer` + `AVAudioEngine`, both plain Objective-C-compatible frameworks (no
 * Swift-only surface like Foundation Models), so this is a direct Kotlin/Native cinterop
 * call with no Swift shim needed — see `IosAiTextService` for the contrasting case.
 *
 * Deployment target is iOS 15.3 (see `iosApp` build settings), so this deliberately uses
 * the older `AVAudioSession.requestRecordPermission` (deprecated in favour of
 * `AVAudioApplication` on iOS 17+, but still functional) rather than branching on OS
 * version for a still-working API.
 *
 * Per this project's `ios_permission_must_request.md` lesson: [checkAvailability] actively
 * calls `requestAuthorization`/`requestRecordPermission` when status is not yet determined,
 * it does not just inspect `authorizationStatus()` — otherwise the app never appears in
 * Settings for the rider to grant the permission from.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosSpeechToTextService : SpeechToTextService {

    private val recognizer = SFSpeechRecognizer()
    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val speechAuthorized = requestSpeechAuthorizationIfNeeded()
        val micAuthorized = requestMicAuthorizationIfNeeded()
        val result = when {
            !speechAuthorized || !micAuthorized ->
                SpeechToTextAvailability.Unavailable(reason = "permission_denied")

            recognizer.available.not() ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.NOT_AVAILABLE)

            else -> SpeechToTextAvailability.Available
        }
        log("SpeechToTextService: checkAvailability -> $result")
        return result
    }

    override fun startListening(): Flow<SpeechToTextResult> = callbackFlow {
        if (recognizer.available.not()) {
            trySend(SpeechToTextResult.Error(reason = SpeechUnavailableReasons.NOT_AVAILABLE))
            close()
            return@callbackFlow
        }

        // `setActive:error:`/`setActive:withOptions:error:` exist in the ObjC header but
        // Kotlin/Native's importer does not expose either overload on AVAudioSession here
        // (confirmed: setCategory resolves fine, setActive is "Unresolved reference" for
        // every arity tried) — a cinterop overload-import gap, not a typo. Starting the
        // engine below activates the session as a side effect, so this is skipped rather
        // than worked around with a brittle guess.
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryRecord, error = null)

        val engine = AVAudioEngine().also { audioEngine = it }
        val request = SFSpeechAudioBufferRecognitionRequest().also { recognitionRequest = it }
        request.shouldReportPartialResults = true

        val inputNode = engine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 1024u,
            format = recordingFormat,
        ) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }

        engine.prepare()
        engine.startAndReturnError(null)

        recognizer.recognitionTaskWithRequest(request) { result, error ->
            when {
                error != null -> {
                    trySend(SpeechToTextResult.Error(reason = error.localizedDescription))
                    close()
                }

                result != null -> {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        trySend(SpeechToTextResult.Final(text = text))
                        close()
                    } else {
                        trySend(SpeechToTextResult.Partial(text = text))
                    }
                }
            }
        }

        awaitClose {
            engine.stop()
            inputNode.removeTapOnBus(0u)
            request.endAudio()
            audioEngine = null
            recognitionRequest = null
        }
    }

    override fun stopListening() {
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
    }

    private suspend fun requestSpeechAuthorizationIfNeeded(): Boolean {
        val current = SFSpeechRecognizer.authorizationStatus()
        if (current != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined) {
            return current == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
        }
        return suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { status ->
                continuation.resume(
                    status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized,
                )
            }
        }
    }

    private suspend fun requestMicAuthorizationIfNeeded(): Boolean =
        suspendCancellableCoroutine { continuation ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                continuation.resume(granted)
            }
        }
}
