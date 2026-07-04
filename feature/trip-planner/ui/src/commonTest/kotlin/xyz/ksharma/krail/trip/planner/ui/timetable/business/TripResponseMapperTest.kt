package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.OranParkToSevenHillsFixture
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.Redfern715BacktrackFixture
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

    // Mirrors the Json config Ktor actually uses in production (core/network/HttpClient.kt,
    // core/remote-config/JsonConfig.kt) - isLenient is required because NSW sends some
    // String-typed fields (e.g. Transportation.properties.tripCode) as unquoted JSON numbers.
    // Without it, a real captured response (see Redfern715BacktrackFixture) fails to parse
    // here even though the app decodes it fine, which would be a false test failure.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
    fun `resolveDurationSeconds returns null for zero-second duration with same timestamps`() {
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

    //region journeyId uniqueness

    /**
     * Regression test for the deduplication bug introduced when [buildJourneyList] was
     * changed to set `tripId = transportation?.id` (without appending `RealtimeTripId`).
     *
     * When multiple trips run on the same route (e.g., T1 trains at 10:33, 10:48, 11:03),
     * they share the same `transportation.id` (e.g., "nsw:020T1:W:H:sj2"). With just the
     * stable ID as `tripId`, [JourneyCardInfo.journeyId] collapses to the same string for
     * all of them, and the `associateBy { it.journeyId }` map in [updateTripsCache] keeps
     * only the last entry — silently dropping the rest from the UI.
     *
     * The fix: `tripId = transportation.id + RealtimeTripId` (unique per scheduled run),
     * with a separate `transportationId = transportation.id` used for tracking deep links.
     */
    @Test
    fun `buildJourneyList returns distinct journeys for same route at different departure times`() {
        val sharedTransportationId = "nsw:020T1:W:H:sj2"
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    transportationId = sharedTransportationId,
                    realtimeTripId = "tripA111",
                    depTime = "2026-04-20T12:33:00Z",
                    arrTime = "2026-04-20T13:06:00Z",
                ),
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    transportationId = sharedTransportationId,
                    realtimeTripId = "tripB222",
                    depTime = "2026-04-20T12:48:00Z",
                    arrTime = "2026-04-20T13:21:00Z",
                ),
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    transportationId = sharedTransportationId,
                    realtimeTripId = "tripC333",
                    depTime = "2026-04-20T13:03:00Z",
                    arrTime = "2026-04-20T13:36:00Z",
                ),
            ),
        )

        val journeys = response.buildJourneyList()

        assertEquals(3, journeys?.size, "All same-route trips must survive — must not be deduplicated by shared transportation.id")
        val journeyIds = journeys?.map { it.journeyId }
        assertEquals(3, journeyIds?.distinct()?.size, "Each journey must produce a unique journeyId")
    }

    /**
     * Verifies that [transportationId] on each [TransportLeg] is set to [Transportation.id]
     * only (the stable timetable ID), separate from the full [tripId] which also includes
     * the volatile [RealtimeTripId] for deduplication.
     */
    @Test
    fun `buildJourneyList sets transportationId to transportation id only`() {
        val transportationId = "nsw:020T1:W:H:sj2"
        val realtimeTripId = "tripX999"
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    transportationId = transportationId,
                    realtimeTripId = realtimeTripId,
                ),
            ),
        )

        val leg = response.buildJourneyList()
            ?.firstOrNull()?.legs?.firstOrNull() as? TimeTableState.JourneyCardInfo.Leg.TransportLeg

        assertEquals(transportationId, leg?.transportationId, "transportationId must be transportation.id only")
        assertEquals("$transportationId$realtimeTripId", leg?.tripId, "tripId must be transportation.id + RealtimeTripId for uniqueness")
    }

    //endregion

    //region totalUniqueServiceAlerts

    @Test
    fun `Given leg with no infos When buildJourneyList Then totalUniqueServiceAlerts is 0`() {
        val response = TripResponse(
            journeys = listOf(buildJourneyWithTransportLeg(productClass = 1L, infos = null)),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(0, journey?.totalUniqueServiceAlerts)
    }

    @Test
    fun `Given leg with fully valid infos When buildJourneyList Then totalUniqueServiceAlerts matches`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    infos = listOf(
                        TripResponse.Info(subtitle = "Delays", content = "Train delayed", priority = "high"),
                        TripResponse.Info(subtitle = "Works", content = "Track works", priority = "normal"),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(2, journey?.totalUniqueServiceAlerts)
    }

    @Test
    fun `Given leg with info missing subtitle When buildJourneyList Then that info is not counted`() {
        // Regression: before the fix, totalUniqueServiceAlerts counted raw infos regardless of
        // whether toAlert() could parse them. An info with null subtitle is dropped by toAlert()
        // but was still included in the badge count, so the alert bottom sheet never opened.
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    infos = listOf(
                        TripResponse.Info(subtitle = null, content = "Some content", priority = "high"),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(0, journey?.totalUniqueServiceAlerts)
    }

    @Test
    fun `Given leg with info missing content When buildJourneyList Then that info is not counted`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    infos = listOf(
                        TripResponse.Info(subtitle = "Delays", content = null, priority = "normal"),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(0, journey?.totalUniqueServiceAlerts)
    }

    @Test
    fun `Given leg with info with unrecognised priority When buildJourneyList Then that info is not counted`() {
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    infos = listOf(
                        TripResponse.Info(subtitle = "Notice", content = "Some notice", priority = "unknown"),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(0, journey?.totalUniqueServiceAlerts)
    }

    @Test
    fun `Given mix of valid and invalid infos When buildJourneyList Then only valid ones are counted`() {
        // Core regression: badge showed 3 alerts but serviceAlertList was empty (or fewer),
        // so the bottom sheet guarded by alerts.isNotEmpty() never opened.
        val response = TripResponse(
            journeys = listOf(
                buildJourneyWithTransportLeg(
                    productClass = 1L,
                    infos = listOf(
                        TripResponse.Info(subtitle = "Delays", content = "Train delayed", priority = "high"),
                        TripResponse.Info(subtitle = null, content = "Missing heading", priority = "normal"),
                        TripResponse.Info(subtitle = "Works", content = null, priority = "low"),
                        TripResponse.Info(subtitle = "Notice", content = "Info notice", priority = "unknown"),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()
        val transportLeg = journey?.legs?.firstOrNull() as? TimeTableState.JourneyCardInfo.Leg.TransportLeg

        assertEquals(
            1,
            journey?.totalUniqueServiceAlerts,
            "Badge count must match the number of alerts serviceAlertList actually contains",
        )
        assertEquals(
            journey?.totalUniqueServiceAlerts,
            transportLeg?.serviceAlertList?.size,
            "totalUniqueServiceAlerts must equal serviceAlertList size so the bottom sheet opens",
        )
    }

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

    //region collapseSameRouteQuickWalks (715 / walk / 715 backtrack)
    //
    // Full background: docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md
    //
    // User complaint: a trip shows as "715 bus / walk / 715 bus / T1 train" (3 transit legs)
    // instead of "715 bus / T1 train", while Google Maps and Opal both show it as the latter.
    // Root cause, confirmed from a real captured API response: NSW splits a 715 that reverses
    // direction one stop from the origin into two separate scheduled trips (different tripCode,
    // opposite-direction description) connected by a genuine but trivial (60s) road-crossing
    // walk. [collapseSameRouteQuickWalks] in TripResponseMapper.kt collapses that pattern back
    // into one displayed leg, matching what other apps show, without claiming to prove it's the
    // same physical vehicle (it deliberately does NOT compare tripCode/RealtimeTripId/direction —
    // those legitimately differ here, which is exactly the case being handled).
    //
    // The first test below decodes the real captured JSON (Redfern715BacktrackFixture) so it
    // also catches API shape drift, not just mapper logic regressions. The other two tests pin
    // the heuristic's boundaries (walk too long / route number differs) with synthetic data,
    // since the real capture doesn't happen to contain those variants.

    /**
     * Regression test for the "715 / walk / 715 / T1" complaint, using the real API response
     * that reproduces it (see [Redfern715BacktrackFixture] for full provenance/background).
     * Decoding real JSON rather than hand-building a [TripResponse.Leg] tree also means this
     * test breaks if NSW's field shape changes in a way the mapper doesn't handle, not just if
     * the collapse logic itself regresses.
     */
    @Test
    fun `buildJourneyList collapses same-route legs separated by a quick walk`() {
        // Arrange: real captured response — leg0 715 tripCode 77 (outbound) -> 60s IDEST walk
        // across the road -> leg2 715 tripCode 15 (inbound) -> leg3 T1.
        val response = json.decodeFromString<TripResponse>(Redfern715BacktrackFixture.JSON)

        // Act
        val journey = response.buildJourneyList()?.firstOrNull()
        val legs = journey?.legs

        // Assert
        assertEquals(2, legs?.size, "The two 715 legs and the quick walk must collapse into one leg")
        val mergedLeg = legs?.getOrNull(0) as? TimeTableState.JourneyCardInfo.Leg.TransportLeg
        assertEquals("715", mergedLeg?.transportModeLine?.lineName)
        assertEquals(
            "6 mins",
            mergedLeg?.totalDuration,
            "Merged duration must span the real door-to-door window (00:03:00 -> 00:09:42 " +
                "estimated), not just the first leg's own 60s ride",
        )
        assertEquals(
            10,
            mergedLeg?.stops?.size,
            "Stops from both real 715 legs (2 + 8) must be combined",
        )
        assertEquals(
            null,
            journey?.totalWalkTime,
            "The collapsed quick walk must not be counted as walking time",
        )
    }

    /**
     * Synthetic edge case (not present in the real capture): pins the 120s quick-walk cutoff.
     * A longer walk is a real interchange a user needs to know about — e.g. enough time to
     * actually leave the stop precinct — and must never silently disappear into a merged leg.
     */
    @Test
    fun `buildJourneyList does not collapse same-route legs when the walk exceeds the quick-walk threshold`() {
        val response = TripResponse(
            journeys = listOf(
                TripResponse.Journey(
                    legs = listOf(
                        buildRouteLeg(
                            routeNumber = "715",
                            realtimeTripId = "trip-outbound-77",
                            depTime = "2026-04-18T22:00:00Z",
                            arrTime = "2026-04-18T22:01:00Z",
                        ),
                        buildQuickWalkLeg(
                            depTime = "2026-04-18T22:01:00Z",
                            arrTime = "2026-04-18T22:03:30Z",
                            durationSeconds = 150L, // exceeds the 120s quick-walk threshold
                        ),
                        buildRouteLeg(
                            routeNumber = "715",
                            realtimeTripId = "trip-inbound-15",
                            depTime = "2026-04-18T22:03:30Z",
                            arrTime = "2026-04-18T22:06:00Z",
                        ),
                        buildRouteLeg(
                            routeNumber = "T1",
                            realtimeTripId = "trip-t1",
                            depTime = "2026-04-18T22:10:00Z",
                            arrTime = "2026-04-18T22:40:00Z",
                            productClass = 1L,
                        ),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(
            4,
            journey?.legs?.size,
            "A walk longer than the quick-walk threshold is a real interchange and must stay separate",
        )
        assertEquals(
            true,
            journey?.totalWalkTime != null,
            "A real (non-collapsed) walking leg must still be counted as walking time",
        )
    }

    /**
     * Synthetic edge case (not present in the real capture): pins that route number is the
     * *only* merge signal, not a coincidence of this fixture. A genuine route change (e.g.
     * transferring from a 715 to a 718) must always render as two legs even if the connecting
     * walk happens to be trivially short.
     */
    @Test
    fun `buildJourneyList does not collapse legs on different route numbers even with a quick walk`() {
        val response = TripResponse(
            journeys = listOf(
                TripResponse.Journey(
                    legs = listOf(
                        buildRouteLeg(
                            routeNumber = "715",
                            realtimeTripId = "trip-715",
                            depTime = "2026-04-18T22:00:00Z",
                            arrTime = "2026-04-18T22:01:00Z",
                        ),
                        buildQuickWalkLeg(
                            depTime = "2026-04-18T22:01:00Z",
                            arrTime = "2026-04-18T22:02:00Z",
                            durationSeconds = 60L,
                        ),
                        buildRouteLeg(
                            routeNumber = "718", // different route number - must not collapse
                            realtimeTripId = "trip-718",
                            depTime = "2026-04-18T22:02:00Z",
                            arrTime = "2026-04-18T22:05:00Z",
                        ),
                        buildRouteLeg(
                            routeNumber = "T1",
                            realtimeTripId = "trip-t1",
                            depTime = "2026-04-18T22:10:00Z",
                            arrTime = "2026-04-18T22:40:00Z",
                            productClass = 1L,
                        ),
                    ),
                ),
            ),
        )

        val journey = response.buildJourneyList()?.firstOrNull()

        assertEquals(
            4,
            journey?.legs?.size,
            "A route change (715 -> 718) is a real interchange and must never be collapsed",
        )
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
        destinationName: String? = "Central",
        description: String? = null,
        transportationId: String = "nsw:020T1:W:H:sj2",
        realtimeTripId: String = "defaultRtId",
        depTime: String = "2026-04-18T22:00:00Z",
        arrTime: String = "2026-04-18T22:30:00Z",
        infos: List<TripResponse.Info>? = null,
    ) = TripResponse.Journey(
        legs = listOf(
            TripResponse.Leg(
                duration = 1800L,
                infos = infos,
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
                    id = transportationId,
                    disassembledName = "T1",
                    description = description,
                    destination = destinationName?.let { TripResponse.OperatorClass(name = it) },
                    product = TripResponse.Product(
                        productClass = productClass,
                        name = "Train",
                    ),
                    properties = TripResponse.TransportationProperties(
                        realtimeTripId = realtimeTripId,
                    ),
                ),
            ),
        ),
    )

    // endregion

    //region collapseSameRouteQuickWalks helpers

    /**
     * Builds a [TripResponse.Leg] for a transport leg identified by [routeNumber], with two
     * stops spanning [depTime] to [arrTime]. Mirrors the shape a same-numbered bus leg has in
     * the real NSW response (see `home-redfern-investigate.json`, journey index 1).
     */
    private fun buildRouteLeg(
        routeNumber: String,
        realtimeTripId: String,
        depTime: String,
        arrTime: String,
        productClass: Long = 5L,
    ) = TripResponse.Leg(
        origin = TripResponse.StopSequence(
            name = "$routeNumber origin",
            departureTimePlanned = depTime,
            departureTimeEstimated = depTime,
        ),
        destination = TripResponse.StopSequence(
            name = "$routeNumber destination",
            arrivalTimePlanned = arrTime,
            arrivalTimeEstimated = arrTime,
        ),
        stopSequence = listOf(
            TripResponse.StopSequence(
                name = "$routeNumber origin",
                departureTimePlanned = depTime,
                departureTimeEstimated = depTime,
            ),
            TripResponse.StopSequence(
                name = "$routeNumber destination",
                arrivalTimePlanned = arrTime,
                arrivalTimeEstimated = arrTime,
            ),
        ),
        transportation = TripResponse.Transportation(
            id = "nsw:$routeNumber:$realtimeTripId",
            number = routeNumber,
            disassembledName = routeNumber,
            description = "$routeNumber towards somewhere",
            destination = TripResponse.OperatorClass(name = "Terminus"),
            product = TripResponse.Product(productClass = productClass, name = "Sydney Buses Network"),
            properties = TripResponse.TransportationProperties(realtimeTripId = realtimeTripId),
        ),
    )

    /**
     * Builds a standalone walking [TripResponse.Leg] (`productClass = 99`, `position = IDEST`)
     * of [durationSeconds] — the shape of the "cross the road" leg between two same-numbered
     * bus trips in the real NSW response.
     */
    private fun buildQuickWalkLeg(
        depTime: String,
        arrTime: String,
        durationSeconds: Long,
    ) = TripResponse.Leg(
        duration = durationSeconds,
        origin = TripResponse.StopSequence(
            departureTimePlanned = depTime,
            departureTimeEstimated = depTime,
        ),
        destination = TripResponse.StopSequence(
            arrivalTimePlanned = arrTime,
            arrivalTimeEstimated = arrTime,
        ),
        stopSequence = listOf(
            TripResponse.StopSequence(departureTimePlanned = depTime, departureTimeEstimated = depTime),
            TripResponse.StopSequence(arrivalTimePlanned = arrTime, arrivalTimeEstimated = arrTime),
        ),
        transportation = TripResponse.Transportation(
            product = TripResponse.Product(productClass = 99L, name = "footpath"),
        ),
        footPathInfo = listOf(
            TripResponse.FootPathInfo(duration = durationSeconds, position = "IDEST"),
        ),
    )

    //endregion
}
