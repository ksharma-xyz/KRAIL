package xyz.ksharma.krail.core.textrecognition

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume

/**
 * ML Kit Text Recognition v2 — on-device, bundled model, no separate download step (unlike
 * `core:ai-text`'s Gemini Nano dependency), so [checkAvailability] has nothing to actually
 * check once the dependency exists on the device's Play Services install.
 */
internal class AndroidTextRecognitionService : TextRecognitionService {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun checkAvailability(): TextRecognitionAvailability =
        TextRecognitionAvailability.Available

    override suspend fun recognizeText(imageBytes: ByteArray): String? {
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }.getOrNull()

        if (bitmap == null) {
            log("TextRecognitionService: recognizeText -> decode failed")
            return null
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text.ifBlank { null }
                    log(
                        "TextRecognitionService: recognizeText -> " +
                            if (text == null) "null" else "${text.length} chars",
                    )
                    continuation.resume(text)
                }
                .addOnFailureListener { throwable ->
                    log("TextRecognitionService: recognizeText failed: ${throwable.message}")
                    continuation.resume(null)
                }
        }
    }
}
