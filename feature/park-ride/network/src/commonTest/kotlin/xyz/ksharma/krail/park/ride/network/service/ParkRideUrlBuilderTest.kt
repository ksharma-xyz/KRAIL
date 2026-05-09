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
}
