package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService

class FakeTripPlanningService : TripPlanningService {

    var isSuccess: Boolean = true

    var tripCallCount: Int = 0
        private set

    var lastCalledOriginStopId: String? = null
        private set
    var lastCalledDestinationStopId: String? = null
        private set
    var lastCalledDate: String? = null
        private set
    var lastCalledTime: String? = null
        private set
    var lastCalledDepArr: DepArr? = null
        private set
    var lastCalledExcludeProductClassSet: Set<Int>? = null
        private set

    /** Configures a custom response for a specific call index (0-based). Null means use default. */
    private val customResponses: MutableMap<Int, TripResponse?> = mutableMapOf()

    fun setResponseForCall(callIndex: Int, response: TripResponse?) {
        customResponses[callIndex] = response
    }

    fun reset() {
        isSuccess = true
        tripCallCount = 0
        lastCalledOriginStopId = null
        lastCalledDestinationStopId = null
        lastCalledDate = null
        lastCalledTime = null
        lastCalledDepArr = null
        lastCalledExcludeProductClassSet = null
        customResponses.clear()
    }

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse {
        lastCalledOriginStopId = originStopId
        lastCalledDestinationStopId = destinationStopId
        lastCalledDate = date
        lastCalledTime = time
        lastCalledDepArr = depArr
        lastCalledExcludeProductClassSet = excludeProductClassSet
        val callIndex = tripCallCount
        tripCallCount++
        val customResponse = customResponses[callIndex]
        if (customResponse != null) return customResponse
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
