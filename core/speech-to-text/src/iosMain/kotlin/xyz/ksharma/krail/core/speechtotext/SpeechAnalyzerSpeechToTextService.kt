package xyz.ksharma.krail.core.speechtotext

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import speechBridge.SpeechBridge
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume

/**
 * iOS 26's `SpeechAnalyzer`, via the `@objc` Swift shim in `src/swift/speechBridge/`.
 *
 * Apple replaced `SFSpeechRecognizer` with a modular analyzer, and two of its modules are
 * exactly the jobs [IosSpeechToTextService] had to do by hand:
 *
 * - `DictationTranscriber` is the documented migration path for short queries, on the same
 *   on-device model `SFSpeechRecognizer` uses. A rider saying where they are going is a short
 *   query, not the long-form audio `SpeechTranscriber` is built for.
 * - `SpeechDetector` is Apple's voice activity detection, reported in **audio time**. The
 *   legacy path had nothing like it and had to infer the rider from how text happened to
 *   arrive, which is where every fault in
 *   `docs/learning/2026-08-23-the-blank-transcript-that-cleared-the-field.md` came from.
 *
 * What is left here is small on purpose: the analyzer owns the microphone and the session,
 * [SpeechActivityWatch] owns the one decision, and this class is the plumbing between them and
 * a Flow. There is no silence-polling coroutine any more, because there is nothing to poll: the
 * detector reports, and each report is enough to decide on.
 *
 * Ending is still ours to ask for. Apple is explicit that terminating the input sequence does
 * not finish a session, so the bridge's `finish()` does both, in the order that matters.
 */
@OptIn(ExperimentalForeignApi::class)
internal class SpeechAnalyzerSpeechToTextService : SpeechToTextService {

    private val bridge = SpeechBridge()

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        // Microphone only. SpeechAnalyzer needs no speech-recognition authorization of its own,
        // unlike SFSpeechRecognizer, so this path shows the rider one system prompt where the
        // legacy one showed two. Permission is still asked for at the Compose layer; this only
        // reads the answer, same contract as every other service here.
        if (!microphoneAuthorized()) {
            return SpeechToTextAvailability.Unavailable(
                reason = SpeechUnavailableReasons.PERMISSION_REQUIRED,
            )
        }
        val result = suspendCancellableCoroutine { continuation ->
            bridge.checkAvailabilityWithCompletion { available, reason ->
                continuation.resume(
                    if (available) {
                        SpeechToTextAvailability.Available
                    } else {
                        SpeechToTextAvailability.Unavailable(
                            reason = reason ?: SpeechUnavailableReasons.NOT_AVAILABLE,
                        )
                    },
                )
            }
        }
        log("SpeechAnalyzerService: checkAvailability -> $result")
        return result
    }

    override fun startListening(): Flow<SpeechToTextResult> = callbackFlow {
        val activity = SpeechActivityWatch()

        // The same TranscriptWatch the legacy path uses, for the same reason: it owns what a
        // transcript means, including that a blank one is never sent on as one. Its own quiet
        // clock goes unused here, because the detector answers that question far better.
        val transcript = TranscriptWatch()

        bridge.startOnPartial(
            onPartial = { text ->
                transcript.record(text = text.orEmpty(), isFinal = false)?.let { trySend(it) }
            },
            onFinal = { text ->
                transcript.record(text = text.orEmpty(), isFinal = true)?.let { trySend(it) }
                close()
            },
            onSpeechActivity = { speechDetected, audioSeconds ->
                activity.record(speechDetected = speechDetected, audioSeconds = audioSeconds)
                if (activity.riderHasFinished()) {
                    log("SpeechAnalyzerService: rider finished, finalising")
                    // finish, not cancel: the analyzer still owes a final transcript and the
                    // flow stays open to receive it.
                    bridge.finish()
                }
            },
            onError = { reason ->
                trySend(
                    SpeechToTextResult.Error(
                        reason = reason ?: SpeechUnavailableReasons.NOT_AVAILABLE,
                    ),
                )
                close()
            },
        )

        awaitClose { bridge.cancel() }
    }

    override fun stopListening() = bridge.finish()

    private fun microphoneAuthorized(): Boolean =
        AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
}
