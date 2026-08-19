package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.festival.model.FixedDateFestival
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
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

/**
 * The loading greeting is a function of what day it is, so it has to be recomputed when the
 * day changes.
 *
 * A ViewModel outlives a night easily — the app sits in the background and the screen is
 * re-entered the next morning. Computing the greeting once per ViewModel meant that rider
 * saw yesterday's festival. Computing it fresh on every screen entry would fix that but
 * re-roll the random emoji each time, which is what the `by lazy` was really protecting;
 * the greeting is therefore cached per calendar day.
 */
@OptIn(ExperimentalTime::class)
class TimeTableGreetingTest {

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
    fun `GIVEN a festival tomorrow WHEN the screen is re-entered after midnight THEN the greeting changes`() =
        krailRunTest {
            val zone = TimeZone.currentSystemDefault()
            val today = clock.now().toLocalDateTime(zone).date
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            festivalManager.festivalForDate[today] = festival(TODAY_GREETING)
            festivalManager.festivalForDate[tomorrow] = festival(TOMORROW_GREETING)

            val viewModel = buildViewModel()

            val firstVisit = enterScreen(viewModel)
            assertEquals(
                TODAY_GREETING,
                viewModel.uiState.value.loadingEmoji?.greeting,
                "the greeting must match today's festival",
            )
            firstVisit.cancel()

            // The app sits in the background overnight; the ViewModel survives.
            pumpOnce(midnightOf(tomorrow, zone) - clock.now())

            val secondVisit = enterScreen(viewModel)
            assertEquals(
                TOMORROW_GREETING,
                viewModel.uiState.value.loadingEmoji?.greeting,
                "after midnight the greeting must be recomputed for the new day",
            )
            secondVisit.cancel()
        }

    @Test
    fun `GIVEN the same day WHEN the screen is re-entered THEN the greeting is unchanged`() = krailRunTest {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        festivalManager.festivalForDate[today] = festival(TODAY_GREETING)

        val viewModel = buildViewModel()

        val firstVisit = enterScreen(viewModel)
        val first = viewModel.uiState.value.loadingEmoji
        firstVisit.cancel()

        val secondVisit = enterScreen(viewModel)
        val second = viewModel.uiState.value.loadingEmoji
        secondVisit.cancel()

        assertEquals(first, second, "the emoji must not re-roll on every screen entry within a day")
    }

    // region helpers

    /** Subscribing to `isLoading` is what "the screen became visible" means to the ViewModel. */
    private fun KrailTestScope.enterScreen(viewModel: TimeTableViewModel): Job {
        val job = launch { viewModel.isLoading.collect {} }
        runCurrent()
        return job
    }

    private fun midnightOf(date: LocalDate, zone: TimeZone) =
        LocalDateTime(date, LocalTime(0, 1)).toInstant(zone)

    private fun festival(greeting: String) = FixedDateFestival(
        type = "Test",
        month = 1,
        day = 1,
        emojiList = listOf("🎉"),
        greeting = greeting,
    )

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

    // endregion

    private companion object {
        const val TODAY_GREETING = "Today greeting"
        const val TOMORROW_GREETING = "Tomorrow greeting"
    }
}
