@file:Suppress("LongMethod")

package xyz.ksharma.krail.feature.track.ui

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LegTrackingInfo
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackingConfig
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.feature.track.TripDeepLinkDecoder
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TrackTripViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tripService: ConfigurableFakeTripPlanningService
    private lateinit var trackingManager: TrackingManager
    private lateinit var festivalManager: FakeLocalFestivalManager
    private var currentVm: TrackTripViewModel? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tripService = ConfigurableFakeTripPlanningService()
        trackingManager = TrackingManager()
        festivalManager = FakeLocalFestivalManager()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Wraps [runTest] with a finally that cancels the ViewModel's [viewModelScope]
     * before the test body returns. The poll/live-positions/clock loops live in
     * [TripPoller] but the poller is constructed with `viewModelScope` here, so the
     * three `while (isActive)` loops still run on the test scheduler and never exit
     * naturally in Tracking state. Without this cancellation, `runTest`'s end-of-body
     * `advanceUntilIdle()` would re-schedule delays forever until the 60 s real-time
     * timeout. See [TripPollerTest] for the cleaner, scope-injected pattern that
     * exercises the polling logic directly via `runTest { TripPoller(backgroundScope) }`.
     */
    private fun runTrackingTest(
        body: suspend TestScope.() -> Unit,
    ): TestResult = runTest {
        try {
            body()
        } finally {
            currentVm?.viewModelScope?.cancel()
        }
    }

    // region ── Initial state resolution ─────────────────────────────────────

    @Test
    fun `GIVEN no encodedData and nothing tracked WHEN ViewModel created THEN state is Error`() =
        runTrackingTest {
            // Nothing in TrackingManager, no encoded deep link → unrecoverable
            val vm = makeViewModel(encodedData = null)

            vm.uiState.test {
                skipItems(1) // Loading() initialValue
                assertIs<TrackTripState.Error>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN valid encodedData and nothing tracked WHEN ViewModel created THEN state is Prompt`() =
        runTrackingTest {
            // A fresh deep link with no existing tracking → show confirm-tracking prompt
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(1.hours))
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem()
                assertIs<TrackTripState.Prompt>(state)
                assertEquals(deepLink.fromStopName, state.deepLink.fromStopName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN active trip in TrackingManager with display WHEN ViewModel created THEN state is Tracking and polling starts`() =
        runTrackingTest {
            // Simulates re-opening the screen for an already-tracking trip
            val deepLink = makeTripDeepLink(departureUtcDateTime = Clock.System.now().toString())
            val display = makeDisplay(deepLink, arrivalUtcDateTime = futureIso(30.minutes))
            trackingManager.start(deepLink)
            trackingManager.update(display)

            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = futureIso(30.minutes)) }
            val vm = makeViewModel(encodedData = null)

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem()
                assertIs<TrackTripState.Tracking>(state)
                assertEquals(display.fromStopName, state.journey.fromStopName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN arrived trip in TrackingManager WHEN ViewModel created THEN state is Arrived and zero API calls are made`() =
        runTrackingTest {
            // This is the key "no extra API call for arrived trip" scenario.
            // The ViewModel should restore Arrived state directly without polling.
            val deepLink = makeTripDeepLink(departureUtcDateTime = pastIso(1.hours))
            val display = makeDisplay(deepLink, arrivalUtcDateTime = pastIso(10.minutes))
            trackingManager.start(deepLink)
            trackingManager.update(display)
            trackingManager.markArrived()

            val vm = makeViewModel(encodedData = null)
            runCurrent()

            vm.uiState.test {
                skipItems(1)
                assertIs<TrackTripState.Arrived>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            // No API call should have been made — arrived trips are read from cache only
            assertEquals(0, tripService.callCount, "Expected zero API calls for an already-arrived trip")
        }

    @Test
    fun `GIVEN new deep link AND different trip already tracking WHEN ViewModel created THEN old tracking cleared and state is Prompt`() =
        runTrackingTest {
            // Deep link tap is explicit user intent to switch trips — old tracking is cleared automatically.
            val existing = makeTripDeepLink(
                transportationId = "existing-trip",
                departureUtcDateTime = futureIso(30.minutes),
            )
            val requested = makeTripDeepLink(
                transportationId = "new-trip",
                departureUtcDateTime = futureIso(45.minutes),
            )
            trackingManager.start(existing)

            val vm = makeViewModel(encodedData = requested.toEncodedData())

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem()
                assertIs<TrackTripState.Prompt>(state)
                assertEquals(requested.fromStopName, state.deepLink.fromStopName)
                cancelAndIgnoreRemainingEvents()
            }
            assertNull(trackingManager.tracked.value, "Old tracking should be cleared")
        }

    @Test
    fun `GIVEN existing trip with departure more than 2h ago WHEN ViewModel created THEN state is ArrivedAndFinished`() =
        runTrackingTest {
            // Stale trip that far exceeds DEPARTURE_EXPIRED_HOURS — should be cleaned up immediately
            val expiredDeepLink = makeTripDeepLink(departureUtcDateTime = pastIso(3.hours))
            trackingManager.start(expiredDeepLink)

            val vm = makeViewModel(encodedData = null)

            vm.uiState.test {
                skipItems(1)
                assertIs<TrackTripState.ArrivedAndFinished>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN new deep link with departure more than 2h ago WHEN ViewModel created THEN state is ArrivedAndFinished`() =
        runTrackingTest {
            // Opening a shared link for a trip that already expired — nothing to show
            val expiredDeepLink = makeTripDeepLink(departureUtcDateTime = pastIso(3.hours))
            val vm = makeViewModel(encodedData = expiredDeepLink.toEncodedData())

            vm.uiState.test {
                skipItems(1)
                assertIs<TrackTripState.ArrivedAndFinished>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region ── Polling / fetchAndUpdate ─────────────────────────────────────

    @Test
    fun `GIVEN Prompt state WHEN onStartTracking called AND API returns future arrival THEN transitions Loading to Tracking`() =
        runTrackingTest {
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(5.minutes))
            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = futureIso(30.minutes)) }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.uiState.test {
                skipItems(1) // Loading initialValue
                assertIs<TrackTripState.Prompt>(awaitItem())

                vm.onStartTracking(deepLink)

                assertIs<TrackTripState.Loading>(awaitItem())
                runCurrent()
                assertIs<TrackTripState.Tracking>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Tracking WHEN API returns journey with arrival in past within 30min THEN state becomes Arrived`() =
        runTrackingTest {
            // Arrival was 10 minutes ago — inside ARRIVAL_FINISHED_MINUTES window → Arrived (not finished)
            val deepLink = makeTripDeepLink(departureUtcDateTime = pastIso(40.minutes))
            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = pastIso(10.minutes)) }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)

            vm.uiState.test {
                skipItems(1)
                runCurrent()
                // Consume intermediate Loading / Tracking states and wait for Arrived
                val finalState = generateSequence { runCatching { expectMostRecentItem() }.getOrNull() }
                    .firstOrNull { it is TrackTripState.Arrived } ?: awaitItem()
                assertIs<TrackTripState.Arrived>(finalState)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Tracking WHEN API returns journey with arrival more than 30min ago THEN state becomes ArrivedAndFinished`() =
        runTrackingTest {
            // Arrival was 40 minutes ago — exceeds ARRIVAL_FINISHED_MINUTES (30) → go straight to finished
            val deepLink = makeTripDeepLink(departureUtcDateTime = pastIso(2.hours))
            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = pastIso(40.minutes)) }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)

            vm.uiState.test {
                skipItems(1)
                runCurrent()
                val state = expectMostRecentItem()
                assertIs<TrackTripState.ArrivedAndFinished>(state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Tracking WHEN API throws exception with no cached display THEN state becomes Error`() =
        runTrackingTest {
            // No existing cached display + network failure → can't recover, show error
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(30.minutes))
            tripService.shouldThrow = true
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)

            vm.uiState.test {
                skipItems(1)
                runCurrent()
                val state = expectMostRecentItem()
                assertIs<TrackTripState.Error>(state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Tracking with cached display WHEN API throws exception THEN stays Tracking with cached data`() =
        runTrackingTest {
            // TrackingManager already has a display — fall back to cache on network failure
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(30.minutes))
            val display = makeDisplay(deepLink, arrivalUtcDateTime = futureIso(1.hours))
            trackingManager.start(deepLink)
            trackingManager.update(display)

            tripService.shouldThrow = true
            val vm = makeViewModel(encodedData = null)

            vm.uiState.test {
                skipItems(1)
                runCurrent()
                val state = expectMostRecentItem()
                assertIs<TrackTripState.Tracking>(state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN Tracking WHEN API response contains no matching journey THEN state becomes NotFound`() =
        runTrackingTest {
            // The response has no journey whose transportation.id matches the deep link
            val deepLink = makeTripDeepLink(transportationId = "expected-id", departureUtcDateTime = futureIso(30.minutes))
            tripService.responseProvider = {
                buildMatchingResponse(
                    deepLink.copy(legs = listOf(TripDeepLink.DeepLinkLeg("different-id", 1))),
                    arrivalUtcDateTime = futureIso(1.hours),
                )
            }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)

            vm.uiState.test {
                skipItems(1)
                runCurrent()
                val state = expectMostRecentItem()
                assertIs<TrackTripState.NotFound>(state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region ── Poll suppression (smart-delay / re-subscribe) ────────────────

    @Test
    fun `GIVEN Tracking WHEN user leaves and returns within poll interval THEN no extra API call is made`() =
        runTrackingTest {
            // Simulates navigating away and back: WhileSubscribed timeout fires (5s),
            // then a new subscriber arrives. Since we just polled, the smart-delay should
            // prevent an immediate second API call.
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(30.minutes))
            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = futureIso(1.hours)) }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)
            // First poll fires immediately
            vm.uiState.test {
                skipItems(1)
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }
            val callsAfterFirstPoll = tripService.callCount
            assertTrue(callsAfterFirstPoll >= 1, "Expected at least one poll before leaving screen")

            // Screen gone: WhileSubscribed stops upstream, onCompletion cancels pollingJob
            advanceTimeBy(6_000) // past the 5s WhileSubscribed stop timeout

            // Screen returns: new subscriber → onStart resumes polling
            vm.uiState.test {
                skipItems(1)
                // Small advance — NOT enough to reach POLL_INTERVAL_MS
                advanceTimeBy(500)
                cancelAndIgnoreRemainingEvents()
            }

            // Because we came back within the poll interval, no new API call should fire
            assertEquals(
                callsAfterFirstPoll,
                tripService.callCount,
                "Expected no extra API call when returning within poll interval",
            )
        }

    // The companion "after poll interval expires THEN API call is made on return" test
    // is intentionally absent: the smart-delay reads `Clock.System.now()` (real wall-clock),
    // not the test scheduler's virtual time. `advanceTimeBy(POLL_INTERVAL_MS)` only moves
    // virtual time, so `Clock.System.now() - lastPollInstant` stays at ~10 ms and the
    // smart-delay never expires under runTest. Verifying the "expires" branch needs a
    // testable clock injected into TrackTripViewModel — out of scope for this test.

    // endregion

    // region ── onStopTracking ────────────────────────────────────────────────

    @Test
    fun `GIVEN Tracking WHEN onStopTracking called THEN tracking is cleared and no more polls fire`() =
        runTrackingTest {
            // User explicitly stops tracking — TrackingManager cleared, no further API calls
            val deepLink = makeTripDeepLink(departureUtcDateTime = futureIso(30.minutes))
            tripService.responseProvider = { buildMatchingResponse(deepLink, arrivalUtcDateTime = futureIso(1.hours)) }
            val vm = makeViewModel(encodedData = deepLink.toEncodedData())

            vm.onStartTracking(deepLink)
            runCurrent()

            vm.onStopTracking()
            runCurrent()

            assertNull(trackingManager.tracked.value, "TrackingManager should be empty after stop")

            val callsAfterStop = tripService.callCount
            advanceTimeBy(TrackingConfig.POLL_INTERVAL_MS * 2)
            assertEquals(callsAfterStop, tripService.callCount, "No API calls should fire after stop")
        }

    // endregion

    // region ── Trip tracking flag ────────────────────────────────────────────

    @Test
    fun `GIVEN flag is false WHEN ViewModel created THEN isTripTrackingEnabled is false`() =
        runTrackingTest {
            val vm = makeViewModel(encodedData = null, isTripTrackingEnabled = false)
            assertTrue(!vm.isTripTrackingEnabled)
        }

    @Test
    fun `GIVEN flag is true WHEN ViewModel created THEN isTripTrackingEnabled is true`() =
        runTrackingTest {
            val vm = makeViewModel(encodedData = null, isTripTrackingEnabled = true)
            assertTrue(vm.isTripTrackingEnabled)
        }

    // endregion

    // region ── Helpers ───────────────────────────────────────────────────────

    private fun makeViewModel(
        encodedData: String?,
        isTripTrackingEnabled: Boolean = false,
    ) = TrackTripViewModel(
        encodedData = encodedData,
        tripPlanningService = tripService,
        trackingManager = trackingManager,
        ioDispatcher = testDispatcher,
        festivalManager = festivalManager,
        gtfsRealtimeRepository = FakeGtfsRealtimeRepository(),
        sandook = FakeSandook(),
        shareManager = NoopShareManager,
        isTripTrackingEnabled = isTripTrackingEnabled,
    ).also { currentVm = it }

    private fun futureIso(duration: kotlin.time.Duration) =
        (Clock.System.now() + duration).toString()

    private fun pastIso(duration: kotlin.time.Duration) =
        (Clock.System.now() - duration).toString()

    private fun makeTripDeepLink(
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

    @OptIn(ExperimentalEncodingApi::class)
    private fun TripDeepLink.toEncodedData(): String {
        val json = Json.encodeToString(this)
        return Base64.UrlSafe.encode(json.encodeToByteArray()).trimEnd('=')
    }

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

    private fun buildMatchingResponse(
        deepLink: TripDeepLink,
        arrivalUtcDateTime: String,
    ): TripResponse {
        val origin = buildStopSeq(deepLink.departureUtcDateTime)
        val destination = buildStopSeq(arrivalUtcDateTime)
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
            duration = 1800L,
        )
        return TripResponse(journeys = listOf(TripResponse.Journey(legs = listOf(leg))))
    }

    private fun buildStopSeq(utcDateTime: String) = TripResponse.StopSequence(
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
}

// ── Local fakes ──────────────────────────────────────────────────────────────

private class ConfigurableFakeTripPlanningService : TripPlanningService {
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

private class FakeLocalFestivalManager : FestivalManager {
    override fun festivalOnDate(date: LocalDate): Festival? = null
    override fun emojiForDate(date: LocalDate): String = "\uD83D\uDE86"
}

private class FakeGtfsRealtimeRepository : GtfsRealtimeRepository {
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

private class FakeSandook : xyz.ksharma.krail.sandook.Sandook {
    override fun observeStopLabels(): Flow<List<xyz.ksharma.krail.sandook.StopLabels>> = flowOf(emptyList())
    override fun upsertStopLabel(label: String, emoji: String, stopId: String?, stopName: String?, sortOrder: Long) = Unit
    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) = Unit
    override fun renameStopLabel(label: String, newLabel: String) = Unit
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
    override fun updateSavedTripSortOrder(tripId: String, sortOrder: Long) = Unit
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
    override fun selectStopsByIds(stopIds: List<String>): List<xyz.ksharma.krail.sandook.SelectProductClassesForStop> = emptyList()
    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> = emptyMap()
    override fun insertOrReplaceRecentSearchStop(stopId: String) = Unit
    override fun selectRecentSearchStops(): List<xyz.ksharma.krail.sandook.SelectRecentSearchStops> = emptyList()
    override fun clearRecentSearchStops() = Unit
    override fun cleanupOrphanedRecentSearchStops() = Unit
    override fun cleanupOldRecentSearchStops() = Unit
}

private object NoopShareManager : xyz.ksharma.krail.core.share.ShareManager {
    override suspend fun shareImage(
        bitmap: androidx.compose.ui.graphics.ImageBitmap,
        title: String,
        text: String?,
    ): Result<Unit> = Result.success(Unit)
}

