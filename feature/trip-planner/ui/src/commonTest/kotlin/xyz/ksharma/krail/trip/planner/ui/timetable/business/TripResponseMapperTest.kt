package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.OranParkToSevenHillsFixture
import kotlin.test.Test
import kotlin.test.assertEquals

import kotlin.test.assertNull

/**
 * Tests for [TripResponse.Leg.resolveDurationSeconds], specifically the duration-resolution logic.
 *
 * Background: The NSW Transport API sometimes omits the `duration` field for certain legs
 * (e.g., the first short bus hop in a multi-leg journey). Without a fallback the leg
 * would be silently dropped from the UI. [TripResponse.Leg.resolveDurationSeconds] ensures
 * the duration is computed from departure/arrival timestamps when the raw field is absent.
 */
class TripResponseMapperTest {

    // Lenient JSON parser — mirrors how Ktor deserialises API responses in production
    private val json = Json { ignoreUnknownKeys = true }

    //region resolveDurationSeconds

    @Test
    fun `resolveDurationSeconds returns explicit duration when present`() {
        // Arrange: leg with an explicit duration of 60 seconds
        val leg = buildLeg(
            duration = 60L,
            depTime = "2026-04-18T22:48:00Z",
            arrTime = "2026-04-18T22:49:00Z",
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: the explicit duration wins over the calculated one
        assertEquals(60L, result)
    }

    @Test
    fun `resolveDurationSeconds calculates duration from timestamps when duration is null`() {
        // Arrange: leg where the API did NOT include a duration field (real-world occurrence
        // observed for the first bus leg, e.g., bus 858 Oran Park — 2 stops, 1 minute ride).
        // 22:48 → 22:49 = 60 seconds
        val leg = buildLeg(
            duration = null,
            depTime = "2026-04-18T22:48:00Z",
            arrTime = "2026-04-18T22:49:00Z",
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: 60 seconds calculated from timestamps
        assertEquals(60L, result)
    }

    @Test
    fun `resolveDurationSeconds prefers estimated times over planned times`() {
        // Arrange: leg where estimated times differ from planned (real-time delay scenario).
        // Planned: 22:48 → 22:49 = 60 s; Estimated: 22:48 → 22:51 = 180 s
        val leg = TripResponse.Leg(
            duration = null,
            origin = TripResponse.StopSequence(
                departureTimePlanned = "2026-04-18T22:48:00Z",
                departureTimeEstimated = "2026-04-18T22:48:00Z", // estimated dep same
            ),
            destination = TripResponse.StopSequence(
                arrivalTimePlanned = "2026-04-18T22:49:00Z",
                arrivalTimeEstimated = "2026-04-18T22:51:00Z",  // estimated arr 2 min late
            ),
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: uses estimated arrival → 3 minutes (180 s)
        assertEquals(180L, result)
    }

    @Test
    fun `resolveDurationSeconds returns null when both duration and timestamps are null`() {
        // Arrange: completely empty leg (edge case / malformed API response)
        val leg = TripResponse.Leg(
            duration = null,
            origin = null,
            destination = null,
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: nothing to compute from, must be null
        assertNull(result)
    }

    @Test
    fun `resolveDurationSeconds returns null when arrival is before departure`() {
        // Arrange: timestamps inverted (shouldn't happen in practice but API data can be noisy)
        val leg = buildLeg(
            duration = null,
            depTime = "2026-04-18T22:49:00Z",
            arrTime = "2026-04-18T22:48:00Z", // arrival BEFORE departure
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: negative duration is meaningless; method returns null via takeIf { it > 0 }
        assertNull(result)
    }

    @Test
    fun `resolveDurationSeconds returns null for zero-second duration (same timestamps)`() {
        // Arrange: departure == arrival (e.g., instantaneous interchange stop)
        val leg = buildLeg(
            duration = null,
            depTime = "2026-04-18T22:48:00Z",
            arrTime = "2026-04-18T22:48:00Z",
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: 0 is not > 0, so returns null (no meaningful duration to show)
        assertNull(result)
    }

    @Test
    fun `resolveDurationSeconds handles longer journeys correctly`() {
        // Arrange: 24-minute leg (1440 seconds) — typical for a multi-stop bus run
        val leg = buildLeg(
            duration = null,
            depTime = "2026-04-18T23:00:00Z",
            arrTime = "2026-04-18T23:24:00Z",
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert
        assertEquals(1440L, result)
    }

    @Test
    fun `resolveDurationSeconds falls back to planned times when estimated times are absent`() {
        // Arrange: no real-time data available (common for timetable-only requests)
        val leg = TripResponse.Leg(
            duration = null,
            origin = TripResponse.StopSequence(
                departureTimePlanned = "2026-04-18T22:48:00Z",
                departureTimeEstimated = null,
            ),
            destination = TripResponse.StopSequence(
                arrivalTimePlanned = "2026-04-18T22:49:00Z",
                arrivalTimeEstimated = null,
            ),
        )

        // Act
        val result = leg.resolveDurationSeconds()

        // Assert: planned timestamps used as fallback → 60 seconds
        assertEquals(60L, result)
    }

    //endregion

    //region buildJourneyList — end-to-end fixture tests (OranPark → SevenHills)

    /**
     * End-to-end smoke test using [OranParkToSevenHillsFixture].
     *
     * Verifies that [buildJourneyList] produces the expected number of journey cards.
     * A regression here means the mapper is silently dropping journeys.
     */
    @Test
    fun `buildJourneyList returns expected journey count from OranPark fixture`() {
        // Arrange: parse the real-world-shaped fixture JSON into a TripResponse
        val response = json.decodeFromString<TripResponse>(OranParkToSevenHillsFixture.JSON)

        // Act
        val journeys = response.buildJourneyList()

        // Assert: both journeys survive — including the one with null duration on the first leg
        assertEquals(
            OranParkToSevenHillsFixture.EXPECTED_JOURNEY_COUNT,
            journeys?.size,
            "Journey with null-duration first leg must not be dropped",
        )
    }

    /**
     * Regression test: the journey whose first bus leg has NO `duration` field in the API
     * response must still appear as a valid journey card.
     *
     * Prior to the fix, [buildJourneyList] logged:
     * "Something is null - NOT adding Transport LEG: ... displayDuration: null"
     * and dropped the entire journey card from the UI.
     */
    @Test
    fun `buildJourneyList includes journey where first leg duration is null`() {
        // Arrange: the second journey in the fixture has duration omitted on its first leg
        val response = json.decodeFromString<TripResponse>(OranParkToSevenHillsFixture.JSON)

        // Act
        val journeys = response.buildJourneyList()

        // Assert: the null-duration journey (index 1) must be present, not null
        val nullDurationJourney = journeys?.getOrNull(1)
        assertEquals(
            true,
            nullDurationJourney != null,
            "Journey with null-duration leg must be present in the result list",
        )
    }

    /**
     * Verifies that [buildJourneyList] with a null-duration leg correctly calculates
     * the leg duration from timestamps (21:46 → 21:47 = 60 seconds = 1 min).
     */
    @Test
    fun `buildJourneyList calculates 1-min duration for null-duration leg from timestamps`() {
        // Arrange
        val response = json.decodeFromString<TripResponse>(OranParkToSevenHillsFixture.JSON)

        // Act
        val journeys = response.buildJourneyList()

        // Assert: leg duration derived from 21:46→21:47 timestamps shows as "1 min"
        val firstLegOfNullDurationJourney = journeys
            ?.getOrNull(1)         // second journey = the null-duration one
            ?.legs
            ?.firstOrNull()        // first TransportLeg
        val legDuration =
            (firstLegOfNullDurationJourney as? TimeTableState.JourneyCardInfo.Leg.TransportLeg)
                ?.totalDuration

        assertEquals(
            "1 min",
            legDuration,
            "Null-duration leg duration should be calculated as 1 min"
        )
    }

    //endregion

    //endregion

    //region displayText on TransportLeg

    @Test
    fun `Given Train leg with destination When buildJourneyList Then displayText is towards destination`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    destinationName = "Bondi Junction via Central",
                    description = "Waterfall or Cronulla to Bondi Junction"
                )
            ),
        )

        val leg = response.buildJourneyList()
            ?.firstOrNull()?.legs?.firstOrNull() as? TimeTableState.JourneyCardInfo.Leg.TransportLeg

        assertEquals("towards Bondi Junction via Central", leg?.displayText)
    }

    @Test
    fun `Given Metro leg with destination When buildJourneyList Then displayText is towards destination`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 2L,
                    destinationName = "Tallawong",
                    description = "Sydenham to Tallawong"
                )
            ),
        )

