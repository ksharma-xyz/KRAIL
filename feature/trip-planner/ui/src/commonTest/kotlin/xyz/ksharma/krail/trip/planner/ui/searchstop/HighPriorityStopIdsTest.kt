package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `high_priority_stop_ids` reaches the app in two JSON shapes: the server's object
 * (`{"stop_ids": [...]}`) and the in-app default, a bare array. Remote Config serves the default
 * until its first successful fetch, so both must parse and neither may throw.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HighPriorityStopIdsTest {

    private val defaultJson: String = RemoteConfigDefaults.getDefaults()
        .first { it.first == FlagKeys.HIGH_PRIORITY_STOP_IDS.key }
        .second as String

    private val defaultIds: List<String> = checkNotNull(parseHighPriorityStopIds(defaultJson))

    // region parser

    @Test
    fun `object shape returns its stop ids`() {
        val ids = FlagValue.JsonValue("""{"stop_ids": ["200060", "200070"]}""").toHighPriorityStopIds()

        assertEquals(listOf("200060", "200070"), ids)
    }

    @Test
    fun `bare array shape returns its stop ids`() {
        val ids = FlagValue.JsonValue("""["200060", "200070"]""").toHighPriorityStopIds()

        assertEquals(listOf("200060", "200070"), ids)
    }

    @Test
    fun `the in-app default array string parses to the default list`() {
        val ids = FlagValue.JsonValue(defaultJson).toHighPriorityStopIds()

        assertEquals(defaultIds, ids)
        assertTrue(ids.size > 1, "Expected the default list to carry several ids, got $ids")
        assertTrue("200060" in ids, "Expected Central in the default list, got $ids")
    }

    @Test
    fun `ids come back without surrounding quotes`() {
        val fromObject = FlagValue.JsonValue("""{"stop_ids": ["200060"]}""").toHighPriorityStopIds()
        val fromArray = FlagValue.JsonValue("""["200060"]""").toHighPriorityStopIds()

        assertEquals(listOf("200060"), fromObject)
        assertEquals(listOf("200060"), fromArray)
    }

    @Test
    fun `numeric ids read the same as quoted ids`() {
        val ids = FlagValue.JsonValue("""{"stop_ids": [200060, "200070"]}""").toHighPriorityStopIds()

        assertEquals(listOf("200060", "200070"), ids)
    }

    @Test
    fun `empty object returns an empty list`() {
        assertEquals(emptyList(), FlagValue.JsonValue("{}").toHighPriorityStopIds())
    }

    @Test
    fun `object with an empty stop_ids array returns an empty list`() {
        assertEquals(emptyList(), FlagValue.JsonValue("""{"stop_ids": []}""").toHighPriorityStopIds())
    }

    @Test
    fun `empty array returns an empty list`() {
        assertEquals(emptyList(), FlagValue.JsonValue("[]").toHighPriorityStopIds())
    }

    @Test
    fun `malformed json returns the defaults and does not throw`() {
        listOf("[\"200060\", ", "{\"stop_ids\": [", "{not json", "[", "[]]").forEach { broken ->
            assertEquals(
                defaultIds,
                FlagValue.JsonValue(broken).toHighPriorityStopIds(),
                "Expected the defaults for malformed value: $broken",
            )
        }
    }

    @Test
    fun `unexpected json shape returns the defaults and does not throw`() {
        listOf(
            """{"stop_ids": "200060"}""",
            """{"stop_ids": {"a": 1}}""",
            """{"stop_ids": 5}""",
            "5",
        ).forEach { odd ->
            assertEquals(
                defaultIds,
                FlagValue.JsonValue(odd).toHighPriorityStopIds(),
                "Expected the defaults for unexpected shape: $odd",
            )
        }
    }

    @Test
    fun `nested and null entries are skipped rather than thrown on`() {
        val ids = FlagValue.JsonValue("""["200060", null, ["x"], {"a": 1}, "200070"]""").toHighPriorityStopIds()

        assertEquals(listOf("200060", "200070"), ids)
    }

    @Test
    fun `a flag that is not json returns the defaults`() {
        assertEquals(defaultIds, FlagValue.BooleanValue(false).toHighPriorityStopIds())
        assertEquals(defaultIds, FlagValue.StringValue("200060").toHighPriorityStopIds())
        assertEquals(defaultIds, FlagValue.NumberValue(200_060).toHighPriorityStopIds())
    }

    @Test
    fun `parseHighPriorityStopIds reports null for input it cannot read`() {
        assertNull(parseHighPriorityStopIds("{not json"))
        assertNull(parseHighPriorityStopIds("""{"stop_ids": "x"}"""))
        assertNull(parseHighPriorityStopIds("\"just a string\""))
    }

    // endregion

    // region manager: typed search reads the flag lazily, so a throw here fails every search

    @Test
    fun `prioritiseStops works when the flag still holds the in-app default array`() {
        val manager = managerWith(FlagValue.JsonValue(defaultJson))

        val result = manager.prioritiseStops(listOf(stop("OTHER", "Other Bus Stop"), stop("200060", "Central")))

        assertEquals("200060", result.first().stopId)
    }

    @Test
    fun `prioritiseStops works with the server object shape`() {
        val manager = managerWith(FlagValue.JsonValue("""{"stop_ids": ["HP001"]}"""))

        val result = manager.prioritiseStops(listOf(stop("OTHER", "Other Bus Stop"), stop("HP001", "Zeta Stop")))

        assertEquals("HP001", result.first().stopId)
    }

    @Test
    fun `prioritiseStops does not throw on a malformed flag value`() {
        val manager = managerWith(FlagValue.JsonValue("{not json"))

        val result = manager.prioritiseStops(listOf(stop("OTHER", "Other Bus Stop"), stop("200060", "Central")))

        assertEquals("200060", result.first().stopId)
    }

    // endregion

    private fun managerWith(highPriority: FlagValue): RealStopResultsManager {
        val flag = FakeFlag().apply {
            setFlagValue(FlagKeys.HIGH_PRIORITY_STOP_IDS.key, highPriority)
            setFlagValue(FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key, FlagValue.BooleanValue(false))
        }
        return RealStopResultsManager(
            sandook = FakeSandook(),
            nswBusRoutesSandook = NoOpBusRoutes,
            flag = flag,
            fuzzyStopRanker = DefaultFuzzyStopRanker(),
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun stop(id: String, name: String) = SearchStopState.SearchResult.Stop(
        stopId = id,
        stopName = name,
        transportModeType = persistentListOf(TransportMode.Bus),
    )
}
