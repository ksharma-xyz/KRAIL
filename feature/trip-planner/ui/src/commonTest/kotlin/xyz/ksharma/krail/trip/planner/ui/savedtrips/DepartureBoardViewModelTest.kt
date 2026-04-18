package xyz.ksharma.krail.trip.planner.ui.savedtrips

import app.cash.turbine.test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import xyz.ksharma.krail.departures.ui.DepartureBoardConfig
import xyz.ksharma.krail.departures.ui.DepartureBoardRepository
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [DepartureBoardViewModel].
 *
 * Covers the **departure board section on the saved trips screen** — the accordion that shows
 * live departures for every unique stop across the user's saved trips. At most one card is
 * expanded at a time; the expanded card polls the network while collapsed cards only read the
 * in-memory cache.
 *
 * For the single-stop departure sheet that opens when a user taps a stop, see
 * [DeparturesViewModelTest] in the `feature:departures:ui` module.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DepartureBoardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testConfig = DepartureBoardConfig(
        refreshIntervalMs = 1_000L,
        previousDeparturesWindowMinutes = 30L,
    )

    private lateinit var fakeService: FakeDepartureBoardService
    private lateinit var repository: DepartureBoardRepository
    private lateinit var viewModel: DepartureBoardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeDepartureBoardService(response = buildResponse(count = 2))
        repository = DepartureBoardRepository(
            departuresService = fakeService,
            ioDispatcher = testDispatcher,
            config = testConfig,
        )
        viewModel = DepartureBoardViewModel(repository = repository, analytics = NoOpAnalytics)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `Given no trips When entries collected Then emits empty list`() = runTest {
        viewModel.entries.test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty(), "No trips → entries must be empty")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given no card expanded When expandedStopId checked Then it is null`() = runTest {
        assertNull(viewModel.expandedStopId.value)
    }

    // ── setTrips ──────────────────────────────────────────────────────────────

    @Test
    fun `Given trip with two distinct stops When setTrips called Then entries has two items`() =
        runTest {
            viewModel.entries.test {
                awaitItem() // initial empty list

                viewModel.setTrips(buildTrips(TRIP_A))
                advanceUntilIdle()

                val updated = awaitItem()
                assertEquals(2, updated.size, "One trip with two distinct stops → 2 entries")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given two trips sharing a stop When setTrips called Then deduplicates to unique stops`() =
        runTest {
            // TRIP_A: (STOP_A_FROM, STOP_A_TO)
            // TRIP_SHARED: (STOP_A_FROM, STOP_C)  ← shares STOP_A_FROM
            // Unique: STOP_A_FROM, STOP_A_TO, STOP_C → 3
            val trips = buildTrips(TRIP_A, TRIP_SHARED_FROM_STOP)

            viewModel.entries.test {
                awaitItem() // initial

                viewModel.setTrips(trips)
                advanceUntilIdle()

                val updated = awaitItem()
                assertEquals(3, updated.size, "Shared stop must be deduplicated")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given expanded card When setTrips removes that stop Then expandedStopId collapses to null`() =
        runTest {
            viewModel.setTrips(buildTrips(TRIP_A))
            viewModel.onCardExpand(STOP_A_FROM)
            assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)

            // New trips do NOT include STOP_A_FROM
            viewModel.setTrips(buildTrips(TRIP_B))
            advanceUntilIdle()

            assertNull(
                viewModel.expandedStopId.value,
                "Expanded stop removed from trip list must auto-collapse",
            )
        }

    @Test
    fun `Given expanded card When setTrips keeps that stop Then expandedStopId is unchanged`() =
        runTest {
            viewModel.setTrips(buildTrips(TRIP_A))
            viewModel.onCardExpand(STOP_A_FROM)
            assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)

            // New trips still include STOP_A_FROM
            viewModel.setTrips(buildTrips(TRIP_A, TRIP_B))
            advanceUntilIdle()

            assertEquals(
                STOP_A_FROM,
                viewModel.expandedStopId.value,
                "Expanded stop still in trip list must remain expanded",
            )
        }

    @Test
    fun `Given trips set When setTrips called with empty list Then entries becomes empty`() =
        runTest {
            viewModel.entries.test {
                awaitItem() // initial

                viewModel.setTrips(buildTrips(TRIP_A))
                advanceUntilIdle()
                awaitItem() // 2-entry list

                viewModel.setTrips(persistentListOf())
                advanceUntilIdle()

                val empty = awaitItem()
                assertTrue(empty.isEmpty(), "Clearing trips must empty the entries list")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── onCardExpand / onCardCollapse ─────────────────────────────────────────

    @Test
    fun `Given no expanded card When onCardExpand called Then expandedStopId is set`() = runTest {
        assertNull(viewModel.expandedStopId.value)

        viewModel.onCardExpand(STOP_A_FROM)

        assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)
    }

    @Test
    fun `Given expanded card When onCardExpand called with same stop Then expandedStopId unchanged`() =
        runTest {
            viewModel.onCardExpand(STOP_A_FROM)
            assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)

            viewModel.onCardExpand(STOP_A_FROM) // same stop — should be NOOP

            assertEquals(
                STOP_A_FROM,
                viewModel.expandedStopId.value,
                "Expanding the already-expanded stop must be a NOOP",
            )
        }

    @Test
    fun `Given expanded card When onCardExpand called with different stop Then expandedStopId switches`() =
        runTest {
            viewModel.onCardExpand(STOP_A_FROM)
            assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)

            viewModel.onCardExpand(STOP_B_FROM)

            assertEquals(STOP_B_FROM, viewModel.expandedStopId.value)
        }

    @Test
    fun `Given expanded card When onCardCollapse called Then expandedStopId is null`() = runTest {
        viewModel.onCardExpand(STOP_A_FROM)
        assertEquals(STOP_A_FROM, viewModel.expandedStopId.value)

        viewModel.onCardCollapse()

        assertNull(viewModel.expandedStopId.value)
    }

    // ── entries — polling behaviour ───────────────────────────────────────────

    @Test
    fun `Given trips set and no card expanded When entries collected Then no API calls made`() =
        runTest {
            viewModel.setTrips(buildTrips(TRIP_A))

            viewModel.entries.test {
                advanceUntilIdle()

                assertEquals(
                    0,
                    fakeService.callCount,
                    "Collapsed stops use observeStop — no API calls expected",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given trips set and card expanded When entries collected Then expanded stop is polled`() =
        runTest {
            viewModel.setTrips(buildTrips(TRIP_A))
            viewModel.onCardExpand(STOP_A_FROM)

            viewModel.entries.test {
                // Advance past one poll interval (not advanceUntilIdle — pollStop loops forever)
                advanceTimeBy(testConfig.refreshIntervalMs + 100L)

                assertTrue(
                    fakeService.callCount >= 1,
                    "Expanded stop uses pollStop — at least one API call expected",
                )
                cancelAndIgnoreRemainingEvents()
            }
            // Let WhileSubscribed(5_000) expire so stateIn cancels the upstream poll loop
            advanceTimeBy(5_100L)
        }

    @Test
    fun `Given expanded stop When card collapses Then no more polling calls after collapse`() =
        runTest {
            viewModel.setTrips(buildTrips(TRIP_A))
            viewModel.onCardExpand(STOP_A_FROM)

            viewModel.entries.test {
                // Advance past one poll interval (not advanceUntilIdle — pollStop loops forever)
                advanceTimeBy(testConfig.refreshIntervalMs + 100L)
                val callsWhileExpanded = fakeService.callCount
                assertTrue(callsWhileExpanded >= 1)

                viewModel.onCardCollapse()
                // After collapse flatMapLatest switches to observeStop — no more API calls
                advanceTimeBy(testConfig.refreshIntervalMs + 100L)

                assertEquals(
                    callsWhileExpanded,
                    fakeService.callCount,
                    "Collapsing must stop further polling calls within the refresh window",
                )
                cancelAndIgnoreRemainingEvents()
            }
            // Let WhileSubscribed(5_000) expire so stateIn cancels the upstream
            advanceTimeBy(5_100L)
        }

    // ── onRefreshStop ─────────────────────────────────────────────────────────

    @Test
    fun `Given expanded stop When onRefreshStop called Then extra API call is made`() = runTest {
        // Populate repository cache via one poll cycle (not advanceUntilIdle — pollStop loops forever)
        repository.pollStop(STOP_A_FROM).test {
            awaitItem()
            advanceTimeBy(testConfig.refreshIntervalMs + 100L)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        val callsBefore = fakeService.callCount

        viewModel.onRefreshStop(STOP_A_FROM)
        advanceUntilIdle()

        assertEquals(callsBefore + 1, fakeService.callCount, "onRefreshStop must trigger one extra API call")
    }

    // ── onLoadPreviousDepartures ──────────────────────────────────────────────

    @Test
    fun `Given stop with data When onLoadPreviousDepartures called Then previousDepartures populated`() =
        runTest {
            val pastTime = "2020-01-01T00:00:00Z"
            fakeService.response = buildResponse(count = 1, plannedTime = pastTime)

            // Populate repository cache via one poll cycle (not advanceUntilIdle — pollStop loops forever)
            repository.pollStop(STOP_A_FROM).test {
                awaitItem()
                advanceTimeBy(testConfig.refreshIntervalMs + 100L)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.onLoadPreviousDepartures(STOP_A_FROM)
            advanceUntilIdle()

            repository.observeStop(STOP_A_FROM).test {
                val state = awaitItem()
                assertFalse(state.isPreviousLoading, "isPreviousLoading must be false when complete")
                assertTrue(
                    state.previousDepartures.isNotEmpty(),
                    "previousDepartures must be populated with past departures",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given stop When onLoadPreviousDepartures API fails Then isPreviousLoading is reset`() =
        runTest {
            fakeService.shouldThrow = true

            // Call without any prior data — error path
            viewModel.onLoadPreviousDepartures(STOP_A_FROM)
            advanceUntilIdle()

            repository.observeStop(STOP_A_FROM).test {
                val state = awaitItem()
                assertFalse(state.isPreviousLoading, "isPreviousLoading must be cleared even on failure")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        count: Int,
        plannedTime: String = "2099-01-01T09:00:00Z",
    ) = DepartureMonitorResponse(
        stopEvents = List(count) {
            DepartureMonitorResponse.StopEvent(
                departureTimePlanned = plannedTime,
                departureTimeEstimated = null,
                transportation = DepartureMonitorResponse.Transportation(
                    id = "T1",
                    disassembledName = "T1",
                    destination = DepartureMonitorResponse.Destination(
                        id = "dest",
                        name = "Destination $it",
                    ),
                    product = DepartureMonitorResponse.Product(cls = 1, iconId = 1, name = "Train"),
                ),
                location = null,
            )
        },
    )

    private fun buildTrips(vararg trips: Trip): ImmutableList<Trip> =
        trips.toList().toImmutableList()

    private companion object {
        const val STOP_A_FROM = "10111010"
        const val STOP_A_TO = "10111020"
        const val STOP_B_FROM = "10111030"
        const val STOP_B_TO = "10111040"
        const val STOP_C = "10111050"

        val TRIP_A = Trip(
            fromStopId = STOP_A_FROM,
            fromStopName = "Stop A From",
            toStopId = STOP_A_TO,
            toStopName = "Stop A To",
        )
        val TRIP_B = Trip(
            fromStopId = STOP_B_FROM,
            fromStopName = "Stop B From",
            toStopId = STOP_B_TO,
            toStopName = "Stop B To",
        )
        val TRIP_SHARED_FROM_STOP = Trip(
            fromStopId = STOP_A_FROM,
            fromStopName = "Stop A From",
            toStopId = STOP_C,
            toStopName = "Stop C",
        )
    }
}

/** No-op [Analytics] for tests — discards all events. */
private object NoOpAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}

/**
 * Fake [DeparturesService] for [DepartureBoardViewModelTest].
 */
class FakeDepartureBoardService(
    var response: DepartureMonitorResponse = DepartureMonitorResponse(stopEvents = emptyList()),
    var shouldThrow: Boolean = false,
) : DeparturesService {

    var callCount = 0
        private set

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): DepartureMonitorResponse {
        callCount++
        if (shouldThrow) throw RuntimeException("Fake network error")
        return response
    }
}
