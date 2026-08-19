package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeRateLimiter
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.fakes.FakeShareManager
import xyz.ksharma.krail.core.testing.fakes.FakeTripPlanningService
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * The auto-refresh cadence, tested against the clock instead of against "some time passed".
 *
 * The previous version of this asserted `triggerCount > 0` after advancing past the interval,
 * which the loop satisfies before any time passes at all — the `onStart` body runs one
 * iteration the moment a subscriber attaches. It could not have caught a wrong interval, a
 * missing delay, or a double fire. These pin the actual contract: one trigger on subscribe,
 * nothing until the interval elapses, then exactly one more.
 */
@OptIn(ExperimentalTime::class)
class TimeTableAutoRefreshTest {

    private val sandook = FakeSandook()
    private val preferences = FakeSandookPreferences()
    private val analytics = FakeAnalytics()
    private val shareManager = FakeShareManager()
    private val tripPlanningService = FakeTripPlanningService()
    private val rateLimiter = FakeRateLimiter()
    private val festivalManager = FakeFestivalManager()
    private val flag = FakeFlag()

    @BeforeTest
    fun setUp() {
        TimeTableViewModel.resetSavePromptSessionFlagForTest()
    }

    @Test
    fun `GIVEN journeys on screen WHEN the interval has not elapsed THEN nothing refreshes, then exactly one does`() =
        krailRunTest {
            val viewModel = buildViewModel()
            loadJourneys(viewModel)
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty(), "journeys must be on screen")
            rateLimiter.reset()

            val collectJob = collectAutoRefresh(viewModel)
            // The loop's first iteration runs on subscribe — that is the refresh-on-entry.
            val onSubscribe = rateLimiter.triggerCount
            assertEquals(1, onSubscribe, "subscribing refreshes once, immediately")

            pumpOnce(AUTO_REFRESH - 1.seconds)
            assertEquals(onSubscribe, rateLimiter.triggerCount, "no refresh before the interval elapses")

            pumpOnce(2.seconds)
            assertEquals(onSubscribe + 1, rateLimiter.triggerCount, "exactly one refresh once it elapses")

            collectJob.cancel()
        }

    @Test
    fun `GIVEN an empty journey list WHEN intervals elapse THEN nothing is ever refreshed`() = krailRunTest {
        val viewModel = buildViewModel()
        rateLimiter.reset()

        val collectJob = collectAutoRefresh(viewModel)
        assertEquals(0, rateLimiter.triggerCount, "nothing on screen — nothing to refresh")

        repeat(INTERVALS_TO_PUMP) { pumpOnce(AUTO_REFRESH + 1.seconds) }

        assertEquals(0, rateLimiter.triggerCount, "an empty screen must never auto-refresh")
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a Leave-at pinned later today WHEN intervals elapse THEN nothing auto-refreshes`() = krailRunTest {
        val viewModel = buildViewModel()
        loadJourneys(viewModel)
        moveToLocalMorning()
        pinLeaveAt(viewModel, hour = EVENING_HOUR)
        rateLimiter.reset()

        val collectJob = collectAutoRefresh(viewModel)
        repeat(INTERVALS_TO_PUMP) { pumpOnce(AUTO_REFRESH + 1.seconds) }

        assertEquals(
            0,
            rateLimiter.triggerCount,
            "a timetable pinned to a future time must not be re-fetched, same day or not",
        )
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a Leave-at pinned earlier today WHEN intervals elapse THEN it still auto-refreshes`() = krailRunTest {
        val viewModel = buildViewModel()
        loadJourneys(viewModel)
        moveToLocalMorning()
        pinLeaveAt(viewModel, hour = EARLY_HOUR)
        rateLimiter.reset()

        val collectJob = collectAutoRefresh(viewModel)
        pumpOnce(AUTO_REFRESH + 1.seconds)

        assertEquals(
            2,
            rateLimiter.triggerCount,
            "a selection already in the past is a live board — it must keep refreshing",
        )
        collectJob.cancel()
    }

    // region helpers

    private fun KrailTestScope.buildViewModel() = TimeTableViewModel(
        tripPlanningService = tripPlanningService,
        rateLimiter = rateLimiter,
        sandook = sandook,
        preferences = preferences,
        analytics = analytics,
        shareManager = shareManager,
        ioDispatcher = ioDispatcher,
        festivalManager = festivalManager,
        flag = flag,
        appReviewManager = FakeAppReviewManager(),
        clock = clock,
    )

    private fun KrailTestScope.loadJourneys(viewModel: TimeTableViewModel) {
        tripPlanningService.isSuccess = true
        viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(TRIP))
        viewModel.fetchTrip()
        pumpOnce(SETTLE)
    }

    /** Subscribes to the auto-refresh flow and lets its first loop iteration run. */
    private fun KrailTestScope.collectAutoRefresh(viewModel: TimeTableViewModel): Job {
        val job = launch { viewModel.autoRefreshTimeTable.collect {} }
        runCurrent()
        return job
    }

    /**
     * Advances virtual time to 08:00 local on the following day, so "later today" and
     * "earlier today" mean the same thing whatever timezone the suite runs in.
     */
    private fun KrailTestScope.moveToLocalMorning() {
        val zone = TimeZone.currentSystemDefault()
        val today = clock.now().toLocalDateTime(zone).date
        val morning = LocalDateTime(today.plus(1, DateTimeUnit.DAY), LocalTime(MORNING_HOUR, 0))
            .toInstant(zone)
        pumpOnce(morning - clock.now())
    }

    private fun KrailTestScope.pinLeaveAt(viewModel: TimeTableViewModel, hour: Int) {
        val localNow = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        viewModel.onEvent(
            TimeTableUiEvent.DateTimeSelectionChanged(
                DateTimeSelectionItem(
                    option = JourneyTimeOptions.LEAVE,
                    hour = hour,
                    minute = 0,
                    date = localNow.date,
                ),
            ),
        )
        runCurrent()
    }

    // endregion

    private companion object {
        val AUTO_REFRESH = TimeTableViewModel.AUTO_REFRESH_TIME_TABLE_DURATION
        val SETTLE = 1.seconds
        const val INTERVALS_TO_PUMP = 10
        const val MORNING_HOUR = 8
        const val EARLY_HOUR = 6
        const val EVENING_HOUR = 18
        val TRIP = Trip(
            fromStopId = "stop1",
            fromStopName = "S1",
            toStopId = "stop2",
            toStopName = "S2",
        )
    }
}
