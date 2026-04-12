package xyz.ksharma.krail.departures.ui.business

import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
