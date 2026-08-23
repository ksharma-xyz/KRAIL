package xyz.ksharma.krail.core.speechtotext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The end-of-speech rule for the iOS 26 path, tested here because Swift source has no test task
 * in this project. The bridge reports Apple's voice activity answers; this decides what they
 * mean, and everything the rider notices about how long listening lasts comes from these rules.
 */
class SpeechActivityWatchTest {

    @Test
    fun `quiet is measured from the last speech, in audio time`() {
        val watch = SpeechActivityWatch()

        watch.record(speechDetected = true, audioSeconds = 2.0)
        watch.record(speechDetected = false, audioSeconds = 3.5)

        assertEquals(1.5, watch.quietForSeconds())
    }

    @Test
    fun `a rider still speaking has not finished`() {
        val watch = SpeechActivityWatch()

        watch.record(speechDetected = true, audioSeconds = 1.0)
        watch.record(speechDetected = true, audioSeconds = 4.0)
        watch.record(speechDetected = false, audioSeconds = 5.0)

        assertFalse(watch.riderHasFinished())
    }

    @Test
    fun `three seconds of silence after speech ends the session`() {
        val watch = SpeechActivityWatch()

        watch.record(speechDetected = true, audioSeconds = 2.4)
        watch.record(speechDetected = false, audioSeconds = 5.4)

        assertTrue(watch.riderHasFinished())
    }

    @Test
    fun `a thinking pause inside the window does not end the session`() {
        val watch = SpeechActivityWatch()

        // "from Central to..." then a pause, then the rest. The pause is real silence, so only
        // the window's length protects the rider here. Two seconds of it must not be enough.
        watch.record(speechDetected = true, audioSeconds = 1.5)
        watch.record(speechDetected = false, audioSeconds = 3.4)
        assertFalse(watch.riderHasFinished())

        watch.record(speechDetected = true, audioSeconds = 3.6)
        watch.record(speechDetected = false, audioSeconds = 5.0)
        assertFalse(watch.riderHasFinished())
    }

    @Test
    fun `a rider who has said nothing yet gets the longer wait`() {
        val watch = SpeechActivityWatch()

        // Tapped the mic, still deciding what to ask for. Four seconds of nothing would end a
        // session that had heard words; here it must not.
        watch.record(speechDetected = false, audioSeconds = 4.0)

        assertFalse(watch.heardSpeech)
        assertFalse(watch.riderHasFinished())

        watch.record(speechDetected = false, audioSeconds = 6.0)
        assertTrue(watch.riderHasFinished())
    }

    @Test
    fun `an answer arriving late cannot move the clock backwards`() {
        val watch = SpeechActivityWatch()

        watch.record(speechDetected = true, audioSeconds = 2.0)
        watch.record(speechDetected = false, audioSeconds = 5.2)

        // Callbacks cross a thread boundary from Swift, so ordering is not guaranteed. An old
        // answer arriving after a newer one must not rewind the session or resurrect speech
        // that was already accounted for.
        watch.record(speechDetected = false, audioSeconds = 3.0)
        assertEquals(3.2, watch.quietForSeconds())

        watch.record(speechDetected = true, audioSeconds = 1.0)
        assertEquals(3.2, watch.quietForSeconds())
    }

    @Test
    fun `quiet is never negative before any answer arrives`() {
        val watch = SpeechActivityWatch()

        assertEquals(0.0, watch.quietForSeconds())
        assertFalse(watch.riderHasFinished())
    }
}
