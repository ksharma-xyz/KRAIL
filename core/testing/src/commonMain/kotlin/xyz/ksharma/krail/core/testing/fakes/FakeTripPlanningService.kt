package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.yield
import xyz.ksharma.krail.core.network.error.NetworkError
import xyz.ksharma.krail.core.network.error.NetworkException
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService

class FakeTripPlanningService : TripPlanningService {

    var isSuccess: Boolean = true

    /**
     * Names *which* failure the next call returns. A test that only needs "something went
     * wrong" can keep using [isSuccess]; a test that asserts on offline versus upstream
     * behaviour sets this. Takes precedence over [isSuccess].
     */
    var failWith: NetworkError? = null

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

    // Configures a custom response for a specific call index (0-based); null = use default.
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
    ): Result<TripResponse> {
        lastCalledOriginStopId = originStopId
        lastCalledDestinationStopId = destinationStopId
        lastCalledDate = date
        lastCalledTime = time
        lastCalledDepArr = depArr
        lastCalledExcludeProductClassSet = excludeProductClassSet
        // See FakeDeparturesService: the real service always suspends, so this must too.
        yield()
        val callIndex = tripCallCount
        tripCallCount++
        val failure = failWith ?: NetworkError.Unknown(
            cause = RuntimeException("Failed to fetch trip"),
        ).takeIf { !isSuccess }

        return failure?.let { Result.failure(NetworkException(error = it)) }
            ?: Result.success(
                customResponses[callIndex] ?: FakeTripResponseBuilder.buildTripResponse(),
            )
    }

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): Result<StopFinderResponse> {
        yield()
        failWith?.let { return Result.failure(NetworkException(error = it)) }
        return if (isSuccess) {
            Result.success(FakeStopFinderResponseBuilder.buildStopFinderResponse())
        } else {
            Result.failure(
                NetworkException(error = NetworkError.Unknown(cause = RuntimeException("Failed to fetch stops"))),
            )
        }
    }
}
