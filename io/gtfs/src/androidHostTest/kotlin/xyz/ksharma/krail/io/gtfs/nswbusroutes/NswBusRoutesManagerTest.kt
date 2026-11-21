package xyz.ksharma.krail.io.gtfs.nswbusroutes

import app.krail.kgtfs.proto.NswBusRouteGroup
import app.krail.kgtfs.proto.NswBusRouteList
import app.krail.kgtfs.proto.NswBusRouteVariant
import app.krail.kgtfs.proto.NswBusTripOption
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_BUS_ROUTES_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_BUS_ROUTES_VERSION
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NswBusRoutesManagerTest {

    private val sandook = FakeNswBusRoutesSandook()
    private val preferences = FakeSandookPreferences()
    private var bytes: suspend () -> ByteArray = { routeBytes("702", "T1") }

    private fun routeBytes(vararg shortNames: String): ByteArray = NswBusRouteList.ADAPTER.encode(
        NswBusRouteList(
            routes = shortNames.map { name ->
                NswBusRouteGroup(
                    routeShortName = name,
                    variants = listOf(
                        NswBusRouteVariant(
                            routeId = "id_$name",
                            routeName = "Route $name",
                            trips = listOf(NswBusTripOption(tripId = "trip_$name", stopIds = listOf("1", "2"))),
                        ),
                    ),
                )
            },
        ),
    )

    private fun KrailTestScope.manager() = NswBusRoutesManager(
        ioDispatcher = ioDispatcher,
        nswBusRoutesSandook = sandook,
        preferences = preferences,
        readRoutesBytes = { bytes() },
    )

    @Test
    fun `a new data version replaces the old routes with the bundled ones and records the version`() = krailRunTest {
        sandook.insertBusRouteGroup("OLD")

        val result = manager().insertStops()

        assertTrue(result.isSuccess)
        assertEquals(listOf("702", "T1"), sandook.groupNames())
        assertEquals(NSW_BUS_ROUTES_VERSION, preferences.getLong(KEY_NSW_BUS_ROUTES_VERSION))
    }

    @Test
    fun `a failure part way through the insert leaves the old routes in place`() = krailRunTest {
        // The clear shares the insert's transaction, so a failed insert cannot leave the table empty.
        sandook.insertBusRouteGroup("OLD")
        bytes = { routeBytes("702", "BAD", "T1") }
        sandook.failInsertGroup = "BAD"

        val result = manager().insertStops()

        assertTrue(result.isFailure)
        assertEquals(listOf("OLD"), sandook.groupNames())
        assertNull(preferences.getLong(KEY_NSW_BUS_ROUTES_VERSION))
    }

    @Test
    fun `a failed read is returned with its cause and the old routes stay`() = krailRunTest {
        sandook.insertBusRouteGroup("OLD")
        bytes = { error("file missing") }

        val result = manager().insertStops()

        assertEquals("file missing", result.exceptionOrNull()?.message)
        assertEquals(listOf("OLD"), sandook.groupNames())
    }

    @Test
    fun `cancellation is rethrown and not reported as a failed insert`() = krailRunTest {
        bytes = { throw CancellationException("stop") }

        assertFailsWith<CancellationException> { manager().insertStops() }
    }
}
