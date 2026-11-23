package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The signal between the Intro and the finale's host: a run only starts when a host is there to play it, a run is
 * played by one host once, the ask to navigate is one shot, and ending a run leaves nothing behind. It is plain
 * state, so it is tested for real with no fake.
 */
class IntroFinaleSignalTest {

    private val signal = IntroFinaleSignal()

    @Test
    fun `nothing is playing until the Intro starts a run`() {
        assertNull(signal.request.value)
        assertFalse(signal.navigated.value)
    }

    @Test
    fun `with no host attached a run does not start, so the caller navigates now`() {
        assertFalse(signal.start("#FF123456"))
        assertNull(signal.request.value)
    }

    @Test
    fun `with a host attached a run starts in the theme colour it was given`() {
        signal.attachHost()

        assertTrue(signal.start("#FF123456"))
        assertEquals("#FF123456", signal.request.value?.themeHexColorCode)
    }

    @Test
    fun `a run does not start while one is being played`() {
        signal.attachHost()
        signal.start("#FF000001")
        val first = signal.request.value

        assertFalse(signal.start("#FF000002"))
        assertEquals(first?.id, signal.request.value?.id)
        assertEquals("#FF000001", signal.request.value?.themeHexColorCode)
    }

    @Test
    fun `a host that has gone away leaves no run to start`() {
        signal.attachHost()
        signal.detachHost()

        assertFalse(signal.start("#FF123456"))
        signal.detachHost()
        signal.attachHost()
        assertTrue(signal.start("#FF123456"), "a host count never goes below none, so one attach is enough")
    }

    @Test
    fun `each run has its own number`() {
        signal.attachHost()
        signal.start("#FF000001")
        val first = assertNotNull(signal.request.value)
        signal.end(first.id)

        signal.start("#FF000001")

        assertNotEquals(first.id, assertNotNull(signal.request.value).id)
    }

    @Test
    fun `a run is claimed once, by the first host that asks`() {
        signal.attachHost()
        signal.start("#FF123456")
        val id = assertNotNull(signal.request.value).id

        assertTrue(signal.claim(id))
        assertFalse(signal.claim(id), "a second host, or the same one after a turn of the screen, played it again")
    }

    @Test
    fun `ending a run clears it and the navigated mark, and ending another run changes nothing`() {
        signal.attachHost()
        signal.start("#FF123456")
        val id = assertNotNull(signal.request.value).id
        signal.onNavigated()

        signal.end(id + 1)
        assertNotNull(signal.request.value)
        assertTrue(signal.navigated.value)

        signal.end(id)
        assertNull(signal.request.value)
        assertFalse(signal.navigated.value)
    }

    @Test
    fun `the ask to navigate arrives once, and two asks before it is taken are one`() = krailRunTest {
        signal.requestNavigate()
        signal.requestNavigate()

        assertEquals(Unit, signal.navigateRequests.first())
        val second = withTimeoutOrNull(1) { signal.navigateRequests.first() }
        assertNull(second, "there was a second ask")
    }

    @Test
    fun `an ask nobody took is dropped when the next run starts, so it never serves the wrong run`() = krailRunTest {
        signal.attachHost()
        signal.requestNavigate()

        signal.start("#FF123456")

        assertNull(withTimeoutOrNull(1) { signal.navigateRequests.first() }, "a stale ask was kept")
    }

    @Test
    fun `a run is not released when it starts, and is released by the Intro once it has written and waited`() {
        signal.attachHost()
        signal.start("#FF123456")
        assertFalse(signal.navigationReleased.value)

        assertTrue(signal.release(), "a run is being played, so the finale holds for this")

        assertTrue(signal.navigationReleased.value)
    }

    @Test
    fun `releasing with no run being played says so, so the caller navigates now, and releases nothing`() {
        assertFalse(signal.release())
        assertFalse(signal.navigationReleased.value)

        signal.attachHost()
        signal.start("#FF123456")
        signal.end(signal.request.value!!.id)
        assertFalse(signal.release())
    }

    @Test
    fun `a run that ends, and the next one that starts, are each held until their own release`() {
        signal.attachHost()
        signal.start("#FF000001")
        signal.release()
        signal.end(signal.request.value!!.id)
        assertFalse(signal.navigationReleased.value)

        signal.start("#FF000002")

        assertFalse(signal.navigationReleased.value, "the second run inherited the first one's release")
    }
}
