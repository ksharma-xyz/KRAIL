package xyz.ksharma.krail.io.gtfs.nswstops

import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.seedCurrentStops
import xyz.ksharma.krail.io.gtfs.seedStops
import xyz.ksharma.krail.io.gtfs.stopListBytes
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_STOPS_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NswStopsManagerTest {

    private val sandook = FakeSandook()
    private val preferences = FakeSandookPreferences()
    private var reads = 0
    private var bytes: suspend () -> ByteArray = { stopListBytes("A", "B", "C") }

    private fun KrailTestScope.manager() = NswStopsManager(
        ioDispatcher = ioDispatcher,
        sandook = sandook,
        preferences = preferences,
        readStopsBytes = {
            reads++
            bytes()
        },
    )

    private fun storedIds(vararg ids: String): Set<String> =
        sandook.selectStopsByIds(ids.toList()).map { it.stopId }.toSet()

    @Test
    fun `a new data version replaces the old rows with the bundled ones and records the version`() = krailRunTest {
        sandook.seedStops("OLD1", "OLD2")

        val result = manager().insertStops()

        assertTrue(result.isSuccess)
        assertEquals(setOf("A", "B", "C"), storedIds("OLD1", "OLD2", "A", "B", "C"))
        assertEquals(NSW_STOPS_VERSION, preferences.getLong(KEY_NSW_STOPS_VERSION))
    }

    @Test
    fun `current data is left alone and the bundled file is not read`() = krailRunTest {
        preferences.setLong(KEY_NSW_STOPS_VERSION, NSW_STOPS_VERSION)
        sandook.seedCurrentStops()

        val result = manager().insertStops()

        assertTrue(result.isSuccess)
        assertEquals(0, reads)
    }

    @Test
    fun `a failed read is returned with its cause and the version is not recorded`() = krailRunTest {
        sandook.seedStops("OLD1")
        bytes = { error("file missing") }

        val result = manager().insertStops()

        assertEquals("file missing", result.exceptionOrNull()?.message)
        assertNull(preferences.getLong(KEY_NSW_STOPS_VERSION))
        assertEquals(setOf("OLD1"), storedIds("OLD1"))
    }

    @Test
    fun `a failure part way through the insert leaves the old rows in place`() = krailRunTest {
        // The clear and the inserts share one transaction, so the clear is undone with them.
        // With the clear committed on its own, OLD1 and OLD2 would be gone here.
        sandook.seedStops("OLD1", "OLD2")
        bytes = { stopListBytes("A", "BAD", "C") }
        sandook.failInsertNswStopId = "BAD"

        val result = manager().insertStops()

        assertTrue(result.isFailure)
        assertEquals(setOf("OLD1", "OLD2"), storedIds("OLD1", "OLD2", "A", "C"))
        assertNull(preferences.getLong(KEY_NSW_STOPS_VERSION))
    }

    @Test
    fun `cancellation is rethrown and not reported as a failed insert`() = krailRunTest {
        bytes = { throw CancellationException("stop") }

        assertFailsWith<CancellationException> { manager().insertStops() }
    }
}
