package xyz.ksharma.krail.core.speechtotext

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.speech.RecognitionService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AndroidSpeechToTextServiceTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val service = AndroidSpeechToTextService(context)

    // SpeechRecognizer.isRecognitionAvailable() queries PackageManager for a service
    // resolving RecognitionService.SERVICE_INTERFACE - Robolectric's shadow PackageManager
    // returns no results for that query by default, so a test has to register one to
    // simulate a device that actually has a recognizer installed.
    private fun registerRecognitionService() {
        val resolveInfo = ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                packageName = context.packageName
                name = "FakeRecognitionService"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(RecognitionService.SERVICE_INTERFACE),
            resolveInfo,
        )
    }

    @Test
    fun `checkAvailability reports not_available when no recognizer is installed`() = runTest {
        val result = service.checkAvailability()

        assertEquals(SpeechToTextAvailability.Unavailable(reason = "not_available"), result)
    }

    @Test
    fun `checkAvailability reports permission_required when a recognizer exists but RECORD_AUDIO is denied`() =
        runTest {
            registerRecognitionService()
            shadowOf(context).denyPermissions(Manifest.permission.RECORD_AUDIO)

            val result = service.checkAvailability()

            assertEquals(SpeechToTextAvailability.Unavailable(reason = "permission_required"), result)
        }

    @Test
    fun `checkAvailability reports Available once a recognizer exists and RECORD_AUDIO is granted`() = runTest {
        registerRecognitionService()
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)

        val result = service.checkAvailability()

        assertEquals(SpeechToTextAvailability.Available, result)
    }

    @Test
    fun `stopListening with nothing listening does not throw`() {
        service.stopListening()
    }
}
