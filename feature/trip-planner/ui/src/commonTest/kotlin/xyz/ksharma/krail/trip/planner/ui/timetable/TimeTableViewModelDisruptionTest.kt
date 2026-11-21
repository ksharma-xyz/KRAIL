package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.connectivity.TransportState
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeConnectivityObserver
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeRateLimiter
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.fakes.FakeShareManager
import xyz.ksharma.krail.core.testing.fakes.FakeTripPlanningService
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.ReplacementBusTripFixtures
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * What the timetable screen reports when it shows a replacement-bus banner. The trip is a real NSW
 * reply (Cronulla to Central, 2026-11-21) in which every leg is a temporary bus.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimeTableViewModelDisruptionTest {

    private val analytics = FakeAnalytics()
    private val tripPlanningService = FakeTripPlanningService()
    private val testDispatcher = StandardTestDispatcher()
    private val trip = Trip(fromStopId = "CRON", fromStopName = "Cronulla", toStopId = "CEN", toStopName = "Central")
    private lateinit var viewModel: TimeTableViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TimeTableViewModel.resetSavePromptSessionFlagForTest()
        val response = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
            .decodeFromString<TripResponse>(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL)
        // The screen refreshes on a timer, so every call returns the same replacement-bus trip.
        repeat(REFRESH_CALLS) { tripPlanningService.setResponseForCall(it, response) }
        viewModel = TimeTableViewModel(
            connectivity = FakeConnectivityObserver(initial = TransportState.Up),
            tripPlanningService = tripPlanningService,
            rateLimiter = FakeRateLimiter(),
            sandook = FakeSandook(),
            preferences = FakeSandookPreferences(),
            analytics = analytics,
            shareManager = FakeShareManager(),
            ioDispatcher = testDispatcher,
            festivalManager = FakeFestivalManager(),
            flag = FakeFlag(),
            appReviewManager = FakeAppReviewManager(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loadReplacementTrip() {
        tripPlanningService.isSuccess = true
        viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
        viewModel.fetchTrip()
    }

    @Test
    fun `GIVEN replacement buses in the results THEN the state carries the replacement banner`() = runTest {
        loadReplacementTrip()
        advanceUntilIdle()

        assertIs<DisruptionBannerState.ReplacementBuses>(viewModel.uiState.value.disruptionBanner)
    }

    @Test
    fun `GIVEN the banner composes WHEN the event arrives twice THEN shown is reported once for the planner`() = runTest {
        loadReplacementTrip()
        advanceUntilIdle()

        repeat(2) { viewModel.onEvent(TimeTableUiEvent.DisruptionBannerDisplayed) }
        advanceUntilIdle()

        val shown = analytics.getTrackedEvents("disruption_status")
        assertEquals(1, shown.size)
        assertEquals("planner", shown.single().properties?.get("transportMode"))
        assertEquals("replacement_bus", shown.single().properties?.get("reason"))
    }

    @Test
    fun `GIVEN the banner WHEN it is expanded and its link tapped THEN both ride the info tile event`() = runTest {
        loadReplacementTrip()
        advanceUntilIdle()

        viewModel.onEvent(TimeTableUiEvent.DisruptionBannerExpanded)
        viewModel.onEvent(TimeTableUiEvent.DisruptionBannerCtaClicked("https://transportnsw.info/alerts"))
        advanceUntilIdle()

        val taps = analytics.getTrackedEvents("info_tile_interaction")
        assertEquals(2, taps.size)
        assertEquals(listOf("disruption_planner", "disruption_planner"), taps.map { it.properties?.get("key") })
    }

    private companion object {
        const val REFRESH_CALLS = 10
    }
}
