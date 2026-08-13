package xyz.ksharma.krail.core.aitext

import aiTextBridge.AiTextBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Backed by Apple's Foundation Models framework (iOS 26+) via a small `@objc` Swift shim
 * ([AiTextBridge], see `src/swift/aiTextBridge/`) — Foundation Models has no Objective-C
 * header, so Kotlin/Native cinterop cannot see `platform.FoundationModels.*` directly; see
 * `docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md` for why this shim exists.
 *
 * Foundation Models is one unified on-device model, unlike Android's two separate ML Kit
 * GenAI features (`genai-summarization` vs `genai-prompt`) — [checkExtractionAvailability]
 * delegates straight to [checkAvailability] rather than tracking independent state.
 *
 * The simulator cannot run Apple Intelligence models at all (an Apple platform
 * restriction, not a bug here) — [checkAvailability] always reports [AiAvailability.Unavailable]
 * there. Real behaviour only shows up on a physical, Apple-Intelligence-enabled device.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosAiTextService : AiTextService {

    private val bridge = AiTextBridge()

    override suspend fun checkAvailability(): AiAvailability =
        suspendCancellableCoroutine { continuation ->
            bridge.checkAvailabilityWithCompletion { available, reason ->
                val result = if (available) {
                    AiAvailability.Available
                } else {
                    AiAvailability.Unavailable(reason = reason ?: "unknown")
                }
                continuation.resume(result)
            }
        }

    override suspend fun checkExtractionAvailability(): AiAvailability = checkAvailability()

    override suspend fun summarize(text: String): String? =
        suspendCancellableCoroutine { continuation ->
            bridge.summarizeWithText(text) { summary ->
                continuation.resume(summary)
            }
        }

    override suspend fun extractTripIntent(text: String): TripIntentExtraction? =
        suspendCancellableCoroutine { continuation ->
            bridge.extractTripIntentWithText(
                text = text,
            ) { originText, destinationText, hasTimeIntent, isArrival, timeText, rawModeHints ->
                val modeHints = rawModeHints.orEmpty().filterIsInstance<String>()
                val hasAnything = originText != null ||
                    destinationText != null ||
                    hasTimeIntent ||
                    modeHints.isNotEmpty()
                val result = if (!hasAnything) {
                    null
                } else {
                    TripIntentExtraction(
                        originText = originText,
                        destinationText = destinationText,
                        timeIntent = if (hasTimeIntent) {
                            TimeIntent(isArrival = isArrival, timeText = timeText ?: "")
                        } else {
                            null
                        },
                        modeHints = modeHints,
                    )
                }
                continuation.resume(result)
            }
        }
}
