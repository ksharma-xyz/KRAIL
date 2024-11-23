package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTextTest {

    @Test
    fun testGetPlatformText() {
        val disassembledNameSamples = listOf(
            "Central Station, Platform 16" to "Platform 16",
            "Central Station, Platform 24" to "Platform 24",
            "Central Station, Platform 17" to "Platform 17",
            null to null,
            "Central Station" to null,
            "Erskineville Station, Platform 1" to "Platform 1",
            "Circular Quay, Wharf 2, Side A" to "Wharf 2, Side A",
            "M2 Motorway, Barclay Rd" to null,
            "Wynyard Station, Platform 3" to "Platform 3",
            "Seven Hills Station, Stand A" to "Stand A",
            "Circular Quay, Wharf 2" to "Wharf 2"
        )

        disassembledNameSamples.forEach { (input, expected) ->
            val leg = TripResponse.Leg(
                origin = TripResponse.StopSequence(
                    disassembledName = input
                )
            )

            assertEquals(expected, leg.getPlatformText())
        }
    }


    @Test
    fun testGetPlatformValue() {
        val disassembledNameSamples = listOf(
            "Central Station, Platform 16" to "16",
            "Central Station" to null,
            "Erskineville Station, Platform 1" to "1",
            "Circular Quay, Wharf 2, Side A" to "2",
            "M2 Motorway, Barclay Rd" to null,
            "Wynyard Station, Platform 3" to "3",
            "Seven Hills Station, Stand A" to "A",
            "Circular Quay, Wharf 2" to "2"
        )

        disassembledNameSamples.forEach { (input, expected) ->
            val leg = TripResponse.Leg(
                origin = TripResponse.StopSequence(
                    disassembledName = input
                )
            )

            assertEquals(expected, leg.getPlatformNumber())
        }
    }
}