        val leg = response.buildJourneyList()
            ?.firstOrNull()?.legs?.firstOrNull() as? TimeTableState.JourneyCardInfo.Leg.TransportLeg

        assertEquals("towards Tallawong", leg?.displayText)
    }

    @Test
    fun `Given Bus leg with description When buildJourneyList Then displayText uses description`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 5L,
                    destinationName = "Rouse Hill Station",
                    description = "Seven Hills to Rouse Hill Station via Norwest"
                )
            ),
        )

        val leg = response.buildJourneyList()
            ?.firstOrNull()?.legs?.firstOrNull() as? TimeTableState.JourneyCardInfo.Leg.TransportLeg

        assertEquals("Seven Hills to Rouse Hill Station via Norwest", leg?.displayText)
    }

    //endregion

    //region helpers

    /**
     * Builds a minimal [TripResponse.Leg] for duration-resolution tests.
     *
     * @param duration   Raw API duration value (null simulates the field being absent).
     * @param depTime    Departure time in UTC ISO-8601.
     * @param arrTime    Arrival time in UTC ISO-8601.
     */
    private fun buildLeg(
        duration: Long?,
        depTime: String,
        arrTime: String,
    ) = TripResponse.Leg(
        duration = duration,
        origin = TripResponse.StopSequence(
            departureTimePlanned = depTime,
            departureTimeEstimated = depTime,
        ),
        destination = TripResponse.StopSequence(
            arrivalTimePlanned = arrTime,
            arrivalTimeEstimated = arrTime,
        ),
    )

    /**
     * Builds a minimal [TripResponse.Journey] with a single public transport leg.
     * The leg has two stops, a duration, and the given transport mode/destination fields.
     * This lets end-to-end [buildJourneyList] tests verify the leg's displayText without JSON fixtures.
     */
    private fun buildJourneyWithTransportLeg(
        productClass: Long,
        destinationName: String?,
        description: String?,
        depTime: String = "2026-04-18T22:00:00Z",
        arrTime: String = "2026-04-18T22:30:00Z",
    ) = TripResponse.Journey(
        legs = listOf(
            TripResponse.Leg(
                duration = 1800L,
                origin = TripResponse.StopSequence(
                    name = "Origin Station",
                    disassembledName = "Origin Station, Platform 1",
                    departureTimePlanned = depTime,
                    departureTimeEstimated = depTime,
                ),
                destination = TripResponse.StopSequence(
                    name = "Destination Station",
                    disassembledName = "Destination Station, Platform 2",
                    arrivalTimePlanned = arrTime,
                    arrivalTimeEstimated = arrTime,
                ),
                stopSequence = listOf(
                    TripResponse.StopSequence(
                        name = "Origin Station",
                        disassembledName = "Origin Station, Platform 1",
                        departureTimePlanned = depTime,
                        departureTimeEstimated = depTime,
                    ),
                    TripResponse.StopSequence(
                        name = "Destination Station",
                        disassembledName = "Destination Station, Platform 2",
                        arrivalTimePlanned = arrTime,
                        arrivalTimeEstimated = arrTime,
                    ),
                ),
                transportation = TripResponse.Transportation(
                    disassembledName = "T1",
                    description = description,
                    destination = destinationName?.let { TripResponse.OperatorClass(name = it) },
                    product = TripResponse.Product(
                        productClass = productClass,
                        name = "Train",
                    ),
                ),
            ),
        ),
    )

    // endregion
}
