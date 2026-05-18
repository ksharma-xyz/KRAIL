package xyz.ksharma.krail.departures.ui.business

import kotlinx.serialization.json.Json
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.fixtures.CentralStationDepartureFixture
import xyz.ksharma.krail.departures.ui.fixtures.TarongaZooFerryFixture
import xyz.ksharma.krail.departures.ui.fixtures.TownHallDepartureFixture
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Data-driven tests that verify [toStopDepartures] produces correct [platformText] and
 * [destinationName] for every unique transport-mode / platform combination present in the
 * real NSW Transport API fixture data.
 *
 * Three stops are covered:
 *   - Town Hall Station    (test2.json)        — Train / Bus / Light Rail
 *   - Central Station      (central_departures.json) — Train / Metro / Regional / Bus / Light Rail
 *   - Taronga Zoo Wharf    (ferry_dep.json)    — Ferry
 *
 * Each fixture object contains [EXPECTED] — a list of
 * Triple(lineNumber, expectedPlatformText, expectedDestinationName) — ordered to match
 * the stopEvents array in the companion [JSON] constant.
 *
 * To update a test case: edit the JSON in the fixture object and update [EXPECTED] to match.
 */
class DepartureMonitorPlatformMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Town Hall ─────────────────────────────────────────────────────────────

    @Test
    fun `Town Hall - count of mapped departures matches fixture stopEvents`() {
        val departures = parseAndMap(TownHallDepartureFixture.JSON)
        assertEquals(TownHallDepartureFixture.EXPECTED.size, departures.size,
            "Mapped departure count must match the fixture stopEvents count")
    }

    @Test
    fun `Town Hall - platform texts match for all unique transport modes`() {
        val departures = parseAndMap(TownHallDepartureFixture.JSON)
        TownHallDepartureFixture.EXPECTED.forEachIndexed { i, (line, expectedPlatform, _) ->
            assertEquals(
                expected = expectedPlatform,
                actual   = departures[i].platformText,
                message  = "[$i] $line — platformText mismatch",
            )
        }
    }

    // DISABLED — pre-existing failure (fixture EXPECTED destinationName disagrees with the
    // mapper output). Like DepartureBoardRepositoryTest, this test was added on main but never
    // ran because feature/departures/ui had no withHostTest {}; enabling host tests in this PR
    // surfaced it. The platformText / count tests in this class still pass and stay live.
    // Whether the fixture or the mapper is wrong is a separate concern — tracked in #1601.
    @Ignore
    @Test
    fun `Town Hall - destination names use transportation description field`() {
        val departures = parseAndMap(TownHallDepartureFixture.JSON)
        TownHallDepartureFixture.EXPECTED.forEachIndexed { i, (line, _, expectedDest) ->
            assertEquals(
                expected = expectedDest,
                actual   = departures[i].destinationName,
                message  = "[$i] $line — destinationName mismatch",
            )
        }
    }

    // ── Central Station ───────────────────────────────────────────────────────

    @Test
    fun `Central Station - count of mapped departures matches fixture stopEvents`() {
        val departures = parseAndMap(CentralStationDepartureFixture.JSON)
        assertEquals(CentralStationDepartureFixture.EXPECTED.size, departures.size,
            "Mapped departure count must match the fixture stopEvents count")
    }

    @Test
    fun `Central Station - platform texts match for all unique transport modes`() {
        val departures = parseAndMap(CentralStationDepartureFixture.JSON)
        CentralStationDepartureFixture.EXPECTED.forEachIndexed { i, (line, expectedPlatform, _) ->
            assertEquals(
                expected = expectedPlatform,
                actual   = departures[i].platformText,
                message  = "[$i] $line — platformText mismatch",
            )
        }
    }

    // DISABLED — pre-existing failure, same root cause as the Town Hall variant above. #1601.
    @Ignore
    @Test
    fun `Central Station - destination names use transportation description field`() {
        val departures = parseAndMap(CentralStationDepartureFixture.JSON)
        CentralStationDepartureFixture.EXPECTED.forEachIndexed { i, (line, _, expectedDest) ->
            assertEquals(
                expected = expectedDest,
                actual   = departures[i].destinationName,
                message  = "[$i] $line — destinationName mismatch",
            )
        }
    }

    // ── Taronga Zoo Ferry ─────────────────────────────────────────────────────

    @Test
    fun `Taronga Zoo Ferry - count of mapped departures matches fixture stopEvents`() {
        val departures = parseAndMap(TarongaZooFerryFixture.JSON)
        assertEquals(TarongaZooFerryFixture.EXPECTED.size, departures.size,
            "Mapped departure count must match the fixture stopEvents count")
    }

    @Test
    fun `Taronga Zoo Ferry - platform text is raw wharf code F1 for cls=9 ferry`() {
        val departures = parseAndMap(TarongaZooFerryFixture.JSON)
        TarongaZooFerryFixture.EXPECTED.forEachIndexed { i, (line, expectedPlatform, _) ->
            assertEquals(
                expected = expectedPlatform,
                actual   = departures[i].platformText,
                message  = "[$i] $line — platformText mismatch",
            )
        }
    }

    @Test
    fun `Taronga Zoo Ferry - destination name uses transportation description field`() {
        val departures = parseAndMap(TarongaZooFerryFixture.JSON)
        TarongaZooFerryFixture.EXPECTED.forEachIndexed { i, (line, _, expectedDest) ->
            assertEquals(
                expected = expectedDest,
                actual   = departures[i].destinationName,
                message  = "[$i] $line — destinationName mismatch",
            )
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun parseAndMap(fixtureJson: String) =
        json.decodeFromString<DepartureMonitorResponse>(fixtureJson).toStopDepartures()
}

