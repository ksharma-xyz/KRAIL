package xyz.ksharma.krail.core.textrecognition

/**
 * On-device OCR. Not currently wired into the AI search-input flow — that flow only offers
 * Speak and Type — but turns a screenshot's text into raw text that would flow into
 * [xyz.ksharma.krail.core.aitext.AiTextService.extractTripIntent] exactly like typed or
 * transcribed text if a caller needed it — OCR is just another way to produce the string,
 * not a separate extraction path. See this module's README for why this is a hand-rolled
 * wrapper rather than an existing library.
 *
 * Same never-throws contract as the rest of this feature's services: unavailability and
 * mid-call failures both collapse to `null`, rendered identically to the feature not
 * existing.
 *
 * Backed by ML Kit Text Recognition v2 on Android and the Vision framework
 * (`VNRecognizeTextRequest`) on iOS — both bundled, on-device, no download step.
 */
interface TextRecognitionService {

    /**
     * Cheap enough to call before every use — implementations cache the underlying
     * platform check rather than re-querying the OS each time. Never throws.
     */
    suspend fun checkAvailability(): TextRecognitionAvailability

    /** Recognizes text in [imageBytes] (PNG/JPEG-encoded), or `null` on failure/no text found. */
    suspend fun recognizeText(imageBytes: ByteArray): String?
}

sealed interface TextRecognitionAvailability {
    data object Available : TextRecognitionAvailability
    data class Unavailable(val reason: String) : TextRecognitionAvailability
}
