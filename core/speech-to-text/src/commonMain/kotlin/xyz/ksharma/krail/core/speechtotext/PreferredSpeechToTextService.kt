package xyz.ksharma.krail.core.speechtotext

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.log.log

/**
 * Uses [preferred] when the device can run it, and [fallback] otherwise.
 *
 * Built for iOS, where the two are `SpeechAnalyzerSpeechToTextService` (iOS 26's analyzer, with
 * Apple's own voice activity detection) and `IosSpeechToTextService` (the `SFSpeechRecognizer`
 * path, which works back to the deployment target). It lives in `commonMain` because choosing
 * between two implementations is not an iOS idea, and because `iosMain` has no test task, which
 * is the reason a run of faults lived in that source set unnoticed.
 *
 * The choice is made in [checkAvailability] and remembered, because that is the one call every
 * caller already makes before listening, and it is the only call that can answer the question
 * without asking the rider to wait twice.
 *
 * A device that cannot run the analyzer is not a device that cannot listen: the fallback answers
 * for itself, and its answer is the one the caller sees. That matters because the analyzer's
 * reasons and the fallback's are not the same set. Reporting "the locale's model is downloading"
 * from a device whose `SFSpeechRecognizer` will happily listen right now would take the feature
 * away for no reason.
 */
internal class PreferredSpeechToTextService(
    private val preferred: SpeechToTextService,
    private val fallback: SpeechToTextService,
) : SpeechToTextService {

    // Null until asked. Listening before checking availability is a caller doing it wrong, and
    // the fallback is the safer of the two to be wrong with: it is the one every device has.
    private var chosen: SpeechToTextService? = null

    private val current: SpeechToTextService get() = chosen ?: fallback

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val fromPreferred = preferred.checkAvailability()
        if (fromPreferred is SpeechToTextAvailability.Available) {
            chosen = preferred
            log("SpeechToText: using the preferred implementation")
            return fromPreferred
        }
        chosen = fallback
        val fromFallback = fallback.checkAvailability()
        log("SpeechToText: preferred unavailable ($fromPreferred), falling back -> $fromFallback")
        return fromFallback
    }

    override fun startListening(): Flow<SpeechToTextResult> = current.startListening()

    override fun stopListening() = current.stopListening()
}
