@file:Suppress("LongMethod")

package xyz.ksharma.krail.feature.track.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LegTrackingInfo
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Direct unit tests for [TripPoller].
 *
 * The poller takes a [kotlinx.coroutines.CoroutineScope] in its constructor, so tests
 * pass [kotlinx.coroutines.test.TestScope.backgroundScope] — which `runTest` auto-
 * cancels at end-of-body. The three `while (isActive)` polling loops therefore stop
 * cleanly when the test returns; no helper wrapper, no manual cleanup, no risk of the
 * trailing `advanceUntilIdle` looping forever (contrast with [TrackTripViewModelTest],
 * which uses `viewModelScope` and needs a cleanup wrapper for exactly that reason).
 *
 * That's the whole point of the [TrackTripViewModel] → [TripPoller] split: the poller
 * accepts its lifecycle scope, so the lifecycle is whatever the caller wants.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TripPollerTest {

    @Test
    fun `GIVEN startPolling WHEN API returns future arrival THEN state becomes Tracking`() = runTest {
        val deepLink = makeDeepLink(departureUtcDateTime = futureIso(5.minutes))
        val tripService = ConfigurableTripService().apply {
            responseProvider = { matchingResponse(deepLink, arrivalUtcDateTime = futureIso(30.minutes)) }
        }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val poller = makePoller(tripService = tripService, state = state)

        poller.startPolling(deepLink)
        runCurrent()

        assertIs<TrackTripState.Tracking>(state.value)
        assertEquals(1, tripService.callCount)
    }

    @Test
    fun `GIVEN startPolling WHEN API returns past arrival within 30min THEN state becomes Arrived`() = runTest {
        val deepLink = makeDeepLink(departureUtcDateTime = pastIso(40.minutes))
        val tripService = ConfigurableTripService().apply {
            responseProvider = { matchingResponse(deepLink, arrivalUtcDateTime = pastIso(10.minutes)) }
        }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val poller = makePoller(tripService = tripService, state = state)

        poller.startPolling(deepLink)
        runCurrent()

        assertIs<TrackTripState.Arrived>(state.value)
    }

    @Test
    fun `GIVEN startPolling WHEN API returns past arrival over 30min ago THEN state becomes ArrivedAndFinished`() =
        runTest {
            val deepLink = makeDeepLink(departureUtcDateTime = pastIso(2.hours))
            val tripService = ConfigurableTripService().apply {
                responseProvider = { matchingResponse(deepLink, arrivalUtcDateTime = pastIso(40.minutes)) }
            }
            val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
            val trackingManager = TrackingManager().apply { start(deepLink) }
            val poller = makePoller(
                tripService = tripService,
                state = state,
                trackingManager = trackingManager,
            )

            poller.startPolling(deepLink)
            runCurrent()

            assertEquals(TrackTripState.ArrivedAndFinished, state.value)
            assertNull(trackingManager.tracked.value, "tracking should be cleared on finish")
            assertFalse(poller.isPollingActive, "polling job should have stopped")
        }

    @Test
    fun `GIVEN startPolling WHEN API throws AND no cached display THEN state becomes Error`() = runTest {
        val deepLink = makeDeepLink(departureUtcDateTime = futureIso(30.minutes))
        val tripService = ConfigurableTripService().apply { shouldThrow = true }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val poller = makePoller(tripService = tripService, state = state)

        poller.startPolling(deepLink)
        runCurrent()

        assertEquals(TrackTripState.Error, state.value)
    }

    @Test
    fun `GIVEN startPolling WHEN API throws AND cached display exists THEN state stays Tracking with cache`() =
        runTest {
            val deepLink = makeDeepLink(departureUtcDateTime = futureIso(30.minutes))
            val tripService = ConfigurableTripService().apply { shouldThrow = true }
            val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
            val trackingManager = TrackingManager().apply {
                start(deepLink)
                update(makeDisplay(deepLink, arrivalUtcDateTime = futureIso(1.hours)))
            }
            val poller = makePoller(
                tripService = tripService,
                state = state,
                trackingManager = trackingManager,
            )

            poller.startPolling(deepLink)
            runCurrent()

            val current = state.value
            assertIs<TrackTripState.Tracking>(current)
            assertEquals(deepLink.fromStopName, current.journey.fromStopName)
        }

    @Test
    fun `GIVEN startPolling WHEN response has no matching journey THEN state becomes NotFound`() = runTest {
        val deepLink = makeDeepLink(
            transportationId = "expected",
            departureUtcDateTime = futureIso(30.minutes),
        )
        val mismatch = deepLink.copy(legs = listOf(TripDeepLink.DeepLinkLeg("other", 1)))
        val tripService = ConfigurableTripService().apply {
            responseProvider = { matchingResponse(mismatch, arrivalUtcDateTime = futureIso(1.hours)) }
        }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val poller = makePoller(tripService = tripService, state = state)

        poller.startPolling(deepLink)
        runCurrent()

        assertEquals(TrackTripState.NotFound, state.value)
    }

    @Test
    fun `GIVEN expired trip WHEN startPolling fires THEN goes straight to ArrivedAndFinished without API call`() =
        runTest {
            val deepLink = makeDeepLink(departureUtcDateTime = pastIso(3.hours))
            val tripService = ConfigurableTripService()
            val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
            val poller = makePoller(tripService = tripService, state = state)

            poller.startPolling(deepLink)
            runCurrent()

            assertEquals(TrackTripState.ArrivedAndFinished, state.value)
            assertEquals(0, tripService.callCount, "expired trip must not hit the API")
        }

    @Test
    fun `GIVEN transitionToArrivedAndFinished WHEN called directly THEN polling stops and state is set`() = runTest {
        val deepLink = makeDeepLink(departureUtcDateTime = futureIso(30.minutes))
        val tripService = ConfigurableTripService().apply {
            responseProvider = { matchingResponse(deepLink, arrivalUtcDateTime = futureIso(1.hours)) }
        }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val trackingManager = TrackingManager().apply { start(deepLink) }
        val poller = makePoller(
            tripService = tripService,
            state = state,
            trackingManager = trackingManager,
        )

        poller.startPolling(deepLink)
        runCurrent()
        assertTrue(poller.isPollingActive)

        poller.transitionToArrivedAndFinished()
        runCurrent()

        assertEquals(TrackTripState.ArrivedAndFinished, state.value)
        assertFalse(poller.isPollingActive)
        assertNull(trackingManager.tracked.value)
    }

    @Test
    fun `GIVEN isTripExpired WHEN departure is past expiry hours THEN returns true`() = runTest {
        val expired = makeDeepLink(departureUtcDateTime = pastIso(3.hours))
        val fresh = makeDeepLink(departureUtcDateTime = futureIso(30.minutes))
        val poller = makePoller()

        assertTrue(poller.isTripExpired(expired))
        assertFalse(poller.isTripExpired(fresh))
    }

    @Test
    fun `GIVEN startPolling WHEN successful THEN polling job is active`() = runTest {
        val deepLink = makeDeepLink(departureUtcDateTime = futureIso(30.minutes))
        val tripService = ConfigurableTripService().apply {
            responseProvider = { matchingResponse(deepLink, arrivalUtcDateTime = futureIso(1.hours)) }
        }
        val state = MutableStateFlow<TrackTripState>(TrackTripState.Loading())
        val poller = makePoller(tripService = tripService, state = state)

        assertFalse(poller.isPollingActive, "poller starts idle")
        poller.startPolling(deepLink)
        runCurrent()
        // First poll completed and state is Tracking — loop is parked on its
        // POLL_INTERVAL_MS delay, still active.
        assertTrue(poller.isPollingActive, "poller should be running after a successful first poll")
        assertIs<TrackTripState.Tracking>(state.value)
    }

    // region helpers

    private fun TestScope.makePoller(
        tripService: TripPlanningService = ConfigurableTripService(),
        state: MutableStateFlow<TrackTripState> = MutableStateFlow(TrackTripState.Loading()),
        trackingManager: TrackingManager = TrackingManager(),
    ): TripPoller = TripPoller(
        scope = backgroundScope,
        // Share the test scheduler so withContext(ioDispatcher) advances under the
        // same virtual clock as runCurrent().
        ioDispatcher = StandardTestDispatcher(testScheduler),
        tripPlanningService = tripService,
        trackingManager = trackingManager,
        gtfsRealtimeRepository = NoopGtfsRealtimeRepository,
        sandook = NoopSandook,
        state = state,
    )

    private fun futureIso(duration: kotlin.time.Duration) =
        (Clock.System.now() + duration).toString()

    private fun pastIso(duration: kotlin.time.Duration) =
        (Clock.System.now() - duration).toString()

    private fun makeDeepLink(
        transportationId: String = "test-transport-id",
        departureUtcDateTime: String = futureIso(1.hours),
    ) = TripDeepLink(
        fromStopId = "from-stop",
        toStopId = "to-stop",
        fromStopName = "Origin Station",
        toStopName = "Destination Station",
        departureUtcDateTime = departureUtcDateTime,
        legs = listOf(TripDeepLink.DeepLinkLeg(transportationId = transportationId, productClass = 1)),
    )

    private fun makeDisplay(
        deepLink: TripDeepLink,
        arrivalUtcDateTime: String,
    ) = xyz.ksharma.krail.feature.track.TrackedJourneyDisplay(
        fromStopId = deepLink.fromStopId,
        toStopId = deepLink.toStopId,
        fromStopName = deepLink.fromStopName,
        toStopName = deepLink.toStopName,
        originTime = "08:00",
        scheduledOriginTime = null,
        destinationTime = "08:30",
        originUtcDateTime = deepLink.departureUtcDateTime,
        destinationUtcDateTime = arrivalUtcDateTime,
        travelTime = "30 mins",
        legs = kotlinx.collections.immutable.persistentListOf(),
    )

    private fun matchingResponse(
        deepLink: TripDeepLink,
        arrivalUtcDateTime: String,
    ): TripResponse {
        val origin = stopSeq(deepLink.departureUtcDateTime)
        val destination = stopSeq(arrivalUtcDateTime)
        val leg = TripResponse.Leg(
            origin = origin,
            destination = destination,
            stopSequence = listOf(origin, destination),
            transportation = TripResponse.Transportation(
                id = deepLink.legs.first().transportationId,
                disassembledName = "T1",
                product = TripResponse.Product(productClass = 1L, name = "Train"),
                destination = TripResponse.OperatorClass(name = "City Circle", id = "cc"),
                name = "T1 Northern",
                description = "Train",
            ),
            duration = SECONDS_PER_HALF_HOUR,
        )
        return TripResponse(journeys = listOf(TripResponse.Journey(legs = listOf(leg))))
    }

    private fun stopSeq(utcDateTime: String) = TripResponse.StopSequence(
        arrivalTimePlanned = utcDateTime,
        arrivalTimeEstimated = utcDateTime,
        departureTimePlanned = utcDateTime,
        departureTimeEstimated = utcDateTime,
        name = "Stop",
        disassembledName = "Stop",
        id = "stop_id",
        type = StopType.STOP.type,
    )

    // endregion

    companion object {
        private const val SECONDS_PER_HALF_HOUR = 1800L
    }
}

