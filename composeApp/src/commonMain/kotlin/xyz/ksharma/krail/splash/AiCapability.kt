package xyz.ksharma.krail.splash

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult

/**
 * The value of `aiCapability` on `app_start`: whether this device can run Ask KRAIL right now,
 * and if not, why.
 *
 * It rides on every launch, not only on launches where the rider opened Ask KRAIL, because it
 * is the denominator. An attempt row says what happened to riders who could use the feature;
 * this says how many could, and separates the device that cannot from the rider who switched
 * it off. The second one is rider state that exists only on the device at that moment, so no
 * later analysis can recover it for launches that did not record it.
 *
 * **It must never cost the app anything.** Every path returns a value: a hung platform call
 * becomes [TIMEOUT], a thrown one becomes [AiUnavailableReasons.CHECK_FAILED], and a reason
 * this code does not recognise becomes [UNKNOWN]. Uses
 * [AiTextService.peekExtractionAvailability], so asking never starts a model download.
 */
internal object AiCapability {

    const val AVAILABLE = "available"
    const val TIMEOUT = "timeout"
    const val UNKNOWN = "unknown"

    /**
     * Long enough for the platform check on a slow device, short enough that `app_start` is
     * not held back noticeably. The check runs off the splash path, so this delays only the
     * event, never the app.
     */
    const val TIMEOUT_MS = 2_000L

    // The only values that may leave the device. A platform reason outside this set is sent
    // as UNKNOWN rather than passed through: Android appends the exception message to
    // check_failed, and free text from an SDK is not a dimension value.
    private val KNOWN_REASONS = setOf(
        AiUnavailableReasons.MODEL_DOWNLOADING,
        AiUnavailableReasons.DEVICE_UNSUPPORTED,
        AiUnavailableReasons.NEEDS_DEVICE_SETTING,
        AiUnavailableReasons.CHECK_FAILED,
    )

    /**
     * @param peek [AiTextService.peekExtractionAvailability] in production. A function rather
     * than the service so the failure paths are testable without a fake of the service.
     */
    suspend fun of(
        peek: suspend () -> AiAvailability,
        dispatcher: CoroutineDispatcher,
        timeoutMs: Long = TIMEOUT_MS,
    ): String = suspendSafeResult(dispatcher) {
        withTimeoutOrNull(timeoutMs) { peek() }
    }.fold(
        onSuccess = { availability -> availability?.let(::valueOf) ?: TIMEOUT },
        onFailure = { AiUnavailableReasons.CHECK_FAILED },
    )

    fun valueOf(availability: AiAvailability): String = when (availability) {
        AiAvailability.Available -> AVAILABLE
        is AiAvailability.Unavailable -> {
            val reason = availability.reason.substringBefore(':').trim()
            if (reason in KNOWN_REASONS) reason else UNKNOWN
        }
    }
}
