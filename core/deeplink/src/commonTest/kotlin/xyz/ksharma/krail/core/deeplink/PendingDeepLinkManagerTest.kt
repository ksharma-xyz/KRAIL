package xyz.ksharma.krail.core.deeplink

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingDeepLinkManagerTest {

    private fun makeManager(): PendingDeepLinkManager = RealPendingDeepLinkManager()

    // region hasPending

    @Test
    fun `hasPending returns false initially`() {
        assertFalse(makeManager().hasPending())
    }

    @Test
    fun `hasPending returns true after setPending`() {
        val manager = makeManager()
        manager.setPending("encoded-data")
        assertTrue(manager.hasPending())
    }

    @Test
    fun `hasPending returns true after dispatchHot`() {
        val manager = makeManager()
        manager.dispatchHot("encoded-data")
        assertTrue(manager.hasPending())
    }

    @Test
    fun `hasPending returns false after consumePending`() {
        val manager = makeManager()
        manager.setPending("encoded-data")
        manager.consumePending()
        assertFalse(manager.hasPending())
    }

    // endregion

    // region consumePending

    @Test
    fun `consumePending returns null when nothing is pending`() {
        assertNull(makeManager().consumePending())
    }

    @Test
    fun `consumePending returns value set by setPending`() {
        val manager = makeManager()
        manager.setPending("abc123")
        assertEquals("abc123", manager.consumePending())
    }

    @Test
    fun `consumePending returns value set by dispatchHot`() {
        val manager = makeManager()
        manager.dispatchHot("hot-data")
        assertEquals("hot-data", manager.consumePending())
    }

    @Test
    fun `consumePending clears the pending value`() {
        val manager = makeManager()
        manager.setPending("abc123")
        manager.consumePending()
        assertNull(manager.consumePending())
    }

    @Test
    fun `consumePending is single-use`() {
        val manager = makeManager()
        manager.setPending("once")

        assertEquals("once", manager.consumePending())
        assertNull(manager.consumePending())
    }

    // endregion

    // region setPending

    @Test
    fun `setPending overwrites existing pending value`() {
        val manager = makeManager()
        manager.setPending("first")
        manager.setPending("second")

        assertEquals("second", manager.consumePending())
    }

    @Test
    fun `setPending stores exact value including special chars`() {
        val manager = makeManager()
        val encodedData = "eyJmIjoiMTAxMDExMDAiLCJ0IjoiMTAxMDIwOTkifQ=="
        manager.setPending(encodedData)
        assertEquals(encodedData, manager.consumePending())
    }

    @Test
    fun `setPending does NOT emit on hotEvents`() = runTest {
        val manager = makeManager()
        manager.hotEvents.test {
            manager.setPending("cold-data")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region dispatchHot

    @Test
    fun `dispatchHot emits on hotEvents`() = runTest {
        val manager = makeManager()
        manager.hotEvents.test {
            manager.dispatchHot("hot-payload")
            assertEquals("hot-payload", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatchHot sets pending AND emits`() = runTest {
        val manager = makeManager()
        manager.hotEvents.test {
            manager.dispatchHot("hot-payload")
            awaitItem()
            assertEquals("hot-payload", manager.consumePending())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatchHot emits each call independently`() = runTest {
        val manager = makeManager()
        manager.hotEvents.test {
            manager.dispatchHot("first")
            manager.consumePending()
            manager.dispatchHot("second")

            assertEquals("first", awaitItem())
            assertEquals("second", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatchHot overwrites pending from previous setPending`() {
        val manager = makeManager()
        manager.setPending("cold")
        manager.dispatchHot("hot")
        assertEquals("hot", manager.consumePending())
    }

    // endregion

    // region sequential use

    @Test
    fun `manager can be reused after consume`() {
        val manager = makeManager()

        manager.setPending("first-trip")
        assertEquals("first-trip", manager.consumePending())

        manager.setPending("second-trip")
        assertTrue(manager.hasPending())
        assertEquals("second-trip", manager.consumePending())

        assertFalse(manager.hasPending())
    }

    @Test
    fun `cold start then hot intent flow`() = runTest {
        val manager = makeManager()

        manager.setPending("cold-start-data")
        assertTrue(manager.hasPending())

        assertEquals("cold-start-data", manager.consumePending())
        assertFalse(manager.hasPending())

        manager.hotEvents.test {
            manager.dispatchHot("hot-deep-link")
            assertEquals("hot-deep-link", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("hot-deep-link", manager.consumePending())
    }

    // endregion
}
