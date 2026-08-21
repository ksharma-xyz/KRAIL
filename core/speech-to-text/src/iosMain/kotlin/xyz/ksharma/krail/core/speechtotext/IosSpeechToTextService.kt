package xyz.ksharma.krail.core.speechtotext

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
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
 * Microphone permission is owned by aagya at the Compose layer
 * ([rememberRequestMicrophonePermission]), so this service only reads the current status,
 * mirroring [AndroidSpeechToTextService] which likewise never shows the system dialog
 * itself. `AVAudioApplication.recordPermission` is an iOS 17+ API, which the deployment
 * target satisfies (see `iosApp` build settings).
 *
 * Speech-recognizer authorization has no aagya family, so it is still requested here. Per
 * this project's `ios_permission_must_request.md` lesson: [checkAvailability] actively
 * calls `SFSpeechRecognizer.requestAuthorization` when status is not yet determined, it
 * does not just inspect `authorizationStatus()` — otherwise the app never appears in
 * Settings for the rider to grant the permission from.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosSpeechToTextService : SpeechToTextService {

    private val recognizer = SFSpeechRecognizer()
    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val speechAuthorized = requestSpeechAuthorizationIfNeeded()
        val micAuthorized = microphoneAuthorized()
        val result = when {
            !speechAuthorized || !micAuthorized ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.PERMISSION_REQUIRED)

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

    // Status read only. By the time this runs, aagya's controller has already raised the
    // system dialog from the mic button (see rememberRequestMicrophonePermission), and only
    // a granted answer starts listening. Anything but granted here means a caller skipped
    // that flow, and the answer is the same one AndroidSpeechToTextService gives: report
    // PERMISSION_REQUIRED rather than ask a second time from a layer that should not own it.
    private fun microphoneAuthorized(): Boolean =
        AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
}
