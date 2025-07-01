package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeRateLimiter
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeTripPlanningService
import xyz.ksharma.core.test.fakes.FakeTripResponseBuilder.buildTripResponse
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.datetime.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Stop
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.JOURNEY_ENDED_CACHE_THRESHOLD_TIME
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.REFRESH_TIME_TEXT_DURATION
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class TimeTableViewModelTest {

    private val sandook: Sandook = FakeSandook()
    private val fakeAnalytics: Analytics = FakeAnalytics()
    private val tripPlanningService = FakeTripPlanningService()
    private val rateLimiter = FakeRateLimiter()
    private lateinit var viewModel: TimeTableViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TimeTableViewModel(
            tripPlanningService = tripPlanningService,
            rateLimiter = rateLimiter,
            sandook = sandook,
            analytics = fakeAnalytics,
            ioDispatcher = testDispatcher,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN initial state WHEN observer is active THEN fetchTrip and trackScreenViewEvent should be called`() =
        runTest {
            // Ensure analytics events have not been tracked before observation
            assertFalse((fakeAnalytics as FakeAnalytics).isEventTracked("view_screen"))

            viewModel.isLoading.test {
                val isLoadingState = awaitItem()
                assertEquals(isLoadingState, true)

                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.TimeTable.name,
                )

                cancelAndConsumeRemainingEvents()
            }
        }

    // region Test isActive Flow

    @Test
    fun `GIVEN journeyList is empty in UI State WHEN REFRESH_TIME_TEXT_DURATION passes THEN updateTimeText is not called`() =
        runTest {
            // GIVEN No Journey list in UI State

            // THEN
            viewModel.isActive.test {

                skipItems(1) // initial state

                advanceTimeBy(REFRESH_TIME_TEXT_DURATION.inWholeMilliseconds)
                expectNoEvents()

                advanceTimeBy(REFRESH_TIME_TEXT_DURATION.inWholeMilliseconds)
                expectNoEvents()

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for fetchTrip / Trip API call

    @Test
    fun `GIVEN a trip WHEN LoadTimeTable event is triggered and Trip API is success response THEN UI State must update with journeyList`() =
        runTest {
            // GIVEN a trip
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2"
            )
            tripPlanningService.isSuccess = true

            // THEN verify that the UI state is updated correctly
            viewModel.uiState.test {
                val initialState = awaitItem()
                initialState.run {
                    assertTrue(isLoading)
                    assertNull(initialState.trip)
                    assertFalse(isError)
                    assertFalse(isTripSaved)
                }

                // WHEN the LoadTimeTable event is triggered
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip() // Manually call fetchTrip() to simulate the actual behavior
                awaitItem().run {
                    assertTrue(isLoading)
                    assertFalse(silentLoading)
                    assertFalse(isError)
                    assertTrue(journeyList.isEmpty())
                }

                // need to skip two items, because silentLoading will be toggled, as we manually call fetchTrip()
                skipItems(2)

                awaitItem().run {
                    assertFalse(isLoading)
                    assertFalse(silentLoading)
                    assertTrue(journeyList.isNotEmpty())
                    assertEquals(expected = 1, journeyList.size)
                }

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a trip WHEN LoadTimeTable event is triggered and Trip API is error response THEN UIState should have isError as true`() =
        runTest {
            // GIVEN a trip
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2"
            )
            tripPlanningService.isSuccess = false

            // THEN verify that the UI state is updated correctly
            viewModel.uiState.test {
                val initialState = awaitItem()
                initialState.run {
                    assertTrue(isLoading)
                    assertNull(initialState.trip)
                    assertFalse(isError)
                    assertFalse(isTripSaved)
                }

                // WHEN the LoadTimeTable event is triggered
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip() // Manually call fetchTrip() to simulate the actual behavior
                awaitItem().run {
                    assertTrue(isLoading)
                    assertFalse(silentLoading)
                    assertFalse(isError)
                    assertTrue(journeyList.isEmpty())
                }

                // need to skip two items, because silentLoading will be toggled, as we manually call fetchTrip()
                skipItems(2)

                awaitItem().run {
                    assertFalse(isLoading)
                    assertFalse(silentLoading)
                    assertTrue(journeyList.isEmpty())
                    assertTrue(isError)
                }

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for updateTripsCache

    @Test
    fun `GIVEN journeys returned from Trip api WHEN updateTripsCache is called THEN journeys object in ViewModel should be updated`() =
        runTest {
            // GIVEN Trip Response
            val tripResponse = buildTripResponse(
                numberOfJourney = 2,
                reverseTimeOrder = false,
            )
            viewModel.journeys.clear()
            tripResponse.journeys?.forEachIndexed { index, item ->
                println("tripResponse Journey #$index: ${item.legs?.get(0)?.origin?.arrivalTimeEstimated?.formatTo12HourTime()}")
            }

            // WHEN
            viewModel.updateTripsCache(tripResponse)

            // THEN
            val viewmodelJourneysList = viewModel.journeys.values.toList()
            assertEquals(2, viewmodelJourneysList.size)
        }

    @Test
    fun `GIVEN started journeys in cache are more than threshold WHEN updateTripsCache is called THEN extra started journeys should be removed from viewmodel`() =
        runTest {
            // GIVEN Trip Response
            val tripResponse = buildTripResponse(
                numberOfJourney = 2,
                reverseTimeOrder = false,
            )
            viewModel.journeys.putAll(
                buildStartedJourneysList(numberOfStartedJourneys = 5)
            )
            tripResponse.journeys?.forEachIndexed { index, item ->
                println("tripResponse Journey #$index: ${item.legs?.get(0)?.origin?.arrivalTimeEstimated?.formatTo12HourTime()}")
            }

            // WHEN
            viewModel.updateTripsCache(tripResponse)

            // THEN
            val viewmodelJourneysList = viewModel.journeys.values.toList()
            // 4 because 2 are from API response and 2 is threshold of started journeys
            assertEquals(4, viewmodelJourneysList.size)
        }

    @Test
    fun `GIVEN multiple started journeys in cache WHEN updateTripsCache is called THEN journeys should be sorted and updated`() =
        runTest {
            // GIVEN Trip Response
            val tripResponse = TripResponse()
            viewModel.journeys.putAll(
                buildStartedJourneysList(numberOfStartedJourneys = 4, distortSortOrder = true)
            )
            tripResponse.journeys?.forEachIndexed { index, item ->
                println("tripResponse Journey #$index: ${item.legs?.get(0)?.origin?.arrivalTimeEstimated?.formatTo12HourTime()}")
            }

            // WHEN
            viewModel.updateTripsCache(tripResponse)

            // THEN
            val viewmodelJourneysList = viewModel.journeys.values.toList()
            assertEquals(2, viewmodelJourneysList.size)
            // Check if the journeys are sorted by originUtcDateTime
            assertTrue(viewmodelJourneysList[0].originUtcDateTime < viewmodelJourneysList[1].originUtcDateTime)
        }

    @Test
    fun `GIVEN started journeys in cache WHEN updateTripsCache is called THEN journeys that have completed with threshold time must be cleared`() =
        runTest {
            // GIVEN Trip Response
            val tripResponse = TripResponse()
            viewModel.journeys.putAll(
                buildStartedJourneysList(
                    numberOfStartedJourneys = 3,
                    distortSortOrder = true,
                    completedJourneyCount = 2,
                )
            )
            tripResponse.journeys?.forEachIndexed { index, item ->
                println("tripResponse Journey #$index: ${item.legs?.get(0)?.origin?.arrivalTimeEstimated?.formatTo12HourTime()}")
            }

            // WHEN
            viewModel.updateTripsCache(tripResponse)

            // THEN
            val viewmodelJourneysList = viewModel.journeys.values.toList()
            // Only 1 started journey should be displayed as other two are completed with threshold time.
            assertEquals(1, viewmodelJourneysList.size)
        }

    // endregion

    /**
     * Builds a list of started journeys, i.e. journeys that have origin time in past.
     *
     * @param numberOfStartedJourneys The number of started journeys to create.
     * Have origin utc date time in past.
     *
     * @param distortSortOrder If true, the order of the journeys will be shuffled,means time will no longer be in ascending or descending.
     *
     * @param completedJourneyCount The number of journeys that have destinationUtcDateTime in the past.
     *  Also factoring in the threshold time for journey completion i.e [JOURNEY_ENDED_CACHE_THRESHOLD_TIME].
     * @return A map of journey IDs to JourneyCardInfo objects.
     */
    private fun buildStartedJourneysList(
        numberOfStartedJourneys: Int,
        distortSortOrder: Boolean = false,
        completedJourneyCount: Int = 0,
    ): Map<String, TimeTableState.JourneyCardInfo> {
        val startedJourneys = mutableMapOf<String, TimeTableState.JourneyCardInfo>()
        val now = Clock.System.now()

        for (i in 1..numberOfStartedJourneys) {
            // Calculate the origin time for each journey, decreasing by 5 minutes for each subsequent journey
            val originTime = now.minus(5.minutes * i)
            val destinationTime = if (i <= completedJourneyCount) {
                now.minus(JOURNEY_ENDED_CACHE_THRESHOLD_TIME + 1.minutes)
            } else {
                now.plus(10.minutes) // Journey Completes at a Future time
            }

            startedJourneys["journey$i"] = TimeTableState.JourneyCardInfo(
                originUtcDateTime = originTime.toString(),
                destinationUtcDateTime = destinationTime.toString(),
                timeText = "1",
                platformText = "1",
                platformNumber = "1",
                originTime = "",
                destinationTime = "",
                travelTime = "",
                totalWalkTime = "",
                transportModeLines = persistentListOf(),
                legs = persistentListOf(
                    TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                        transportModeLine = TransportModeLine(
                            transportMode = TransportMode.Train(),
                            lineName = "T1",
                        ),
                        displayText = "A via B",
                        totalDuration = "1 hour",
                        stops = persistentListOf(
                            Stop(name = "", time = "", isWheelchairAccessible = true),
                        ),
                        tripId = "id_$i",
                    )
                ),
                totalUniqueServiceAlerts = 1,
            )
        }

        // If distortSortOrder is true, shuffle the list of journeys before returning
        return if (distortSortOrder) {
            startedJourneys.toList().shuffled().toMap()
        } else startedJourneys
    }

    // region Test for saveTrip

    @Test
    fun `GIVEN trip info WHEN SaveTripButtonClicked is triggered THEN trip should be saved or deleted`() =
        runTest {
            // GIVEN
            val trip = Trip(
                fromStopId = "stop1",
                fromStopName = "Stop 1",
                toStopId = "stop2",
                toStopName = "Stop 2"
            )
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.uiState.test {

                awaitItem().run {
                    assertFalse(isTripSaved)
                }

                // GIVEN
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))

                // WHEN
                viewModel.onEvent(TimeTableUiEvent.SaveTripButtonClicked)

                // THEN
                advanceUntilIdle()
                assertTrue(analytics.isEventTracked("save_trip_click"))
                analytics.clear()
                skipItems(1)
                awaitItem().run {
                    assertTrue(isTripSaved)
                }

                // WHEN
                viewModel.onEvent(TimeTableUiEvent.SaveTripButtonClicked)

                // THEN
                awaitItem().run {
                    assertFalse(isTripSaved)
                }
                assertTrue(analytics.isEventTracked("save_trip_click"))
                analytics.clear()

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region Test for reverse trip
    @Test
    fun `GIVEN trip info WHEN ReverseTripButtonClicked is triggered THEN trip should be reversed`() =
        runTest {
            // GIVEN
            val initialTrip = Trip(
                fromStopId = "stop1",
                fromStopName = "Stop 1",
                toStopId = "stop2",
                toStopName = "Stop 2"
            )

            viewModel.uiState.test {
                awaitItem().run {
                    assertNull(trip) // trip is null initially
                }

                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(initialTrip))

                awaitItem().run {
                    assertNotNull(trip)
                    assertEquals("stop1", trip?.fromStopId)
                    assertEquals("Stop 1", trip?.fromStopName)
                    assertEquals("stop2", trip?.toStopId)
                    assertEquals("Stop 2", trip?.toStopName)
                }

                // WHEN ReverseTripButtonClicked is triggered
                viewModel.onEvent(TimeTableUiEvent.ReverseTripButtonClicked)

                // THEN trip should be reversed
                awaitItem().run {
                    assertNotNull(trip)
                    assertEquals("stop2", trip?.fromStopId)
                    assertEquals("Stop 2", trip?.fromStopName)
                    assertEquals("stop1", trip?.toStopId)
                    assertEquals("Stop 1", trip?.toStopName)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region Test for Retry Button Clicked

    @Test
    fun `GIVEN trip info WHEN RetryButtonClicked is triggered THEN trip should be reloaded`() =
        runTest {
            // GIVEN
            val tripInfo = Trip(
                fromStopId = "stop1",
                fromStopName = "Stop 1",
                toStopId = "stop2",
                toStopName = "Stop 2"
            )

            viewModel.uiState.test {
                skipItems(1) // initial state

                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(tripInfo))
                // WHEN RetryButtonClicked is triggered
                viewModel.onEvent(TimeTableUiEvent.RetryButtonClicked)

                awaitItem().run {
                    assertTrue(isLoading)
                    assertEquals(tripInfo, trip)
                    assertTrue(journeyList.isEmpty())
                }

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for JourneyCardClicked

    @Test
    fun `GIVEN a trip with journey list WHEN JourneyCardClicked is triggered THEN analytics event for collapse or expand is triggered`() =
        runTest {
            // GIVEN
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2"
            )
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            // THEN journey details should be loaded
            viewModel.uiState.test {

                skipItems(1)

                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip()
                skipItems(4) // silent loading toggle

                // WHEN JourneyCardClicked is triggered
                viewModel.onEvent(TimeTableUiEvent.JourneyCardClicked("TransportationId0null"))
                // THEN
                assertTrue(analytics.isEventTracked("journey_card_expand"))
                fakeAnalytics.clear()

                // WHEN JourneyCardClicked is triggered again
                viewModel.onEvent(TimeTableUiEvent.JourneyCardClicked("TransportationId0null"))
                // THEN
                assertTrue(analytics.isEventTracked("journey_card_collapse"))
                fakeAnalytics.clear()

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for DateTimeSelectionChanged

    @Test
    fun `GIVEN different dateTimeSelectionItem WHEN onDateTimeSelectionChanged is called THEN uiState is updated and analytics event is tracked`() =
        runTest {
            // GIVEN
            val dateTimeSelectionItem = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 12,
                minute = 0,
            )
            tripPlanningService.isSuccess = true
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(
                TimeTableUiEvent.DateTimeSelectionChanged(
                    dateTimeSelectionItem = dateTimeSelectionItem, // different because by default is null.
                ),
            )
            assertTrue(analytics.isEventTracked("date_time_select"))
        }

    @Test
    fun `GIVEN same dateTimeSelectionItem WHEN onDateTimeSelectionUnchanged is called THEN no action is taken`() =
        runTest {
            // GIVEN
            val dateTimeSelectionItem = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 12,
                minute = 0,
            )
            tripPlanningService.isSuccess = true
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            // WHEN
            viewModel.onEvent(
                TimeTableUiEvent.DateTimeSelectionChanged(
                    dateTimeSelectionItem = dateTimeSelectionItem, // different because by default is null.
                ),
            )
            // THEN
            assertTrue(analytics.isEventTracked("date_time_select"))
            analytics.clear()

            // WHEN same dateTimeSelectionItem is called, no action should be taken.
            viewModel.onEvent(
                TimeTableUiEvent.DateTimeSelectionChanged(
                    dateTimeSelectionItem = dateTimeSelectionItem,
                ),
            )
            // THEN
            assertFalse(analytics.isEventTracked("date_time_select"))
        }

    // endregion

    // region Test for Analytics
    @Test
    fun `GIVEN trip info WHEN AnalyticsDateTimeSelectorClicked is triggered THEN analytics event should be tracked`() =
        runTest {
            // GIVEN
            val trip = Trip(
                fromStopId = "stop1",
                fromStopName = "Stop 1",
                toStopId = "stop2",
                toStopName = "Stop 2"
            )
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            // WHEN AnalyticsDateTimeSelectorClicked is triggered
            viewModel.onEvent(TimeTableUiEvent.AnalyticsDateTimeSelectorClicked)

            // THEN analytics event should be tracked
            assertTrue(analytics.isEventTracked("plan_trip_click"))
        }

    @Test
    fun `GIVEN expanded state WHEN JourneyLegClicked is triggered THEN analytics event should be tracked`() =
        runTest {
            // GIVEN
            val expanded = true
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            // WHEN JourneyLegClicked is triggered
            viewModel.onEvent(TimeTableUiEvent.JourneyLegClicked(expanded))

            // THEN analytics event should be tracked
            assertTrue(analytics.isEventTracked("journey_leg_click"))
        }

    // endregion

    // region Test for ModeSelectionChanged
    @Test
    fun `GIVEN unselectedModes WHEN ModeSelectionChanged is called THEN UI state and analytics are updated`() = runTest {
        val initialUnselectedModes = setOf(1, 2)
        viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(initialUnselectedModes))
        advanceUntilIdle()
        assertEquals(initialUnselectedModes, viewModel.uiState.value.unselectedModes)
        assertTrue((fakeAnalytics as FakeAnalytics).isEventTracked("mode_selection_done"))
    }
    // endregion
}
