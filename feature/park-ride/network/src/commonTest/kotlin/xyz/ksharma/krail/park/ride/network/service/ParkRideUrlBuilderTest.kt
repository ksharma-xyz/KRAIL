package xyz.ksharma.krail.park.ride.network.service

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the Park & Ride URL helpers used by [RealParkRideService].
 *
 * NSW and BFF speak different path shapes for the same data:
 * - facility list: NSW `/v1/carpark` vs BFF `/v1/parking/facilities`
 * - facility detail: NSW `/v1/carpark?facility=ID` vs BFF
 *   `/v1/parking/facilities/{id}/availability` (id in path, no query param)
 * - batch by stop IDs: BFF only at `/v1/parking/availability?stopIds=...`
 *   (no NSW equivalent)
 *
 * The branching is therefore non-trivial; pinning these strings catches
 * regressions where someone edits one branch and forgets to update the other.
 */
class ParkRideUrlBuilderTest {

    private val nswBaseUrl = "https://api.transport.nsw.gov.au"
    private val bffBaseUrl = "http://10.0.2.2:8080"

    // region: facility list (no-arg overload)

    @Test
    fun `Given override unset When building list URL Then NSW carpark path is returned`() {
        val url = buildParkRideListUrl(
            isBffOverrideSet = false,
            bffBaseUrl = bffBaseUrl,
            nswBaseUrl = nswBaseUrl,
        )

        assertEquals("$nswBaseUrl/v1/carpark", url)
    }

    @Test
    fun `Given override set When building list URL Then BFF parking facilities path is returned`() {
        val url = buildParkRideListUrl(
            isBffOverrideSet = true,
            bffBaseUrl = bffBaseUrl,
            nswBaseUrl = nswBaseUrl,
        )

        assertEquals("$bffBaseUrl/v1/parking/facilities", url)
    }

    // endregion

    // region: facility detail (facilityId overload)

    @Test
    fun `Given override unset When building detail URL Then NSW carpark path is returned`() {
        // NSW uses a query param (?facility={id}) appended by the caller, not
        // baked into the URL — so the base URL is identical to the list URL.
        val url = buildParkRideDetailUrl(
            isBffOverrideSet = false,
            bffBaseUrl = bffBaseUrl,
            nswBaseUrl = nswBaseUrl,
            facilityId = "1",
        )

        assertEquals("$nswBaseUrl/v1/carpark", url)
    }

    @Test
    fun `Given override set When building detail URL Then BFF availability path with facility ID is returned`() {
        val url = buildParkRideDetailUrl(
            isBffOverrideSet = true,
            bffBaseUrl = bffBaseUrl,
            nswBaseUrl = nswBaseUrl,
            facilityId = "1",
        )

        assertEquals("$bffBaseUrl/v1/parking/facilities/1/availability", url)
    }

    @Test
    fun `Given override set and complex facility ID When building detail URL Then path interpolates the ID verbatim`() {
        // NSW facility IDs are numeric in practice; assert the BFF path
        // accepts the same string form without transformation.
        val url = buildParkRideDetailUrl(
            isBffOverrideSet = true,
            bffBaseUrl = bffBaseUrl,
            nswBaseUrl = nswBaseUrl,
            facilityId = "486",
        )

        assertEquals("$bffBaseUrl/v1/parking/facilities/486/availability", url)
    }

    // endregion

    // region: batch by stop IDs (BFF only)

    @Test
    fun `Given any input When building batch by stops URL Then BFF availability path is returned`() {
        // The query string itself is appended by Ktor's `url { parameters.append(...) }`
        // call inside RealParkRideService, so the helper only owns the base path.
        // Pinning that path here protects against typos in the route itself.
        val url = buildParkRideBatchByStopsUrl(bffBaseUrl = bffBaseUrl)

        assertEquals("$bffBaseUrl/v1/parking/availability", url)
    }

    @Test
    fun `Given a different BFF host When building batch by stops URL Then host is interpolated verbatim`() {
        // Sanity check that the helper does not hard-code 10.0.2.2: iOS Simulator
        // talks to localhost, not 10.0.2.2, and a future production deploy will
        // use a real hostname.
        val url = buildParkRideBatchByStopsUrl(bffBaseUrl = "http://localhost:8080")

        assertEquals("http://localhost:8080/v1/parking/availability", url)
    }

    @Test
    fun `Given full BFF batch URL with two stop IDs joined Then matches the canonical Android emulator form`() {
        // The full URL the brief asks us to pin: base + path + joined stopIds.
        // Ktor appends the query string at request time; we mirror that here
        // by joining manually so the assertion is on a real string match.
        val base = buildParkRideBatchByStopsUrl(bffBaseUrl = "http://10.0.2.2:8080")
        val stopIds = listOf("275010", "2155384")
        val full = "$base?stopIds=${stopIds.joinToString(",")}"

        assertEquals(
            "http://10.0.2.2:8080/v1/parking/availability?stopIds=275010,2155384",
            full,
        )
    }

    // endregion

    // region: stop-ID cap helper

    @Test
    fun `Given stop IDs at the cap When capping Then the same list is returned unchanged`() {
        val ids = (1..MAX_STOP_IDS_PER_BATCH).map { it.toString() }

        val capped = capStopIdsForBatch(ids)

        assertEquals(ids, capped)
    }

    @Test
    fun `Given stop IDs over the cap When capping Then the first MAX_STOP_IDS_PER_BATCH are kept`() {
        // Build a 25-element list: cap is 20, so we expect the first 20.
        val ids = (1..25).map { it.toString() }

        val capped = capStopIdsForBatch(ids)

        assertEquals(MAX_STOP_IDS_PER_BATCH, capped.size)
        assertEquals(ids.take(MAX_STOP_IDS_PER_BATCH), capped)
    }

    @Test
    fun `Given an empty list When capping Then an empty list is returned`() {
        val capped = capStopIdsForBatch(emptyList())

        assertEquals(emptyList(), capped)
    }

    // endregion
}
