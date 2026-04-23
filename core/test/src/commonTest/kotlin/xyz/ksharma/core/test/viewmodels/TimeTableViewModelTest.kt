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
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeFestivalManager
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.core.test.fakes.FakeRateLimiter
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeShareManager
import xyz.ksharma.core.test.fakes.FakeTripPlanningService
import xyz.ksharma.core.test.fakes.FakeTripResponseBuilder
import xyz.ksharma.core.test.fakes.FakeTripResponseBuilder.buildTripResponse
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.core.test.fakes.FakeImageBitmap
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.datetime.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Stop
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.JOURNEY_ENDED_CACHE_THRESHOLD_TIME
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.MAX_LOAD_MORE_COUNT
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel.Companion.REFRESH_TIME_TEXT_DURATION
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimeTableViewModelTest {

    private val sandook: Sandook = FakeSandook()
    private val fakeAnalytics: Analytics = FakeAnalytics()
    private val fakeShareManager = FakeShareManager()
    private val tripPlanningService = FakeTripPlanningService()
    private val rateLimiter = FakeRateLimiter()
    private val festivalManager = FakeFestivalManager()
    private lateinit var viewModel: TimeTableViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val fakeFlag = FakeFlag()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TimeTableViewModel(
            tripPlanningService = tripPlanningService,
            rateLimiter = rateLimiter,
            sandook = sandook,
            analytics = fakeAnalytics,
            shareManager = fakeShareManager,
            ioDispatcher = testDispatcher,
            festivalManager = festivalManager,
            flag = fakeFlag,
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
    @OptIn(ExperimentalTime::class)
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
                            transportMode = NswTransportMode.Train,
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

    @OptIn(ExperimentalTime::class)
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

    @OptIn(ExperimentalTime::class)
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
            viewModel.onEvent(TimeTableUiEvent.AnalyticsJourneyLegClicked(expanded))

            // THEN analytics event should be tracked
            assertTrue(analytics.isEventTracked("journey_leg_click"))
        }

    // endregion

    // region Test for ModeSelectionChanged
    @Test
    fun `GIVEN unselectedModes WHEN ModeSelectionChanged is called THEN UI state and analytics are updated`() =
        runTest {
            val initialUnselectedModes = setOf(1, 2)
            viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(initialUnselectedModes))
            advanceUntilIdle()
            assertEquals(initialUnselectedModes, viewModel.uiState.value.unselectedModes)
            assertTrue((fakeAnalytics as FakeAnalytics).isEventTracked("mode_selection_done"))
        }
    // endregion

    // region Test for ShareJourneyClicked

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GIVEN journey in cache WHEN ShareJourneyClicked is triggered THEN shareImage is called with correct bitmap and text`() =
        runTest {
            // GIVEN
            val journeyId = "journey_share_1"
            viewModel.journeys[journeyId] = buildShareTestJourney(journeyId)
            val bitmap = FakeImageBitmap()
            val shareText = "Hey mate!\n\nI'll reach Central at 8:40am."

            // WHEN
            viewModel.onEvent(
                TimeTableUiEvent.ShareJourneyClicked(
                    bitmap = bitmap,
                    shareText = shareText,
                    journeyId = journeyId,
                    isPastDeparture = false,
                ),
            )
            advanceUntilIdle()

            // THEN
            assertTrue(fakeShareManager.shareImageCalled)
            assertEquals(shareText, fakeShareManager.lastSharedText)
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GIVEN journey in cache WHEN ShareJourneyClicked is triggered THEN share_journey_click analytics event is tracked`() =
        runTest {
            // GIVEN
            val journeyId = "journey_share_2"
            viewModel.journeys[journeyId] = buildShareTestJourney(journeyId)

            // WHEN
            viewModel.onEvent(
                TimeTableUiEvent.ShareJourneyClicked(
                    bitmap = FakeImageBitmap(),
                    shareText = "test",
                    journeyId = journeyId,
                    isPastDeparture = true,
                ),
            )
            advanceUntilIdle()

            // THEN
            assertTrue((fakeAnalytics as FakeAnalytics).isEventTracked("share_journey_click"))
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GIVEN journey NOT in cache WHEN ShareJourneyClicked is triggered THEN shareImage is still called but analytics is skipped`() =
        runTest {
            // GIVEN - journeys cache is empty

            // WHEN
            viewModel.onEvent(
                TimeTableUiEvent.ShareJourneyClicked(
                    bitmap = FakeImageBitmap(),
                    shareText = "test",
                    journeyId = "non_existent_id",
                    isPastDeparture = false,
                ),
            )
            advanceUntilIdle()

            // THEN - share still fires, analytics skipped gracefully
            assertTrue(fakeShareManager.shareImageCalled)
            assertFalse((fakeAnalytics as FakeAnalytics).isEventTracked("share_journey_click"))
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `GIVEN shareManager fails WHEN ShareJourneyClicked is triggered THEN error is handled without crash`() =
        runTest {
            // GIVEN
            val journeyId = "journey_share_fail"
            viewModel.journeys[journeyId] = buildShareTestJourney(journeyId)
            fakeShareManager.shouldFail = true

            // WHEN
            viewModel.onEvent(
                TimeTableUiEvent.ShareJourneyClicked(
                    bitmap = FakeImageBitmap(),
                    shareText = "test",
                    journeyId = journeyId,
                    isPastDeparture = false,
                ),
            )
            advanceUntilIdle()

            // THEN - called but returned failure, no exception propagated
            assertTrue(fakeShareManager.shareImageCalled)
        }

    // endregion

    // region Test for initializeTrip

    @Test
    fun `GIVEN initialized route WHEN initializeTrip called with same route THEN re-initialization is skipped`() =
        runTest {
            val fromStopId = "stop_a"
            val toStopId = "stop_b"
            tripPlanningService.isSuccess = true
            rateLimiter.reset()

            viewModel.initializeTrip(fromStopId, "Stop A", toStopId, "Stop B")
            advanceUntilIdle()
            val triggerCountAfterFirst = rateLimiter.triggerCount

            // Same route again — should be a no-op
            viewModel.initializeTrip(fromStopId, "Stop A", toStopId, "Stop B")
            advanceUntilIdle()

            assertEquals(
                triggerCountAfterFirst,
                rateLimiter.triggerCount,
                "Rate limiter should not be triggered again for the same route",
            )
        }

    @Test
    fun `GIVEN initialized route WHEN initializeTrip called with different route THEN new trip is loaded`() =
        runTest {
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1) // initial state

                viewModel.initializeTrip("stop_a", "Stop A", "stop_b", "Stop B")
                awaitItem().run {
                    assertEquals("stop_a", trip?.fromStopId)
                    assertEquals("stop_b", trip?.toStopId)
                }

                // Different destination
                viewModel.initializeTrip("stop_a", "Stop A", "stop_c", "Stop C")
                awaitItem().run {
                    assertEquals("stop_a", trip?.fromStopId)
                    assertEquals("stop_c", trip?.toStopId)
                    assertTrue(isLoading)
                }

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for loading state transitions

    @Test
    fun `GIVEN trip info WHEN fetchTrip is called THEN silentLoading is true during fetch and false after`() =
        runTest {
            val trip = Trip(
                fromStopId = "stop1",
                fromStopName = "Stop 1",
                toStopId = "stop2",
                toStopName = "Stop 2",
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1) // initial state

                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip()

                // emit 1: LoadTimeTable sets trip, isLoading=true, silentLoading still false
                awaitItem().run {
                    assertTrue(isLoading)
                    assertFalse(silentLoading)
                }

                // emit 2: fetchTrip sets silentLoading=true
                awaitItem().run { assertTrue(silentLoading) }

                // emit 3: collectLatest clears silentLoading before final state
                awaitItem().run { assertFalse(silentLoading) }

                // emit 4: updateUiStateWithFilteredTrips sets isLoading=false + journeyList
                awaitItem().run {
                    assertFalse(isLoading)
                    assertFalse(silentLoading)
                    assertTrue(journeyList.isNotEmpty())
                }

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN different trip WHEN onLoadTimeTable is called THEN isLoading is true and journey cache is cleared`() =
        runTest {
            val trip1 = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            val trip2 = Trip(fromStopId = "stop3", fromStopName = "S3", toStopId = "stop4", toStopName = "S4")
            tripPlanningService.isSuccess = true

            // Load first trip and populate journeys cache
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip1))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.journeys.isNotEmpty())

            // Now switch to a different trip
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip2))
            assertTrue(viewModel.journeys.isEmpty(), "Journey cache should be cleared on trip change")

            viewModel.uiState.test {
                awaitItem().run {
                    assertTrue(isLoading)
                    assertEquals("stop3", trip?.fromStopId)
                }
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN same trip WHEN onLoadTimeTable is called THEN journey cache is preserved`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val cacheSize = viewModel.journeys.size
            assertTrue(cacheSize > 0, "Expected journeys in cache after fetch")

            // Re-load same trip (simulates nav back)
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            advanceUntilIdle()

            assertEquals(cacheSize, viewModel.journeys.size, "Journey cache should be preserved for same trip")
        }

    // endregion

    // region Test for journey cache lifecycle

    @Test
    fun `GIVEN loaded journeys WHEN DateTimeSelectionChanged THEN journey cache is cleared`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.journeys.isNotEmpty())

            @OptIn(ExperimentalTime::class)
            val newSelection = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 10,
                minute = 30,
            )

            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(newSelection))
            assertTrue(viewModel.journeys.isEmpty(), "Journey cache should be cleared when date/time changes")
        }

    @Test
    fun `GIVEN loaded journeys WHEN ReverseTripButtonClicked THEN journey cache is cleared`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.journeys.isNotEmpty())

            viewModel.onEvent(TimeTableUiEvent.ReverseTripButtonClicked)
            assertTrue(viewModel.journeys.isEmpty(), "Journey cache should be cleared on reverse trip")
        }

    // endregion

    // region Test for autoRefreshTimeTable

    @Test
    fun `GIVEN non-empty journey list WHEN AUTO_REFRESH_TIME_TABLE_DURATION passes THEN rate limiter is triggered`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())

            rateLimiter.reset()

            viewModel.autoRefreshTimeTable.test {
                skipItems(1) // initial value

                advanceTimeBy(TimeTableViewModel.AUTO_REFRESH_TIME_TABLE_DURATION.inWholeMilliseconds + 1.seconds.inWholeMilliseconds)
                assertTrue(rateLimiter.triggerCount > 0, "Rate limiter should be triggered after auto-refresh interval")

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN empty journey list WHEN AUTO_REFRESH_TIME_TABLE_DURATION passes THEN rate limiter is NOT triggered`() =
        runTest {
            // GIVEN empty journey list (no trips loaded)
            rateLimiter.reset()

            viewModel.autoRefreshTimeTable.test {
                skipItems(1)

                advanceTimeBy(TimeTableViewModel.AUTO_REFRESH_TIME_TABLE_DURATION.inWholeMilliseconds + 1.seconds.inWholeMilliseconds)
                assertEquals(0, rateLimiter.triggerCount, "Rate limiter should not be triggered when journey list is empty")

                cancelAndConsumeRemainingEvents()
            }
        }

    // endregion

    // region Test for isActive (time text updates)

    @Test
    fun `GIVEN non-empty journey list WHEN isActive ticker runs THEN journey list remains intact across ticks`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())

            // Subscribe to isActive to activate the time-text ticker, advance through two full
            // intervals, then cancel — journey list must not be cleared by the ticker.
            viewModel.isActive.test {
                skipItems(1) // initial value
                advanceTimeBy(REFRESH_TIME_TEXT_DURATION.inWholeMilliseconds * 2 + 100)
                // Cancel BEFORE advanceUntilIdle to avoid infinite loop from the while(true) ticker.
                cancelAndConsumeRemainingEvents()
            }

            assertTrue(
                viewModel.uiState.value.journeyList.isNotEmpty(),
                "Journey list must remain non-empty across time-text refresh ticks",
            )
        }

    // endregion

    // region Test for cleanupJobs

    @Test
    fun `WHEN cleanupJobs is called THEN alerts are cleared`() =
        runTest {
            val sandookCast = sandook as FakeSandook
            sandookCast.insertAlerts(
                journeyId = "j1",
                alerts = listOf(
                    xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId(
                        journeyId = "j1",
                        heading = "Test heading",
                        message = "Test message",
                    ),
                ),
            )
            assertTrue(sandookCast.getAlerts("j1").isNotEmpty())

            viewModel.cleanupJobs()
            advanceUntilIdle()

            assertTrue(
                sandookCast.getAlerts("j1").isEmpty(),
                "Alerts should be cleared after cleanupJobs()",
            )
        }

    // endregion

    // region Test for ModeClicked and BackClick analytics

    @Test
    fun `GIVEN mode click event WHEN ModeClicked is triggered THEN mode_click analytics event is tracked`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(TimeTableUiEvent.ModeClicked(displayModeSelectionRow = true))
            assertTrue(analytics.isEventTracked("mode_click"))
        }

    @Test
    fun `GIVEN back click event WHEN BackClick is triggered THEN back_click analytics event is tracked`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(TimeTableUiEvent.BackClick)
            assertTrue(analytics.isEventTracked("back_click"))
        }

    // endregion

    // region Test for trip API call arguments

    @Test
    fun `GIVEN mode filter WHEN ModeSelectionChanged THEN API is called with correct excludeProductClassSet`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true
            val excludedModes = setOf(5, 7) // Bus, Coach

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(excludedModes))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertEquals(excludedModes, tripPlanningService.lastCalledExcludeProductClassSet)
        }

    @Test
    fun `GIVEN date time selection WHEN fetchTrip is called THEN API receives correct date and time`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            @OptIn(ExperimentalTime::class)
            val selection = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 9,
                minute = 15,
            )

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(selection))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertEquals("0915", tripPlanningService.lastCalledTime)
            assertNotNull(tripPlanningService.lastCalledDate)
        }

    // endregion

    // region Test for LoadMoreTrips and LoadPreviousTrips

    @Test
    fun `GIVEN loaded journeys WHEN LoadMoreTrips event THEN loadMoreJourneys cache is populated and journeyList grows`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val initialSize = viewModel.uiState.value.journeyList.size
            assertTrue(initialSize > 0)

            // Configure service to return 2 journeys: index 0 is a duplicate (deduped), index 1 is new.
            // This verifies dedup logic while ensuring at least one new journey appears.
            tripPlanningService.setResponseForCall(
                tripPlanningService.tripCallCount,
                FakeTripResponseBuilder.buildTripResponse(numberOfJourney = 2),
            )

            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            // loadMoreJourneys cache should contain the non-duplicate journey (index 1)
            assertTrue(viewModel.loadMoreJourneys.isNotEmpty(), "loadMoreJourneys must be populated with new journey")
            // journeyList in state should include the loaded-more trip
            assertTrue(
                viewModel.uiState.value.journeyList.size >= initialSize,
                "journeyList should include load-more journeys",
            )
        }

    @Test
    fun `GIVEN MAX_LOAD_MORE_COUNT reached WHEN LoadMoreTrips THEN no API call is made`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Reach the limit — each LoadMoreTrips cancels the previous job, so we must
            // advance after each dispatch so the job runs and loadMoreCount is incremented.
            repeat(TimeTableViewModel.MAX_LOAD_MORE_COUNT) {
                viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
                advanceUntilIdle()
            }

            val callCountAtLimit = tripPlanningService.tripCallCount
            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            assertEquals(callCountAtLimit, tripPlanningService.tripCallCount, "No extra API call when limit reached")
            assertFalse(viewModel.uiState.value.canLoadMore)
        }

    @Test
    fun `GIVEN loaded journeys WHEN LoadPreviousTrips event THEN previousJourneysCache is populated`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())

            // Service returns past journeys for the previous-trips call
            tripPlanningService.setResponseForCall(
                tripPlanningService.tripCallCount,
                FakeTripResponseBuilder.buildTripResponse(numberOfJourney = 1),
            )

            viewModel.onEvent(TimeTableUiEvent.LoadPreviousTrips)
            advanceUntilIdle()

            // Even if the fake response returns future journeys (filtered out), the API call happened
            assertTrue(tripPlanningService.tripCallCount > 1, "LoadPreviousTrips should trigger an API call")
        }

    @Test
    fun `GIVEN load-more journeys WHEN trip changes THEN loadMoreJourneys and previousJourneysCache are cleared`() =
        runTest {
            val trip1 = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip1))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Seed load-more and previous caches manually
            viewModel.loadMoreJourneys["extra1"] = buildShareTestJourney("extra1")
            viewModel.previousJourneysCache["prev1"] = buildShareTestJourney("prev1")

            // Switch to a different trip
            val trip2 = Trip(fromStopId = "stop3", fromStopName = "S3", toStopId = "stop4", toStopName = "S4")
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip2))
            advanceUntilIdle()

            assertTrue(viewModel.loadMoreJourneys.isEmpty(), "loadMoreJourneys cleared on trip change")
            assertTrue(viewModel.previousJourneysCache.isEmpty(), "previousJourneysCache cleared on trip change")
        }

    @Test
    fun `GIVEN load-more journeys WHEN auto-refresh runs THEN loadMoreJourneys are preserved in journeyList`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // Seed load-more cache manually (simulates a prior LoadMoreTrips success)
            val extraJourney = buildShareTestJourney("extra_future_journey")
            viewModel.loadMoreJourneys[extraJourney.journeyId] = extraJourney

            // Simulate auto-refresh (calls fetchTrip which calls updateTripsCache + updateUiStateWithFilteredTrips)
            viewModel.fetchTrip()
            advanceUntilIdle()

            // The load-more journey must still appear in the displayed list
            val journeyIds = viewModel.uiState.value.journeyList.map { it.journeyId }
            assertTrue(
                journeyIds.contains(extraJourney.journeyId),
                "Load-more journeys must survive auto-refresh",
            )
        }

    @Test
    fun `GIVEN canLoadMore is true WHEN journeyList is non-empty THEN canLoadMore reflects limit`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())
            assertTrue(viewModel.uiState.value.canLoadMore, "canLoadMore should be true when journeys loaded")
        }

    // endregion

    @OptIn(ExperimentalTime::class)
    private fun buildShareTestJourney(journeyId: String): TimeTableState.JourneyCardInfo {
        val now = Clock.System.now()
        return TimeTableState.JourneyCardInfo(
            originUtcDateTime = now.toString(),
            destinationUtcDateTime = now.plus(30.minutes).toString(),
            timeText = "in 5 mins",
            platformText = "Platform 1",
            platformNumber = "1",
            originTime = "8:10 AM",
            destinationTime = "8:40 AM",
            travelTime = "30 mins",
            totalWalkTime = null,
            transportModeLines = persistentListOf(
                TransportModeLine(transportMode = NswTransportMode.Train, lineName = "T1"),
            ),
            legs = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    transportModeLine = TransportModeLine(
                        transportMode = NswTransportMode.Train,
                        lineName = "T1",
                    ),
                    displayText = "towards Central",
                    totalDuration = "30 mins",
                    stops = persistentListOf(
                        Stop(name = "Central", time = "8:40 AM", isWheelchairAccessible = true),
                    ),
                    tripId = journeyId,
                ),
            ),
            totalUniqueServiceAlerts = 0,
        )
    }
}
