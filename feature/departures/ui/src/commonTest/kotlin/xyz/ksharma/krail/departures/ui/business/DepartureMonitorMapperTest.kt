package xyz.ksharma.krail.departures.ui.business

import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DepartureMonitorMapperTest {

    // region: real-time vs planned time

    @Test
    fun `Given event with estimated time When mapped Then departureUtcDateTime uses estimated`() {
        val event = buildStopEvent(
            plannedTime = "2026-04-08T09:00:00Z",
            estimatedTime = "2026-04-08T09:03:00Z",
        )

        val result = event.toStopDeparture()

        assertEquals("2026-04-08T09:03:00Z", result?.departureUtcDateTime)
    }

    @Test
    fun `Given event without estimated time When mapped Then departureUtcDateTime uses planned`() {
        val event = buildStopEvent(
            plannedTime = "2026-04-08T09:05:00Z",
            estimatedTime = null,
        )

        val result = event.toStopDeparture()

        assertEquals("2026-04-08T09:05:00Z", result?.departureUtcDateTime)
    }

    @Test
    fun `Given event with estimated time When mapped Then isRealTime is true`() {
        val event = buildStopEvent(estimatedTime = "2026-04-08T09:03:00Z")

        val result = event.toStopDeparture()

        assertTrue(result?.isRealTime == true)
    }

    @Test
    fun `Given event without estimated time When mapped Then isRealTime is false`() {
        val event = buildStopEvent(estimatedTime = null)

        val result = event.toStopDeparture()

        assertFalse(result?.isRealTime == true)
    }

    @Test
    fun `Given event with no departure time When mapped Then result is null`() {
        val event = buildStopEvent(plannedTime = null, estimatedTime = null)

        val result = event.toStopDeparture()

        assertNull(result)
    }

    // region: line colour

    @Test
    fun `Given T1 line When mapped Then lineColorCode is T1 orange`() {
        val event = buildStopEvent(lineNumber = "T1", productClass = 1)

        assertEquals("#F99D1C", event.toStopDeparture()?.lineColorCode)
    }

    @Test
    fun `Given T4 line When mapped Then lineColorCode is T4 blue`() {
        val event = buildStopEvent(lineNumber = "T4", productClass = 1)

        assertEquals("#005AA3", event.toStopDeparture()?.lineColorCode)
    }

    @Test
    fun `Given F1 ferry line When mapped Then lineColorCode is F1 green`() {
        val event = buildStopEvent(lineNumber = "F1", productClass = 9)

        assertEquals("#00774B", event.toStopDeparture()?.lineColorCode)
    }

    @Test
    fun `Given unknown line with train product class When mapped Then falls back to train colour`() {
        val event = buildStopEvent(lineNumber = "XYZ", productClass = 1)

        assertEquals("#F6891F", event.toStopDeparture()?.lineColorCode)
    }

    @Test
    fun `Given unknown line with null product class When mapped Then falls back to default colour`() {
        val event = buildStopEvent(lineNumber = "XYZ", productClass = null)

        assertEquals("#00B5EF", event.toStopDeparture()?.lineColorCode)
    }

    // region: transport mode name

    @Test
    fun `Given product class 1 When mapped Then transportModeName is Train`() {
        val event = buildStopEvent(productClass = 1)

        assertEquals("Train", event.toStopDeparture()?.transportModeName)
    }

    @Test
    fun `Given product class 2 When mapped Then transportModeName is Metro`() {
        val event = buildStopEvent(productClass = 2)

        assertEquals("Metro", event.toStopDeparture()?.transportModeName)
    }

    @Test
    fun `Given product class 9 When mapped Then transportModeName is Ferry`() {
        val event = buildStopEvent(productClass = 9)

        assertEquals("Ferry", event.toStopDeparture()?.transportModeName)
    }

    @Test
    fun `Given unknown product class When mapped Then transportModeName falls back to Bus`() {
        val event = buildStopEvent(productClass = 99)

        assertEquals("Bus", event.toStopDeparture()?.transportModeName)
    }

    // region: platform text
    // --- baseline (no properties) ---

    @Test
    fun `Given location with distinct platform label When mapped Then platformText is set`() {
        val event = buildStopEvent(
            locationDisassembledName = "Platform 1",
            parentDisassembledName = "Town Hall",
        )

        assertEquals("Platform 1", event.toStopDeparture()?.platformText)
    }

    @Test
    fun `Given location matching parent name When mapped Then platformText is null`() {
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall",
            parentDisassembledName = "Town Hall",
        )

        assertNull(event.toStopDeparture()?.platformText)
    }

    @Test
    fun `Given location with no parent When mapped Then platformText is null`() {
        val event = buildStopEvent(
            locationDisassembledName = "Platform 1",
            parentDisassembledName = null,
        )

        assertNull(event.toStopDeparture()?.platformText)
    }

    // --- full compound disassembledName (as seen in the live API, no properties) ---

    @Test
    fun `Given train location with full compound disassembledName When mapped Then platformText extracts Platform label`() {
        // Live API returns "Town Hall Station, Platform 1, Sydney" — not just "Platform 1".
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Platform 1, Sydney",
            parentDisassembledName = "Town Hall Station",
        )

        assertEquals("Platform 1", event.toStopDeparture()?.platformText)
    }

    // --- properties.platformName is a real label (differs from platform code) ---
    // The regex is run on platformName first; the full disassembledName is NOT used.

    @Test
    fun `Given THL train platform with clean platformName When mapped Then platformText is Platform N`() {
        // Typical case: platform="THL6", platformName="Platform 6"
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Platform 6, Sydney",
            parentDisassembledName = "Town Hall Station",
            locationPlatformCode = "THL6",
            locationPlatformName = "Platform 6",
        )

        assertEquals("Platform 6", event.toStopDeparture()?.platformText)
    }

    @Test
    fun `Given bus location with compound platformName containing Stand J When mapped Then platformText is Stand J`() {
        // Live API: platform="J", platformName="Town Hall, Park St, Stand J"
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall, Park St, Stand J, Sydney",
            parentDisassembledName = "Town Hall",
            locationPlatformCode = "J",
            locationPlatformName = "Town Hall, Park St, Stand J",
        )

        assertEquals("Stand J", event.toStopDeparture()?.platformText)
    }

    @Test
    fun `Given light rail location with platformName Town Hall Light Rail When mapped Then platformText includes code and name`() {
        // Live API: platform="LR1", platformName="Town Hall Light Rail", cls=4 (Light Rail)
        // Mode-aware rule → "$pCode · $pName"
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Town Hall Light Rail, Sydney",
            parentDisassembledName = "Town Hall Station",
            locationPlatformCode = "LR1",
            locationPlatformName = "Town Hall Light Rail",
            productClass = 4,
        )

        assertEquals("LR1 · Town Hall Light Rail", event.toStopDeparture()?.platformText)
    }

    @Test
    fun `Given light rail location with missing platform key and platformName Town Hall Light Rail When mapped Then platformText is name only`() {
        // Live API occasionally omits the platform code but keeps platformName; cls=4 (Light Rail).
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Town Hall Light Rail, Sydney",
            parentDisassembledName = "Town Hall Station",
            locationPlatformCode = null,
            locationPlatformName = "Town Hall Light Rail",
            productClass = 4,
        )

        assertEquals("Town Hall Light Rail", event.toStopDeparture()?.platformText)
    }

    // --- properties.platformName equals raw platform code (API inconsistency) ---

    @Test
    fun `Given THL platform where platformName echoes raw code THL6 When mapped Then platformText derives Platform 6 from code`() {
        // Inconsistent API response: platform="THL6", platformName="THL6"
        // Number is extracted from the code and formatted as "Platform N".
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Platform 6, Sydney",
            parentDisassembledName = "Town Hall Station",
            locationPlatformCode = "THL6",
            locationPlatformName = "THL6",
        )

        assertEquals("Platform 6", event.toStopDeparture()?.platformText)
    }

    // --- no properties at all ---

    @Test
    fun `Given light rail location with no properties When mapped Then platformText is null`() {
        // Without properties there is no fallback label for Light Rail names.
        val event = buildStopEvent(
            locationDisassembledName = "Town Hall Station, Town Hall Light Rail, Sydney",
            parentDisassembledName = "Town Hall Station",
        )

        assertNull(event.toStopDeparture()?.platformText)
    }

    // region: destination name

    @Test
    fun `Given transportation with destination When mapped Then destinationName is correct`() {
        val event = buildStopEvent(destinationName = "Cronulla Station")

        assertEquals("Cronulla Station", event.toStopDeparture()?.destinationName)
    }

    @Test
    fun `Given transportation with no destination When mapped Then destinationName is empty`() {
        val event = buildStopEvent(destinationName = null)

        assertEquals("", event.toStopDeparture()?.destinationName)
    }

    // region: response-level mapping

    @Test
    fun `Given response with multiple events When mapped Then all valid events are returned`() {
        val response = DepartureMonitorResponse(
            stopEvents = listOf(
                buildStopEvent(plannedTime = "2026-04-08T09:00:00Z"),
                buildStopEvent(plannedTime = "2026-04-08T09:05:00Z"),
                buildStopEvent(plannedTime = null, estimatedTime = null), // invalid — skipped
            ),
        )

        val result = response.toStopDepartures()

        assertEquals(2, result.size)
    }

    @Test
    fun `Given response with null stopEvents When mapped Then result is empty`() {
        val response = DepartureMonitorResponse(stopEvents = null)

        assertTrue(response.toStopDepartures().isEmpty())
    }

    // region: test builders

    private fun buildStopEvent(
        plannedTime: String? = "2026-04-08T09:00:00Z",
        estimatedTime: String? = null,
        lineNumber: String = "T1",
        productClass: Int? = 1,
        destinationName: String? = "Cronulla Station",
        locationDisassembledName: String? = "Platform 1",
        parentDisassembledName: String? = "Town Hall",
        /** Mirrors `location.properties.platformName` from the live NSW Transport API. */
        locationPlatformName: String? = null,
        /** Mirrors `location.properties.platform` (raw code, e.g. "THL6", "J", "LR1"). */
        locationPlatformCode: String? = null,
    ) = DepartureMonitorResponse.StopEvent(
        departureTimePlanned = plannedTime,
        departureTimeEstimated = estimatedTime,
        location = DepartureMonitorResponse.Location(
            id = "10111012",
            name = "Town Hall Station",
            disassembledName = locationDisassembledName,
            parent = parentDisassembledName?.let {
                DepartureMonitorResponse.Parent(
                    id = "10111010",
                    name = "Town Hall Station",
                    disassembledName = it,
                )
            },
            properties = if (locationPlatformName != null || locationPlatformCode != null) {
                DepartureMonitorResponse.LocationProperties(
                    platformName = locationPlatformName,
                    platform = locationPlatformCode,
                )
            } else null,
        ),
        transportation = DepartureMonitorResponse.Transportation(
            id = "sydneytrains:$lineNumber:H",
            disassembledName = lineNumber,
            destination = destinationName?.let {
                DepartureMonitorResponse.Destination(id = "stop_id", name = it)
            },
            product = productClass?.let {
                DepartureMonitorResponse.Product(cls = it, iconId = it, name = "")
            },
        ),
    )
}
