package xyz.ksharma.krail.core.testing.fakes

import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse

object FakeStopFinderResponseBuilder {

    // ProductClass ints (see reference_transport_mode): Train=1, Metro=2, Bus=5, Ferry=9.
    private const val TRAIN = 1
    private const val METRO = 2
    private const val BUS = 5
    private const val FERRY = 9

    fun buildStopFinderResponse(): StopFinderResponse {
        return StopFinderResponse(
            locations = listOf(
                StopFinderResponse.Location(
                    id = "1",
                    name = "Stop 1",
                    type = "stop",
                    disassembledName = "Stop 1, Suburb 1",
                    properties = StopFinderResponse.Properties("1"),
                    productClasses = listOf(TRAIN, BUS, FERRY),
                ),
                StopFinderResponse.Location(
                    id = "2",
                    name = "Stop 2",
                    type = "stop",
                    disassembledName = "Stop 2, Suburb 2",
                    properties = StopFinderResponse.Properties("2"),
                    productClasses = listOf(TRAIN, METRO),
                ),
            ),
        )
    }
}
