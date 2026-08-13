package xyz.ksharma.krail.core.textrecognition

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * [AndroidTextRecognitionService.recognizeText] isn't covered here beyond [checkAvailability]:
 * every path past the bitmap decode goes through ML Kit's [com.google.mlkit.vision.common.InputImage],
 * which requires `MlKitContext` to have been initialized by the real on-device Play Services
 * `ContentProvider` - something a Robolectric JVM host test never runs (and Robolectric's
 * `BitmapFactory` shadow returns a stub bitmap for any input regardless of content, so even
 * the decode-failure branch can't be forced from here either). Real device/emulator
 * instrumentation is the only way to exercise that path.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidTextRecognitionServiceTest {

    private val service = AndroidTextRecognitionService()

    @Test
    fun `checkAvailability is always Available - bundled model, no download step`() = runTest {
        assertEquals(TextRecognitionAvailability.Available, service.checkAvailability())
    }
}
