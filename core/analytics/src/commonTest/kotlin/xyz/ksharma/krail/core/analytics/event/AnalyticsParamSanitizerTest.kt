package xyz.ksharma.krail.core.analytics.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsParamSanitizerTest {

    @Test
    fun `transit stop id passes through unchanged`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(mapOf("stopId" to "200060"))

        assertEquals("200060", sanitized["stopId"])
    }

    @Test
    fun `address id is hashed`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(mapOf("stopId" to ADDRESS_ID))

        val stopId = sanitized["stopId"] as String
        assertTrue(stopId.startsWith(AnalyticsParamSanitizer.HASHED_ID_PREFIX))
        assertTrue(stopId.length <= AnalyticsParamSanitizer.MAX_PARAM_VALUE_LENGTH)
    }

    @Test
    fun `hashed address id carries none of the address text`() {
        val stopId = AnalyticsParamSanitizer.locationId(ADDRESS_ID)

        assertTrue("Example" !in stopId)
        assertTrue("Parramatta" !in stopId)
        assertTrue("2150" !in stopId)
    }

    @Test
    fun `same address id always hashes to the same value`() {
        assertEquals(
            AnalyticsParamSanitizer.locationId(ADDRESS_ID),
            AnalyticsParamSanitizer.locationId(ADDRESS_ID),
        )
    }

    @Test
    fun `different address ids hash to different values`() {
        val other = ADDRESS_ID.replace("Example St", "Other St")

        assertTrue(
            AnalyticsParamSanitizer.locationId(ADDRESS_ID) !=
                AnalyticsParamSanitizer.locationId(other),
        )
    }

    @Test
    fun `poi and coord ids are hashed too`() {
        listOf("poiID:987654:Museum", "coord:151.2:-33.8:EPSG:4326").forEach { rawId ->
            assertTrue(
                AnalyticsParamSanitizer.locationId(rawId)
                    .startsWith(AnalyticsParamSanitizer.HASHED_ID_PREFIX),
                "expected $rawId to be hashed",
            )
        }
    }

    @Test
    fun `trip endpoint ids are sanitized as well`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(
            mapOf("fromStopId" to ADDRESS_ID, "toStopId" to "200060"),
        )

        assertTrue(
            (sanitized["fromStopId"] as String)
                .startsWith(AnalyticsParamSanitizer.HASHED_ID_PREFIX),
        )
        assertEquals("200060", sanitized["toStopId"])
    }

    @Test
    fun `stop name is redacted when the id is an address`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(
            mapOf("stopId" to ADDRESS_ID, "stopName" to "35 Example St, Parramatta"),
        )

        assertEquals(AnalyticsParamSanitizer.REDACTED_LOCATION_NAME, sanitized["stopName"])
    }

    @Test
    fun `stop name is kept for a transit stop`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(
            mapOf("stopId" to "200060", "stopName" to "Central Station"),
        )

        assertEquals("Central Station", sanitized["stopName"])
    }

    @Test
    fun `any over-long string value is truncated rather than dropped by Firebase`() {
        val long = "a".repeat(250)

        val sanitized = AnalyticsParamSanitizer.sanitize(mapOf("query" to long))

        assertEquals(AnalyticsParamSanitizer.MAX_PARAM_VALUE_LENGTH, (sanitized["query"] as String).length)
    }

    @Test
    fun `non string values are untouched`() {
        val sanitized = AnalyticsParamSanitizer.sanitize(
            mapOf("isRecentSearch" to true, "totalCount" to 7),
        )

        assertEquals(true, sanitized["isRecentSearch"])
        assertEquals(7, sanitized["totalCount"])
    }

    @Test
    fun `event properties are sanitized through the base class`() {
        val event = AnalyticsEvent.StopSelectedEvent(
            stopId = ADDRESS_ID,
            locationKind = AnalyticsEvent.StopSelectedEvent.LocationKind.ADDRESS,
        )

        val stopId = event.properties?.get("stopId") as String
        assertTrue(stopId.length <= AnalyticsParamSanitizer.MAX_PARAM_VALUE_LENGTH)
        assertTrue("Example" !in stopId)
    }

    private companion object {
        // Shape of a real EFA address id: the street, suburb and postcode are plain text
        // and the whole thing is well over Firebase's 100-char parameter limit.
        const val ADDRESS_ID =
            "streetID:1500000015:123:95346013:-1:Example St:Parramatta:Example St::" +
                "Example St:2150:ANY:DIVA_SINGLEHOUSE:4871080:3749205:GDAV:nsw:0"
    }
}
