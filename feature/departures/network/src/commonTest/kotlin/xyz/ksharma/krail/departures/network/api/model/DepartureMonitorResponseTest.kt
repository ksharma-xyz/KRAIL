package xyz.ksharma.krail.departures.network.api.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DepartureMonitorResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    // region: happy path

    @Test
    fun `Given valid response When parsed Then stopEvents count is correct`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)

        assertEquals(3, response.stopEvents?.size)
    }

    @Test
    fun `Given stopEvent with estimatedTime When parsed Then estimatedTime takes priority over planned`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        val event = response.stopEvents?.first()

        assertEquals("2026-04-08T09:00:00Z", event?.departureTimePlanned)
        assertEquals("2026-04-08T09:03:00Z", event?.departureTimeEstimated)
    }

    @Test
    fun `Given stopEvent without estimatedTime When parsed Then estimatedTime is null`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        val event = response.stopEvents?.get(1)

        assertEquals("2026-04-08T09:05:00Z", event?.departureTimePlanned)
        assertNull(event?.departureTimeEstimated)
    }

    @Test
    fun `Given stopEvent When parsed Then transportation fields are mapped correctly`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        val transportation = response.stopEvents?.first()?.transportation

        assertNotNull(transportation)
        assertEquals("T1", transportation.disassembledName)
        assertEquals("Cronulla Station", transportation.destination?.name)
        assertEquals(1, transportation.product?.cls)
        assertEquals("Train", transportation.product?.name)
        assertEquals("sydneytrains", transportation.operator?.id)
    }

    @Test
    fun `Given stopEvent When parsed Then location platform is mapped correctly`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        val location = response.stopEvents?.first()?.location

        assertNotNull(location)
        assertEquals("Platform 1", location.disassembledName)
        assertEquals("Town Hall Station", location.parent?.name)
    }

    @Test
    fun `Given stopEvent without platform info When parsed Then disassembledName falls back to name`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        // Third event has no platform — disassembledName equals the stop name
        val location = response.stopEvents?.get(2)?.location

        assertNotNull(location)
        assertEquals("Town Hall", location.disassembledName)
    }

    @Test
    fun `Given Metro stopEvent When parsed Then product class is 2`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(SAMPLE_RESPONSE)
        val metro = response.stopEvents?.get(2)?.transportation

        assertEquals(2, metro?.product?.cls)
        assertEquals("M1", metro?.disassembledName)
    }

    // region: error response

    @Test
    fun `Given error response When parsed Then error message is present and stopEvents is null`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(ERROR_RESPONSE)

        assertNotNull(response.error)
        assertEquals("Stop not found", response.error.message)
        assertTrue(response.stopEvents.isNullOrEmpty())
    }

    // region: empty / unknown

    @Test
    fun `Given empty stopEvents When parsed Then list is empty`() {
        val response = json.decodeFromString<DepartureMonitorResponse>(EMPTY_STOP_EVENTS_RESPONSE)

        assertTrue(response.stopEvents?.isEmpty() == true)
    }

    @Test
    fun `Given unknown keys in JSON When parsed Then no exception is thrown`() {
        // The lenient Json config (ignoreUnknownKeys) should handle extra fields gracefully
        val response = json.decodeFromString<DepartureMonitorResponse>(EXTRA_FIELDS_RESPONSE)

        assertEquals(1, response.stopEvents?.size)
    }

    // region: test data

    companion object {
        private val SAMPLE_RESPONSE = """
            {
              "version": "10.6.14.22",
              "stopEvents": [
                {
                  "departureTimePlanned": "2026-04-08T09:00:00Z",
                  "departureTimeEstimated": "2026-04-08T09:03:00Z",
                  "location": {
                    "id": "10111012",
                    "name": "Town Hall Station, Platform 1",
                    "disassembledName": "Platform 1",
                    "parent": {
                      "id": "10111010",
                      "name": "Town Hall Station",
                      "disassembledName": "Town Hall"
                    }
                  },
                  "transportation": {
                    "id": "sydneytrains:T1:H",
                    "name": "Train: Rouse Hill to Cronulla",
                    "disassembledName": "T1",
                    "number": "T1",
                    "description": "Rouse Hill to Cronulla",
                    "destination": { "id": "10101116", "name": "Cronulla Station" },
                    "product": { "class": 1, "iconId": 1, "name": "Train" },
                    "operator": { "id": "sydneytrains", "name": "Sydney Trains" }
                  }
                },
                {
                  "departureTimePlanned": "2026-04-08T09:05:00Z",
                  "location": {
                    "id": "10111014",
                    "name": "Town Hall Station, Platform 3",
                    "disassembledName": "Platform 3",
                    "parent": {
                      "id": "10111010",
                      "name": "Town Hall Station",
                      "disassembledName": "Town Hall"
                    }
                  },
                  "transportation": {
                    "id": "sydneytrains:T2:H",
                    "name": "Train: Campbelltown to Macdonaldtown",
                    "disassembledName": "T2",
                    "destination": { "id": "10102040", "name": "Macdonaldtown Station" },
                    "product": { "class": 1, "iconId": 1, "name": "Train" },
                    "operator": { "id": "sydneytrains", "name": "Sydney Trains" }
                  }
                },
                {
                  "departureTimePlanned": "2026-04-08T09:08:00Z",
                  "departureTimeEstimated": "2026-04-08T09:08:00Z",
                  "location": {
                    "id": "200070",
                    "name": "Town Hall Station",
                    "disassembledName": "Town Hall",
                    "parent": {
                      "id": "200070",
                      "name": "Town Hall Station",
                      "disassembledName": "Town Hall"
                    }
                  },
                  "transportation": {
                    "id": "sydneymetro:M1:H",
                    "name": "Metro: Tallawong to Sydenham",
                    "disassembledName": "M1",
                    "destination": { "id": "10111199", "name": "Sydenham Station" },
                    "product": { "class": 2, "iconId": 2, "name": "Metro" },
                    "operator": { "id": "sydneymetro", "name": "Sydney Metro" }
                  }
                }
              ]
            }
        """.trimIndent()

        private val ERROR_RESPONSE = """
            {
              "error": {
                "message": "Stop not found",
                "versions": {
                  "controller": "10.6.14.22",
                  "interfaceMax": "10.6.14.22",
                  "interfaceMin": "10.2.1.42"
                }
              }
            }
        """.trimIndent()

        private val EMPTY_STOP_EVENTS_RESPONSE = """
            {
              "version": "10.6.14.22",
              "stopEvents": []
            }
        """.trimIndent()

        private val EXTRA_FIELDS_RESPONSE = """
            {
              "version": "10.6.14.22",
              "unknownTopLevelField": "should be ignored",
              "stopEvents": [
                {
                  "departureTimePlanned": "2026-04-08T09:00:00Z",
                  "unknownEventField": 99,
                  "location": {
                    "id": "10111012",
                    "name": "Town Hall Station, Platform 1",
                    "disassembledName": "Platform 1",
                    "unknownLocationField": true
                  },
                  "transportation": {
                    "id": "sydneytrains:T1:H",
                    "disassembledName": "T1",
                    "unknownTransportField": []
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
