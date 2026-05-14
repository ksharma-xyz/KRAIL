package xyz.ksharma.krail.feature.track.network

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [buildGtfsUrl]. Covers all three feed-type cases (vehicle
 * positions, v1 trip updates, v2 trip updates) against both override states
 * (NSW direct vs BFF override).
 *
 * Background: the BFF uses identical paths to NSW for all GTFS-RT feeds, so
 * the only thing that changes when [IS_BFF_LOCAL_OVERRIDE_SET] flips is the
 * base URL. This test pins that contract so an accidental edit to the path
 * shape (e.g. dropping the `v2/` prefix) shows up in CI.
 */
class BuildGtfsUrlTest {

    private val nswBaseUrl = "https://api.transport.nsw.gov.au"
    private val bffBaseUrl = "http://10.0.2.2:8080"

    // region: vehicle positions — always v2/vehiclepos, regardless of feed name

    @Test
    fun `Given vehicle positions feed When base URL is NSW Then path is v2 vehiclepos`() {
        val url = buildGtfsUrl(
            baseUrl = nswBaseUrl,
            feedName = "buses",
            feedType = GtfsFeedType.VEHICLE_POSITIONS,
        )

        assertEquals("$nswBaseUrl/v2/gtfs/vehiclepos/buses", url)
    }

    @Test
    fun `Given vehicle positions feed When base URL is BFF Then path is v2 vehiclepos`() {
        val url = buildGtfsUrl(
            baseUrl = bffBaseUrl,
            feedName = "buses",
            feedType = GtfsFeedType.VEHICLE_POSITIONS,
        )

        assertEquals("$bffBaseUrl/v2/gtfs/vehiclepos/buses", url)
    }

    // endregion

    // region: trip updates — v2 for sydneytrains and metro, v1 for everything else

    @Test
    fun `Given v2 trip updates feed When base URL is NSW Then path is v2 realtime`() {
        val url = buildGtfsUrl(
            baseUrl = nswBaseUrl,
            feedName = "sydneytrains",
            feedType = GtfsFeedType.TRIP_UPDATES,
        )

        assertEquals("$nswBaseUrl/v2/gtfs/realtime/sydneytrains", url)
    }

    @Test
    fun `Given v2 trip updates feed When base URL is BFF Then path is v2 realtime`() {
        val url = buildGtfsUrl(
            baseUrl = bffBaseUrl,
            feedName = "metro",
            feedType = GtfsFeedType.TRIP_UPDATES,
        )

        assertEquals("$bffBaseUrl/v2/gtfs/realtime/metro", url)
    }

    @Test
    fun `Given v1 trip updates feed When base URL is NSW Then path is v1 realtime`() {
        val url = buildGtfsUrl(
            baseUrl = nswBaseUrl,
            feedName = "buses",
            feedType = GtfsFeedType.TRIP_UPDATES,
        )

        assertEquals("$nswBaseUrl/v1/gtfs/realtime/buses", url)
    }

    @Test
    fun `Given v1 trip updates feed When base URL is BFF Then path is v1 realtime`() {
        val url = buildGtfsUrl(
            baseUrl = bffBaseUrl,
            feedName = "buses",
            feedType = GtfsFeedType.TRIP_UPDATES,
        )

        assertEquals("$bffBaseUrl/v1/gtfs/realtime/buses", url)
    }

    // endregion
}