// region Local fakes

private class ConfigurableTripService : TripPlanningService {
    var callCount = 0
    var shouldThrow = false
    var responseProvider: () -> TripResponse = { TripResponse(journeys = emptyList()) }

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse {
        callCount++
        if (shouldThrow) throw IllegalStateException("Simulated network error")
        return responseProvider()
    }

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): StopFinderResponse = StopFinderResponse()
}

private object NoopGtfsRealtimeRepository : GtfsRealtimeRepository {
    override suspend fun pollLiveTracking(
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay = LiveTrackingOverlay(
        vehiclePositions = emptyMap(),
        stopDelays = emptyMap(),
        lastModified = null,
    )

    override fun clearCache() = Unit
}

private object NoopSandook : xyz.ksharma.krail.sandook.Sandook {
    override fun observeStopLabels(): Flow<List<xyz.ksharma.krail.sandook.StopLabels>> = flowOf(emptyList())
    override fun upsertStopLabel(label: String, emoji: String, stopId: String?, stopName: String?, sortOrder: Long) = Unit
    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) = Unit
    override fun deleteStopLabel(label: String) = Unit
    override fun clearStopLabels() = Unit
    override fun insertOrReplaceTheme(productClass: Long) = Unit
    override fun getProductClass(): Long? = null
    override fun clearTheme() = Unit
    override fun insertOrReplaceTrip(tripId: String, fromStopId: String, fromStopName: String, toStopId: String, toStopName: String) = Unit
    override fun deleteTrip(tripId: String) = Unit
    override fun selectAllTrips(): List<xyz.ksharma.krail.sandook.SavedTrip> = emptyList()
    override fun observeAllTrips(): Flow<List<xyz.ksharma.krail.sandook.SavedTrip>> = flowOf(emptyList())
    override fun selectTripById(tripId: String): xyz.ksharma.krail.sandook.SavedTrip? = null
    override fun clearSavedTrips() = Unit
    override fun getAlerts(journeyId: String): List<xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId> = emptyList()
    override fun clearAlerts() = Unit
    override fun insertAlerts(journeyId: String, alerts: List<xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId>) = Unit
    override fun insertNswStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double, isParent: Boolean?) = Unit
    override fun stopsCount(): Int = 0
    override fun productClassCount(): Int = 0
    override fun insertNswStopProductClass(stopId: String, productClass: Int) = Unit
    override fun <R> insertTransaction(block: () -> R): R = block()
    override fun clearNswStopsTable() = Unit
    override fun clearNswProductClassTable() = Unit
    override fun selectStops(stopName: String, excludeProductClassList: List<Int>): List<xyz.ksharma.krail.sandook.SelectProductClassesForStop> = emptyList()
    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> = emptyMap()
    override fun insertOrReplaceRecentSearchStop(stopId: String) = Unit
    override fun selectRecentSearchStops(): List<xyz.ksharma.krail.sandook.SelectRecentSearchStops> = emptyList()
    override fun clearRecentSearchStops() = Unit
    override fun cleanupOrphanedRecentSearchStops() = Unit
    override fun cleanupOldRecentSearchStops() = Unit
}

// endregion
