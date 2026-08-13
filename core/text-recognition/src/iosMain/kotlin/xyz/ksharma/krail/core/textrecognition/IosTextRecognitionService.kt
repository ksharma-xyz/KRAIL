package xyz.ksharma.krail.core.textrecognition

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume

/**
 * Vision framework's `VNRecognizeTextRequest`, run via `VNImageRequestHandler` — a plain
 * Objective-C-compatible framework, directly callable from Kotlin/Native, no SPM cinterop
 * bridge needed (same reasoning as [xyz.ksharma.krail.core.speechtotext.IosSpeechToTextService]
 * vs. the Foundation Models bridge `IosAiTextService` actually needs).
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosTextRecognitionService : TextRecognitionService {

    override suspend fun checkAvailability(): TextRecognitionAvailability =
        TextRecognitionAvailability.Available

    override suspend fun recognizeText(imageBytes: ByteArray): String? {
        val cgImage = imageBytes.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
            UIImage(data = data).CGImage
        }

        if (cgImage == null) {
            log("TextRecognitionService: recognizeText -> decode failed")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val request = VNRecognizeTextRequest { request, error ->
                if (error != null) {
                    log("TextRecognitionService: recognizeText failed: ${error.localizedDescription}")
                    continuation.resume(null)
                } else {
                    val observations = request?.results as? List<VNRecognizedTextObservation>
                    val text = observations
                        ?.mapNotNull { observation ->
                            val topCandidate = observation.topCandidates(1UL).firstOrNull() as? VNRecognizedText
                            topCandidate?.string
                        }
                        ?.joinToString(separator = "\n")
                        ?.ifBlank { null }
                    log(
                        "TextRecognitionService: recognizeText -> " +
                            if (text == null) "null" else "${text.length} chars",
                    )
                    continuation.resume(text)
                }
            }

            val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any>())
            handler.performRequests(listOf(request), error = null)
        }
    }
}
