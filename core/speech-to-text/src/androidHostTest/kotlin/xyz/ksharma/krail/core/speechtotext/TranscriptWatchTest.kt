package xyz.ksharma.krail.core.speechtotext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * The two rules the iOS speech surface turns on, tested here because iOS source has no test task
 * in this project. Both were rider-facing bugs: a sentence cleared from the field, and a session
 * that never ended on its own.
 */
class TranscriptWatchTest {

    @Test
    fun `partials carry the words as they arrive`() {
        val watch = TranscriptWatch()

        assertEquals(SpeechToTextResult.Partial("central"), watch.record("central", isFinal = false))
        assertEquals(
            SpeechToTextResult.Partial("central to town hall"),
            watch.record("central to town hall", isFinal = false),
        )
        assertTrue(watch.heardAnything)
    }

    @Test
    fun `a blank partial is not sent on`() {
        val watch = TranscriptWatch()
        watch.record("central to town hall", isFinal = false)

        // Nothing to send, rather than an empty string to write into the rider's field.
        assertNull(watch.record("", isFinal = false))
    }

    @Test
    fun `a blank final reports the last words actually heard`() {
        val watch = TranscriptWatch()
        watch.record("central to town hall", isFinal = false)

        assertEquals(
            SpeechToTextResult.Final("central to town hall"),
            watch.record("", isFinal = true),
        )
    }

    @Test
    fun `a session that heard nothing ends as a session with no result`() {
        val watch = TranscriptWatch()

        assertEquals(
            SpeechToTextResult.Error(SpeechUnavailableReasons.NO_RESULT),
            watch.record("", isFinal = true),
        )
        assertTrue(!watch.heardAnything)
    }

    @Test
    fun `a whitespace-only transcript counts as nothing heard`() {
        val watch = TranscriptWatch()

        // isBlank, not isEmpty. A recogniser that reports a space or a newline is reporting the
        // same nothing, and " " written into the field is still the rider's sentence gone.
        assertNull(watch.record("   ", isFinal = false))
        assertEquals(
            SpeechToTextResult.Error(SpeechUnavailableReasons.NO_RESULT),
            watch.record("\n", isFinal = true),
        )
    }

    @Test
    fun `new words restart the quiet window`() {
        val time = TestTimeSource()
        val watch = TranscriptWatch(timeSource = time)

        time += 3.seconds
        watch.record("central", isFinal = false)
        time += 1.seconds

        assertEquals(1_000L, watch.quietForMillis())
    }

    @Test
    fun `a revision that adds no words does not restart the quiet window`() {
        val time = TestTimeSource()
        val watch = TranscriptWatch(timeSource = time)
        watch.record("central to town hall", isFinal = false)

        // The rider stopped here. Everything after this is the recogniser tidying up what it
        // already heard: capitalisation, then punctuation. Treated as speech, these kept the
        // microphone open until the ViewModel's ceiling cut it off.
        time += 2.seconds
        watch.record("Central to Town Hall", isFinal = false)
        time += 1.seconds
        watch.record("Central to Town Hall.", isFinal = false)

        assertEquals(3_000L, watch.quietForMillis())
    }

    @Test
    fun `a revision that drops a word does not restart the window either`() {
        val time = TestTimeSource()
        val watch = TranscriptWatch(timeSource = time)
        watch.record("central to town hall station", isFinal = false)

        // The recogniser deciding it heard one word too many is not the rider speaking. Only
        // growth counts, so a shorter transcript leaves the clock exactly where it was.
        time += 2.seconds
        watch.record("central to town hall", isFinal = false)
        time += 1.seconds

        assertEquals(3_000L, watch.quietForMillis())
    }

    @Test
    fun `the last words heard survive a revision down to nothing`() {
        val watch = TranscriptWatch()
        watch.record("central to town hall", isFinal = false)
        watch.record("", isFinal = false)

        // The blank in the middle must not become what the session is worth.
        assertEquals(
            SpeechToTextResult.Final("central to town hall"),
            watch.record("", isFinal = true),
        )
    }

    @Test
    fun `a revision that adds a word does restart it`() {
        val time = TestTimeSource()
        val watch = TranscriptWatch(timeSource = time)
        watch.record("central to town", isFinal = false)

        time += 2.seconds
        watch.record("central to town hall", isFinal = false)
        time += 1.seconds

        assertEquals(1_000L, watch.quietForMillis())
    }
}
