package xyz.ksharma.krail.core.speechtotext

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.speechtotext.testfakes.FakeSpeechToTextService
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which of the two iOS implementations answers, and what the rider is told when the newer one
 * cannot run. Tested here because the alternative is testing it nowhere: both implementations
 * live in `iosMain`, which has no test task in this project.
 */
class PreferredSpeechToTextServiceTest {

    @Test
    fun `a device that can run the preferred implementation uses it`() = runTest {
        val preferred = FakeSpeechToTextService(SpeechToTextAvailability.Available, "analyzer")
        val fallback = FakeSpeechToTextService(SpeechToTextAvailability.Available, "legacy")
        val service = PreferredSpeechToTextService(preferred = preferred, fallback = fallback)

        assertEquals(SpeechToTextAvailability.Available, service.checkAvailability())
        assertEquals("analyzer", service.firstTranscript())
    }

    @Test
    fun `a device that cannot falls back, and the fallback's answer is the one reported`() =
        runTest {
            // The two implementations do not share a set of reasons. Reporting the newer one's
            // "the model for this locale is still downloading" on a device whose older
            // recogniser will listen right now would take the feature away for no reason.
            val preferred = FakeSpeechToTextService(
                SpeechToTextAvailability.Unavailable("model_downloading"),
                "analyzer",
            )
            val fallback = FakeSpeechToTextService(SpeechToTextAvailability.Available, "legacy")
            val service = PreferredSpeechToTextService(preferred = preferred, fallback = fallback)

            assertEquals(SpeechToTextAvailability.Available, service.checkAvailability())
            assertEquals("legacy", service.firstTranscript())
        }

    @Test
    fun `when neither can listen the rider hears about the fallback`() = runTest {
        val preferred = FakeSpeechToTextService(SpeechToTextAvailability.Unavailable("not_available"), "a")
        val fallback = FakeSpeechToTextService(
            SpeechToTextAvailability.Unavailable(SpeechUnavailableReasons.PERMISSION_REQUIRED),
            "b",
        )
        val service = PreferredSpeechToTextService(preferred = preferred, fallback = fallback)

        assertEquals(
            SpeechToTextAvailability.Unavailable(SpeechUnavailableReasons.PERMISSION_REQUIRED),
            service.checkAvailability(),
        )
    }

    @Test
    fun `listening before checking availability uses the one every device has`() = runTest {
        // A caller doing it in the wrong order still gets a working microphone rather than the
        // implementation that may not exist on this OS version.
        val preferred = FakeSpeechToTextService(SpeechToTextAvailability.Available, "analyzer")
        val fallback = FakeSpeechToTextService(SpeechToTextAvailability.Available, "legacy")
        val service = PreferredSpeechToTextService(preferred = preferred, fallback = fallback)

        assertEquals("legacy", service.firstTranscript())
    }

    @Test
    fun `stopping goes to whichever one is listening`() = runTest {
        val preferred = FakeSpeechToTextService(SpeechToTextAvailability.Available, "analyzer")
        val fallback = FakeSpeechToTextService(SpeechToTextAvailability.Available, "legacy")
        val service = PreferredSpeechToTextService(preferred = preferred, fallback = fallback)
        service.checkAvailability()

        service.stopListening()

        assertEquals(1, preferred.stopCount)
        assertEquals(0, fallback.stopCount)
    }

    private suspend fun SpeechToTextService.firstTranscript(): String {
        var text = ""
        startListening().collect { if (it is SpeechToTextResult.Final) text = it.text }
        return text
    }
}
