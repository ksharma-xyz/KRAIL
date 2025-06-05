package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService

class FakeTripPlanningService : TripPlanningService {

    var isSuccess: Boolean = true

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse {
        return if (isSuccess) FakeTripResponseBuilder.buildTripResponse()
        else throw IllegalStateException("Failed to fetch trip")
    }

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): StopFinderResponse {
        // Return a fake StopFinderResponse
        return if (isSuccess) FakeStopFinderResponseBuilder.buildStopFinderResponse()
        else throw IllegalStateException("Failed to fetch stops")
    }
}
