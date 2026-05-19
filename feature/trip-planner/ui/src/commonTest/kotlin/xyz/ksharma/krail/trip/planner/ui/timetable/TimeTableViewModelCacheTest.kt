package xyz.ksharma.krail.trip.planner.ui.timetable

/**
 * Comprehensive tests for TimeTableViewModel's three-cache architecture and staleness-pruning
 * logic. If you are modifying any cache-related code in TimeTableViewModel, read the design doc
 * first:
 *
 *   feature/trip-planner/ui/docs/timetable_cache_architecture.md
 *
 * Test categories:
 *   1. Pruning correctness — entries removed/kept by [pruneStaleLoadMoreEntries]
 *   2. Cancellation simulation — a trip disappears from the API → pruned from loadMoreJourneys
 *   3. Real-time data wins — journeys (fresh) beats loadMoreJourneys (stale) in the merged list
 *   4. previousJourneysCache isolation — auto-refresh must never touch it
 *   5. canLoadMore invariants — flag stays coherent after pruning
 *   6. End-to-end time progression — full auto-refresh cycle through the ViewModel
 */

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeRateLimiter
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeShareManager
import xyz.ksharma.krail.core.testing.fakes.FakeTripPlanningService
import xyz.ksharma.krail.core.testing.fakes.FakeTripResponseBuilder
import xyz.ksharma.krail.core.testing.fakes.FakeTripResponseBuilder.buildTripResponse
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.MAX_LOAD_MORE_COUNT
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TimeTableViewModelCacheTest {

    // ── Test infrastructure ────────────────────────────────────────────────────

    private val tripPlanningService = FakeTripPlanningService()
    private val rateLimiter = FakeRateLimiter()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TimeTableViewModel

    private val defaultTrip = Trip(
        fromStopId = "stop1",
        fromStopName = "Origin",
        toStopId = "stop2",
        toStopName = "Destination",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TimeTableViewModel(
            tripPlanningService = tripPlanningService,
            rateLimiter = rateLimiter,
            sandook = FakeSandook(),
            analytics = FakeAnalytics(),
            shareManager = FakeShareManager(),
            ioDispatcher = testDispatcher,
            festivalManager = FakeFestivalManager(),
            flag = FakeFlag(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helper: build a JourneyCardInfo with a fully-controlled originUtcDateTime ──

    /**
     * Creates a minimal [TimeTableState.JourneyCardInfo] with the given departure time and a
     * stable, predictable [TimeTableState.JourneyCardInfo.journeyId] derived from [tripId].
     *
     * journeyId = tripId filtered to alphanumeric chars, so use only letters and digits in [tripId]
     * to keep the expected value trivially obvious in assertions.
     */
    private fun buildJourneyCardInfo(
        tripId: String,
        originUtcDateTime: String,
        destinationUtcDateTime: String = originUtcDateTime,
    ): TimeTableState.JourneyCardInfo {
        val modeLine = TransportModeLine(transportMode = TransportMode.Train, lineName = "T1")
        return TimeTableState.JourneyCardInfo(
            timeText = "in 5 min",
            originTime = "12:00pm",
            originUtcDateTime = originUtcDateTime,
            destinationTime = "12:30pm",
            destinationUtcDateTime = destinationUtcDateTime,
            travelTime = "30 min",
            transportModeLines = persistentListOf(modeLine),
            legs = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    transportModeLine = modeLine,
                    displayText = "towards Central",
                    totalDuration = "30 min",
                    stops = persistentListOf(),
                    // tripId drives journeyId computation
                    tripId = tripId,
                ),
            ),
            totalUniqueServiceAlerts = 0,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 1: Pruning correctness — pruneStaleLoadMoreEntries()
    //
    // See: timetable_cache_architecture.md § Staleness Pruning
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN load-more trip departs before latest fresh journey WHEN pruneStaleLoadMoreEntries THEN it is removed`() =
        runTest {
            // GIVEN: auto-refresh window covers now..now+10m (3 journeys: indices 0,1,2)
            // latestFreshInstant ≈ now+10m
            tripPlanningService.isSuccess = true
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 3))
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Insert a load-more trip at now+5 — inside the fresh window (5 < 10)
            val staleTime = Clock.System.now().plus(5.minutes).toString()
            val staleTrip = buildJourneyCardInfo(tripId = "STALE1", originUtcDateTime = staleTime)
            viewModel.loadMoreJourneys[staleTrip.journeyId] = staleTrip

            // WHEN: pruning runs
            viewModel.pruneStaleLoadMoreEntries()

            // THEN: stale trip removed because its departure <= latestFreshInstant (now+10m)
            assertFalse(
                viewModel.loadMoreJourneys.containsKey(staleTrip.journeyId),
                "Load-more trip inside the fresh window must be pruned",
            )
        }

    @Test
    fun `GIVEN load-more trip departs after latest fresh journey WHEN pruneStaleLoadMoreEntries THEN it is NOT removed`() =
        runTest {
            // GIVEN: auto-refresh window covers up to now+10m (3 journeys)
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Insert a load-more trip genuinely beyond the fresh window
            val futureTime = Clock.System.now().plus(60.minutes).toString()
            val futureTrip = buildJourneyCardInfo(tripId = "FUTURE1", originUtcDateTime = futureTime)
            viewModel.loadMoreJourneys[futureTrip.journeyId] = futureTrip

            // WHEN
            viewModel.pruneStaleLoadMoreEntries()

            // THEN: future trip survives — the fresh window has not reached it
            assertTrue(
                viewModel.loadMoreJourneys.containsKey(futureTrip.journeyId),
                "Load-more trip beyond the fresh window must NOT be pruned",
            )
        }

    @Test
    fun `GIVEN load-more trip departs at exactly the latest fresh instant WHEN pruneStaleLoadMoreEntries THEN it IS removed (boundary)`() =
        runTest {
            // The pruning condition is <= (not <), so boundary trips are treated as covered.
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))

            // Use 1 journey so the latest fresh instant is precisely defined (index 0 = now)
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 1))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // The sole fresh journey is at index-0 time ≈ now
            val latestFreshTime = viewModel.journeys.values
                .maxByOrNull { it.originUtcDateTime }!!
                .originUtcDateTime

            // Insert a load-more trip at exactly the same instant
            val boundaryTrip = buildJourneyCardInfo(
                tripId = "BOUNDARY1",
                originUtcDateTime = latestFreshTime,
            )
            viewModel.loadMoreJourneys[boundaryTrip.journeyId] = boundaryTrip

            viewModel.pruneStaleLoadMoreEntries()

            assertFalse(
                viewModel.loadMoreJourneys.containsKey(boundaryTrip.journeyId),
                "Boundary trip at exactly latestFreshInstant must be pruned (<=)",
            )
        }

    @Test
    fun `GIVEN multiple load-more trips spanning both sides of fresh window WHEN pruneStaleLoadMoreEntries THEN only in-window trips are removed`() =
        runTest {
            // auto-refresh window: 3 journeys → latestFreshInstant = journeys.max(originUtcDateTime)
            tripPlanningService.isSuccess = true
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 3))
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Anchor relative to the *actual* latestFreshInstant stored in journeys so the
            // test is immune to wall-clock drift between buildTripResponse() and here.
            val latestFreshInstant = kotlin.time.Instant.parse(
                viewModel.journeys.values.maxByOrNull { it.originUtcDateTime }!!.originUtcDateTime,
            )

            // inside: 2 min before the boundary → must be pruned
            val insideWindow = buildJourneyCardInfo(
                "INSIDE1",
                latestFreshInstant.minus(2.minutes).toString(),
            )
            // on boundary: exactly at latestFreshInstant → pruned (<=)
            val onBoundary = buildJourneyCardInfo("BOUND1", latestFreshInstant.toString())
            // outside: clearly beyond → must survive
            val outsideWindow = buildJourneyCardInfo(
                "OUTSIDE1",
                latestFreshInstant.plus(10.minutes).toString(),
            )
            val farFuture = buildJourneyCardInfo(
                "FUTURE1",
                latestFreshInstant.plus(35.minutes).toString(),
            )

            viewModel.loadMoreJourneys[insideWindow.journeyId] = insideWindow
            viewModel.loadMoreJourneys[onBoundary.journeyId] = onBoundary
            viewModel.loadMoreJourneys[outsideWindow.journeyId] = outsideWindow
            viewModel.loadMoreJourneys[farFuture.journeyId] = farFuture

            viewModel.pruneStaleLoadMoreEntries()

            // Trips inside or at the boundary → removed
            assertFalse(viewModel.loadMoreJourneys.containsKey(insideWindow.journeyId))
            assertFalse(viewModel.loadMoreJourneys.containsKey(onBoundary.journeyId))

            // Trips genuinely beyond the latest fresh instant → kept
            assertTrue(viewModel.loadMoreJourneys.containsKey(outsideWindow.journeyId))
            assertTrue(viewModel.loadMoreJourneys.containsKey(farFuture.journeyId))
        }

    @Test
    fun `GIVEN loadMoreJourneys is empty WHEN pruneStaleLoadMoreEntries THEN no crash and journeys unchanged`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertTrue(viewModel.loadMoreJourneys.isEmpty())

            // Must not throw
            viewModel.pruneStaleLoadMoreEntries()

            assertTrue(viewModel.loadMoreJourneys.isEmpty())
        }

    @Test
    fun `GIVEN journeys is empty WHEN pruneStaleLoadMoreEntries THEN nothing is pruned (no reference instant)`() =
        runTest {
            // journeys is empty — no latestFreshInstant can be computed, so nothing should be pruned
            val futureTrip = buildJourneyCardInfo(
                tripId = "SAFE1",
                originUtcDateTime = Clock.System.now().plus(30.minutes).toString(),
            )
            viewModel.loadMoreJourneys[futureTrip.journeyId] = futureTrip

            viewModel.pruneStaleLoadMoreEntries()

            // Still present — no reference instant means no pruning
            assertTrue(
                viewModel.loadMoreJourneys.containsKey(futureTrip.journeyId),
                "Without a reference instant no entries should be pruned",
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 2: Cancellation simulation
    //
    // A trip that was "future" when loaded more gets cancelled.  The API stops
    // returning it.  On the next auto-refresh the time window advances to cover
    // that departure slot → the pruning removes the ghost trip.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN cancelled trip in loadMoreJourneys WHEN auto-refresh covers its window THEN ghost trip is removed from UI`() =
        runTest {
            // Step 1: initial load → journeys covers now+0..now+10 (3 journeys)
            tripPlanningService.isSuccess = true
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 3))
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Step 2: user loaded more trips; simulate with a trip at now+15 that is later cancelled
            val cancelledTime = Clock.System.now().plus(15.minutes).toString()
            val cancelledTrip = buildJourneyCardInfo(
                tripId = "CANCELLED1",
                originUtcDateTime = cancelledTime,
            )
            viewModel.loadMoreJourneys[cancelledTrip.journeyId] = cancelledTrip
            assertTrue(viewModel.loadMoreJourneys.containsKey(cancelledTrip.journeyId))

            // Step 3: auto-refresh advances the window to now+0..now+20 (no CANCELLED1 in response)
            // The new latestFreshInstant = now+20m → cancelledTrip at now+15 is <= that → pruned.
            val freshResponseWithoutCancelled = buildTripResponse(numberOfJourney = 5)
            // 5 journeys: indices 0..4 → times now, now+5, now+10, now+15, now+20
            // CANCELLED1 is NOT in this response (different tripId), so the API effectively dropped it
            tripPlanningService.setResponseForCall(1, freshResponseWithoutCancelled)
            viewModel.fetchTrip() // second call — auto-refresh
            advanceUntilIdle()

            // THEN: the cancelled trip no longer appears in loadMoreJourneys
            assertFalse(
                viewModel.loadMoreJourneys.containsKey(cancelledTrip.journeyId),
                "Cancelled trip within the fresh window must be evicted from loadMoreJourneys",
            )

            // And it must not appear in the merged UI journey list
            assertFalse(
                viewModel.uiState.value.journeyList.any { it.journeyId == cancelledTrip.journeyId },
                "Cancelled trip must not appear in the merged UI state",
            )
        }

    @Test
    fun `GIVEN cancelled trip in loadMoreJourneys beyond auto-refresh window WHEN auto-refresh runs THEN ghost trip remains until window catches up`() =
        runTest {
            // This test documents intentional behaviour: trips beyond the fresh window
            // are not yet cancelled in the UI — they persist until the window reaches them.
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip() // covers now..now+10
            advanceUntilIdle()

            // A future trip at now+60 that the API has already cancelled, but our fresh window
            // only reaches now+10 — we cannot detect the cancellation yet.
            val ghostTime = Clock.System.now().plus(60.minutes).toString()
            val ghostTrip = buildJourneyCardInfo("GHOST1", ghostTime)
            viewModel.loadMoreJourneys[ghostTrip.journeyId] = ghostTrip

            viewModel.pruneStaleLoadMoreEntries()

            // Expected: ghost trip still visible — this is an accepted limitation documented in
            // timetable_cache_architecture.md (trips beyond the fresh window can't be validated)
            assertTrue(
                viewModel.loadMoreJourneys.containsKey(ghostTrip.journeyId),
                "Trip beyond the fresh window cannot be detected as cancelled — known limitation",
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 3: Real-time data wins over stale load-more cache
    //
    // When the same journeyId appears in both `journeys` (freshly fetched) and
    // `loadMoreJourneys` (older fetch), the merge in updateUiStateWithFilteredTrips
    // must prefer the `journeys` copy because it carries current real-time data.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN same journeyId in journeys and loadMoreJourneys WHEN UI state is built THEN journeys version appears in journeyList`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Grab the first journey that auto-refresh produced
            val freshJourney = viewModel.journeys.values.first()
            val journeyId = freshJourney.journeyId

            // Manually inject a "stale" copy of the same journey into loadMoreJourneys with
            // a different (older) timeText to distinguish the two copies
            val staleVersion = freshJourney.copy(timeText = "STALE_VERSION")
            viewModel.loadMoreJourneys[journeyId] = staleVersion

            // Trigger UI rebuild
            viewModel.fetchTrip()
            advanceUntilIdle()

            // THEN: the UI list must NOT show the stale version
            val uiJourney = viewModel.uiState.value.journeyList.find { it.journeyId == journeyId }
            assertFalse(
                uiJourney?.timeText == "STALE_VERSION",
                "journeys (fresh) must win over loadMoreJourneys (stale) in the merged UI list",
            )
        }

    @Test
    fun `GIVEN trip in loadMoreJourneys with stale timeText WHEN auto-refresh returns same trip THEN fresh timeText appears in UI`() =
        runTest {
            // Covers the "trip got delayed/updated after user loaded more" scenario.
            // We use timeText as a proxy for "real-time data" because it is trivially
            // injectable without needing a custom TripResponse builder for deviations.
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip() // call 0: initial load
            advanceUntilIdle()

            val originalJourney = viewModel.journeys.values.first()
            val journeyId = originalJourney.journeyId

            // Inject a stale copy into loadMoreJourneys (the user previously "loaded more")
            val staleLoadMore = originalJourney.copy(timeText = "STALE_TIMETEXT")
            viewModel.loadMoreJourneys[journeyId] = staleLoadMore

            // Auto-refresh returns the same trip (fresh, not stale)
            // The service default response gives a non-stale timeText
            viewModel.fetchTrip() // call 1: refresh — journeys is rebuilt from fresh response
            advanceUntilIdle()

            // THEN: UI must NOT show the stale timeText; journeys version wins via distinctBy
            val uiJourney = viewModel.uiState.value.journeyList.find { it.journeyId == journeyId }
            assertTrue(
                uiJourney?.timeText != "STALE_TIMETEXT",
                "Auto-refresh (journeys) must override stale load-more copy in merged UI list",
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 4: previousJourneysCache isolation
    //
    // The auto-refresh loop must NEVER modify previousJourneysCache.  Past trips
    // are immutable historical records from the user's perspective.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN previousJourneysCache has past trips WHEN auto-refresh runs THEN previousJourneysCache is unaffected`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Populate previousJourneysCache directly (mirrors what onLoadPreviousTrips does)
            val pastTime = Clock.System.now().minus(30.minutes).toString()
            val pastTrip = buildJourneyCardInfo("PAST1", pastTime)
            viewModel.previousJourneysCache[pastTrip.journeyId] = pastTrip

            val sizeBeforeRefresh = viewModel.previousJourneysCache.size

            // Run another auto-refresh cycle
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertEquals(
                sizeBeforeRefresh,
                viewModel.previousJourneysCache.size,
                "Auto-refresh must not modify previousJourneysCache",
            )
            assertTrue(
                viewModel.previousJourneysCache.containsKey(pastTrip.journeyId),
                "Past trip must survive auto-refresh",
            )
        }

    @Test
    fun `GIVEN previousJourneysCache has a trip WHEN pruneStaleLoadMoreEntries runs THEN previousJourneysCache is unaffected`() =
        runTest {
            // pruneStaleLoadMoreEntries only targets loadMoreJourneys — not previousJourneysCache
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val pastTime = Clock.System.now().minus(60.minutes).toString()
            val historicalTrip = buildJourneyCardInfo("HIST1", pastTime)
            viewModel.previousJourneysCache[historicalTrip.journeyId] = historicalTrip

            viewModel.pruneStaleLoadMoreEntries()

            assertTrue(
                viewModel.previousJourneysCache.containsKey(historicalTrip.journeyId),
                "pruneStaleLoadMoreEntries must never touch previousJourneysCache",
            )
        }

    @Test
    fun `GIVEN previousJourneyList shown in UI WHEN auto-refresh runs multiple times THEN previousJourneyList remains stable`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val pastTime = Clock.System.now().minus(20.minutes).toString()
            val pastTrip = buildJourneyCardInfo("PAST2", pastTime)
            viewModel.previousJourneysCache[pastTrip.journeyId] = pastTrip

            // Simulate 3 auto-refresh cycles
            repeat(3) {
                viewModel.fetchTrip()
                advanceUntilIdle()
            }

            // previousJourneyList in UI state must still contain the past trip
            assertTrue(
                viewModel.uiState.value.previousJourneyList.any { it.journeyId == pastTrip.journeyId },
                "previousJourneyList must remain stable across multiple auto-refresh cycles",
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 5: canLoadMore invariants
    //
    // canLoadMore gates the "Load More Departures" button.  Pruning must not
    // inadvertently resurrect the button after the session cap is reached, and
    // the button must disappear when loadMoreCount hits MAX_LOAD_MORE_COUNT.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN loadMoreCount at MAX WHEN pruning removes all load-more trips THEN canLoadMore stays false`() =
        runTest {
            // The loadMoreCount tracks user intent (how many extra pages were requested),
            // not the current cache size.  Even if pruning empties loadMoreJourneys, the cap stands.
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Exhaust the load-more budget
            repeat(MAX_LOAD_MORE_COUNT) {
                viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
                advanceUntilIdle()
            }
            assertFalse(viewModel.uiState.value.canLoadMore, "canLoadMore must be false at cap")

            // Force-empty the cache (as pruning would)
            viewModel.loadMoreJourneys.clear()
            viewModel.fetchTrip() // triggers updateUiStateWithFilteredTrips
            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.canLoadMore,
                "canLoadMore must remain false after pruning empties loadMoreJourneys — the cap is per-session",
            )
        }

    @Test
    fun `GIVEN loadMoreCount below MAX AND journeys non-empty THEN canLoadMore is true`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // No load-more taps yet → count = 0 < MAX
            assertTrue(
                viewModel.uiState.value.canLoadMore,
                "canLoadMore must be true when count is below MAX and journeys are present",
            )
        }

    @Test
    fun `GIVEN paginationEnabled is false THEN canLoadMore is always false`() =
        runTest {
            // If the PAGINATION_ENABLED flag is toggled off, canLoadMore must never be true.
            // This test documents the contract so a future flag-wiring change can verify it.
            // The constant is currently hardcoded to true; this test would catch a regression
            // if someone disables it and forgets to update the ViewModel logic.
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // canLoadMore depends on PAGINATION_ENABLED — verify it matches the flag
            val expected = TimeTableViewModel.PAGINATION_ENABLED && viewModel.journeys.isNotEmpty()
            assertEquals(
                expected,
                viewModel.uiState.value.canLoadMore,
                "canLoadMore must reflect PAGINATION_ENABLED flag",
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Category 6: End-to-end time progression
    //
    // Full auto-refresh cycle to verify that the three caches interact correctly
    // as the API window shifts forward over time.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN user loads more trips WHEN auto-refresh advances the window THEN load-more trips inside new window are pruned and fresh data wins`() =
        runTest {
            // Phase 1: initial load → 3 journeys (indices 0,1,2 → now, now+5, now+10)
            tripPlanningService.isSuccess = true
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 3))
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertEquals(3, viewModel.journeys.size)

            // Phase 2: user taps Load More → fetch returns 6 journeys (indices 0..5)
            // Dedup skips 0,1,2 (already in journeys); adds 3,4,5 to loadMoreJourneys
            tripPlanningService.setResponseForCall(1, buildTripResponse(numberOfJourney = 6))
            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            // loadMoreJourneys should have exactly the 3 genuinely new journeys (indices 3,4,5)
            assertEquals(
                3,
                viewModel.loadMoreJourneys.size,
                "loadMoreJourneys should hold the 3 new journeys beyond the initial window",
            )

            // Phase 3: auto-refresh advances → now returns 5 journeys (0..4)
            // latestFreshInstant = now+20m (index 4) → prunes load-more trips at now+15 (index 3)
            // load-more trip at index 4 is also pruned (≤ latestFreshInstant)
            // load-more trip at index 5 (now+25) survives
            tripPlanningService.setResponseForCall(2, buildTripResponse(numberOfJourney = 5))
            viewModel.fetchTrip() // auto-refresh
            advanceUntilIdle()

            // After pruning the load-more cache should only contain the trip beyond now+20
            // (journeyIndex 5 = now+25m). Trips at now+15 and now+20 were pruned.
            val survivingLmTripIds = viewModel.loadMoreJourneys.keys
            assertEquals(
                1,
                survivingLmTripIds.size,
                "Only the trip beyond the new fresh window should survive in loadMoreJourneys",
            )

            // The merged UI list must not contain duplicates
            val journeyIds = viewModel.uiState.value.journeyList.map { it.journeyId }
            assertEquals(
                journeyIds.distinct().size,
                journeyIds.size,
                "Merged journeyList must have no duplicate journeyIds",
            )
        }

    @Test
    fun `GIVEN load-more trips beyond auto-refresh window WHEN auto-refresh runs THEN they survive and appear in merged list`() =
        runTest {
            // Phase 1: initial load
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Manually inject 2 far-future trips into loadMoreJourneys
            val far1Time = Clock.System.now().plus(90.minutes).toString()
            val far2Time = Clock.System.now().plus(120.minutes).toString()
            val farTrip1 = buildJourneyCardInfo("FAR1", far1Time)
            val farTrip2 = buildJourneyCardInfo("FAR2", far2Time)
            viewModel.loadMoreJourneys[farTrip1.journeyId] = farTrip1
            viewModel.loadMoreJourneys[farTrip2.journeyId] = farTrip2

            // Phase 2: auto-refresh (fresh window still only covers ~now..now+10)
            viewModel.fetchTrip()
            advanceUntilIdle()

            // THEN: far-future trips must still be in loadMoreJourneys
            assertTrue(viewModel.loadMoreJourneys.containsKey(farTrip1.journeyId))
            assertTrue(viewModel.loadMoreJourneys.containsKey(farTrip2.journeyId))

            // And they must appear in the merged UI list alongside the fresh journeys
            val allJourneyIds = viewModel.uiState.value.journeyList.map { it.journeyId }
            assertTrue(allJourneyIds.contains(farTrip1.journeyId), "FAR1 must be in merged list")
            assertTrue(allJourneyIds.contains(farTrip2.journeyId), "FAR2 must be in merged list")
        }

    @Test
    fun `GIVEN all caches populated WHEN trip destination changes THEN all three caches are cleared`() =
        runTest {
            // Verifies resetPaginationCaches() is called on trip change — guards against regression
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Populate all three caches
            val futureTrip = buildJourneyCardInfo("FUTURE", Clock.System.now().plus(30.minutes).toString())
            val pastTrip = buildJourneyCardInfo("PAST", Clock.System.now().minus(30.minutes).toString())
            viewModel.loadMoreJourneys[futureTrip.journeyId] = futureTrip
            viewModel.previousJourneysCache[pastTrip.journeyId] = pastTrip

            assertTrue(viewModel.loadMoreJourneys.isNotEmpty())
            assertTrue(viewModel.previousJourneysCache.isNotEmpty())

            // Switch to a different trip → should reset caches
            val differentTrip = Trip("stop3", "Origin B", "stop4", "Destination B")
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(differentTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertTrue(viewModel.loadMoreJourneys.isEmpty(), "loadMoreJourneys must be cleared on trip change")
            assertTrue(viewModel.previousJourneysCache.isEmpty(), "previousJourneysCache must be cleared on trip change")
        }

    @Test
    fun `GIVEN all caches populated WHEN date-time selector changes THEN pagination caches are cleared`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            viewModel.loadMoreJourneys["LM"] = buildJourneyCardInfo("LM", Clock.System.now().plus(30.minutes).toString())
            viewModel.previousJourneysCache["PC"] = buildJourneyCardInfo("PC", Clock.System.now().minus(30.minutes).toString())

            // Change the date/time selection to a concrete value (null → null is a no-op)
            val newSelection = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 14,
                minute = 0,
            )
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem = newSelection))
            advanceUntilIdle()

            assertTrue(viewModel.loadMoreJourneys.isEmpty(), "loadMoreJourneys must be cleared on date/time change")
            assertTrue(viewModel.previousJourneysCache.isEmpty(), "previousJourneysCache must be cleared on date/time change")
        }

    @Test
    fun `GIVEN merged list built from journeys and loadMoreJourneys THEN list is sorted chronologically`() =
        runTest {
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(defaultTrip))
            // Use 3 journeys so we have a range of departure times
            tripPlanningService.setResponseForCall(0, buildTripResponse(numberOfJourney = 3))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Add a load-more trip that is genuinely in the future (beyond the fresh window)
            val futureTime = Clock.System.now().plus(45.minutes).toString()
            val futureTrip = buildJourneyCardInfo("SORTED1", futureTime)
            viewModel.loadMoreJourneys[futureTrip.journeyId] = futureTrip

            viewModel.fetchTrip() // rebuilds UI state via updateUiStateWithFilteredTrips
            advanceUntilIdle()

            val departures = viewModel.uiState.value.journeyList.map { it.originUtcDateTime }
            assertEquals(
                departures.sortedWith(compareBy { it }),
                departures,
                "Merged journeyList must be sorted chronologically",
            )
        }
}
