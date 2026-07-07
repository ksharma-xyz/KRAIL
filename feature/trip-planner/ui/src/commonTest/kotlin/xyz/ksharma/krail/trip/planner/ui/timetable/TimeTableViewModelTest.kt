package xyz.ksharma.krail.trip.planner.ui.timetable

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
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeRateLimiter
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.fakes.FakeShareManager
import xyz.ksharma.krail.core.testing.fakes.FakeTripPlanningService
import xyz.ksharma.krail.core.testing.fakes.FakeTripResponseBuilder
import xyz.ksharma.krail.core.testing.fakes.FakeTripResponseBuilder.buildTripResponse
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.testing.fakes.FakeImageBitmap
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.datetime.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
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
import kotlin.test.assertIs
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
    private val fakePreferences = FakeSandookPreferences()
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
        TimeTableViewModel.resetSavePromptSessionFlagForTest()
        viewModel = TimeTableViewModel(
            tripPlanningService = tripPlanningService,
            rateLimiter = rateLimiter,
            sandook = sandook,
            preferences = fakePreferences,
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

    @Test
    fun `GIVEN loaded journeys WHEN a later refresh fails THEN error screen is not shown and journeys are preserved`() =
        runTest {
            // GIVEN a trip whose first load succeeds and populates the list
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2"
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip()

                // Drain until journeys are loaded.
                var loaded = awaitItem()
                while (loaded.journeyList.isEmpty()) loaded = awaitItem()
                assertFalse(loaded.isError)
                assertTrue(loaded.journeyList.isNotEmpty())

                // WHEN a later (silent/auto) refresh fails
                tripPlanningService.isSuccess = false
                viewModel.fetchTrip()

                // THEN no emission flips to the error screen while data is present,
                // and the loaded journeys survive the failed refresh.
                var settled = awaitItem()
                while (settled.silentLoading) {
                    assertFalse(settled.isError)
                    settled = awaitItem()
                }
                settled.run {
                    assertFalse(isError)
                    assertTrue(journeyList.isNotEmpty())
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
    fun `GIVEN a trip with journey list WHEN JourneyCardClicked is triggered THEN toggle event fires with expanded true then false`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip()
                skipItems(4)

                // First click — should expand
                viewModel.onEvent(TimeTableUiEvent.JourneyCardClicked("TransportationId0null"))
                assertTrue(analytics.isEventTracked("journey_card_toggle"))
                val expandEvent = analytics.getTrackedEvent("journey_card_toggle")
                assertIs<AnalyticsEvent.JourneyCardToggleEvent>(expandEvent)
                assertTrue(expandEvent.expanded)
                fakeAnalytics.clear()

                // Second click — should collapse
                viewModel.onEvent(TimeTableUiEvent.JourneyCardClicked("TransportationId0null"))
                assertTrue(analytics.isEventTracked("journey_card_toggle"))
                val collapseEvent = analytics.getTrackedEvent("journey_card_toggle")
                assertIs<AnalyticsEvent.JourneyCardToggleEvent>(collapseEvent)
                assertFalse(collapseEvent.expanded)
                fakeAnalytics.clear()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN journey with one Train leg WHEN JourneyCardClicked THEN toggle event carries correct transportModes and legCount`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                viewModel.fetchTrip()
                skipItems(4)

                viewModel.onEvent(TimeTableUiEvent.JourneyCardClicked("TransportationId0null"))

                val event = analytics.getTrackedEvent("journey_card_toggle")
                assertIs<AnalyticsEvent.JourneyCardToggleEvent>(event)
                // Fake builder produces one Train leg (productClass = 1)
                assertEquals("1", event.transportModes)
                assertEquals(1, event.legCount)

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GIVEN AnalyticsJourneyLegClicked WHEN sent to ViewModel THEN JourneyLegClickEvent is tracked with all properties`() =
        runTest {
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(
                TimeTableUiEvent.AnalyticsJourneyLegClicked(
                    expanded = true,
                    transportMode = "Train",
                    lineName = "T1",
                ),
            )
            advanceUntilIdle()

            assertTrue(analytics.isEventTracked("journey_leg_click"))
            val event = analytics.getTrackedEvent("journey_leg_click")
            assertIs<AnalyticsEvent.JourneyLegClickEvent>(event)
            assertTrue(event.expanded)
            assertEquals("Train", event.transportMode)
            assertEquals("T1", event.lineName)
        }

    @Test
    fun `GIVEN AnalyticsJourneyLegClicked with expanded false WHEN sent to ViewModel THEN JourneyLegClickEvent reflects collapse`() =
        runTest {
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(
                TimeTableUiEvent.AnalyticsJourneyLegClicked(
                    expanded = false,
                    transportMode = "Bus",
                    lineName = "700",
                ),
            )
            advanceUntilIdle()

            val event = analytics.getTrackedEvent("journey_leg_click")
            assertIs<AnalyticsEvent.JourneyLegClickEvent>(event)
            assertFalse(event.expanded)
            assertEquals("Bus", event.transportMode)
            assertEquals("700", event.lineName)
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
            val analytics: FakeAnalytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(
                TimeTableUiEvent.AnalyticsJourneyLegClicked(
                    expanded = true,
                    transportMode = "Train",
                    lineName = "T1",
                ),
            )

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

    @Test
    fun `GIVEN train-only journeys WHEN Train is de-selected THEN journeys are filtered out client-side and emptyDueToModeFilter is true`() =
        runTest {
            // GIVEN a loaded trip — the fake builder returns train-only journeys (productClass 1).
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2"
            )
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())
            assertFalse(viewModel.uiState.value.emptyDueToModeFilter)

            // WHEN Train (productClass 1) is de-selected and the trip re-fetches. The NSW API
            // still returns train journeys, so the client-side filter must drop them.
            viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(setOf(1)))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // THEN all train journeys are filtered out and the mode-specific empty hint is set.
            viewModel.uiState.value.run {
                assertTrue(journeyList.isEmpty())
                assertTrue(emptyDueToModeFilter)
                assertFalse(isError)
            }
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

    @Test
    fun `GIVEN a selected Arrive-by time WHEN Retry is clicked THEN the re-fetch keeps that time`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")

            @OptIn(ExperimentalTime::class)
            val selection = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.ARRIVE,
                hour = 9,
                minute = 15,
            )

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(selection))

            // First fetch fails -> error screen.
            tripPlanningService.isSuccess = false
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isError)

            // WHEN Retry is clicked
            tripPlanningService.isSuccess = true
            viewModel.onEvent(TimeTableUiEvent.RetryButtonClicked)
            advanceUntilIdle()

            // THEN the retried request keeps the selected Arrive-by time, not "now".
            assertEquals("0915", tripPlanningService.lastCalledTime)
            assertEquals(DepArr.ARR, tripPlanningService.lastCalledDepArr)
            assertNotNull(viewModel.dateTimeSelectionItem)
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
    fun `GIVEN loaded journeys WHEN LoadMoreTrips fires THEN load_more_click is tracked with trip pair and pre-increment count`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "S1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "S2",
            )
            tripPlanningService.isSuccess = true
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val visibleAtTap = viewModel.uiState.value.journeyList.size
            assertTrue(visibleAtTap > 0)
            analytics.clear()

            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            val tracked = analytics.getTrackedEvent("timetable_load_more_click")
            assertNotNull(tracked)
            val event = assertIs<AnalyticsEvent.TimeTableLoadMoreClickEvent>(tracked)
            assertEquals("FROM_STOP_ID_1", event.fromStopId)
            assertEquals("TO_STOP_ID_1", event.toStopId)
            // First tap, no successful pages yet → 0
            assertEquals(0, event.loadMoreCount)
            // Default time used → not custom
            assertFalse(event.isCustomDateTime)
            // Captured the visible list size at tap time, before this fetch's results merged in
            assertEquals(visibleAtTap, event.visibleJourneyCount)
        }

    @Test
    fun `GIVEN multiple successful LoadMoreTrips WHEN tapped again THEN loadMoreCount reflects pages already fetched`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // First two pages succeed, then we inspect the third tap's payload
            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()
            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()
            analytics.clear()

            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            val tracked = analytics.getTrackedEvent("timetable_load_more_click")
            assertNotNull(tracked)
            val event = assertIs<AnalyticsEvent.TimeTableLoadMoreClickEvent>(tracked)
            // Two pages already fetched before this tap
            assertEquals(2, event.loadMoreCount)
        }

    @Test
    fun `GIVEN loaded journeys WHEN LoadPreviousTrips fires THEN load_previous_click is tracked with trip pair and visible count`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "S1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "S2",
            )
            tripPlanningService.isSuccess = true
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()
            val visibleAtTap = viewModel.uiState.value.journeyList.size
            assertTrue(visibleAtTap > 0)
            analytics.clear()

            viewModel.onEvent(TimeTableUiEvent.LoadPreviousTrips)
            advanceUntilIdle()

            val tracked = analytics.getTrackedEvent("timetable_load_previous_click")
            assertNotNull(tracked)
            val event = assertIs<AnalyticsEvent.TimeTableLoadPreviousClickEvent>(tracked)
            assertEquals("FROM_STOP_ID_1", event.fromStopId)
            assertEquals("TO_STOP_ID_1", event.toStopId)
            assertFalse(event.isCustomDateTime)
            assertEquals(visibleAtTap, event.visibleJourneyCount)
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

            // Seed load-more cache with a trip genuinely beyond the auto-refresh window.
            // Using now+60m ensures pruning does not remove it (latestFreshInstant ≈ now).
            val now = Clock.System.now()
            val extraJourney = buildShareTestJourney("extra_future_journey").copy(
                originUtcDateTime = now.plus(60.minutes).toString(),
                destinationUtcDateTime = now.plus(90.minutes).toString(),
            )
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

    // ── rawJourneyDataByJourneyId lifecycle (backs the journey map screen) ─────
    //
    // The map screen looks up coordinates via `viewModel.getRawJourneyById(id)`. That
    // map is fed by `rawJourneyDataByJourneyId`. Before the fix:
    //
    //   - onLoadMoreTrips / onLoadPreviousTrips discarded the raw-data half of
    //     `response.buildJourneyListWithRawData()` — tapping a load-more / previous
    //     journey on the map showed an empty map.
    //   - onModeSelectionChanged / resetPaginationCaches cleared everything *except*
    //     raw-data, so a stale load-more entry could leak into a re-fetched list.

    @Test
    fun `GIVEN initial load WHEN LoadMoreTrips THEN raw journey data is cached for new journeys`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            // 2-journey response so dedup leaves at least one new load-more journey
            // (the first overlaps the initial fetch; the second is fresh).
            tripPlanningService.setResponseForCall(
                tripPlanningService.tripCallCount,
                FakeTripResponseBuilder.buildTripResponse(numberOfJourney = 2),
            )

            viewModel.onEvent(TimeTableUiEvent.LoadMoreTrips)
            advanceUntilIdle()

            assertTrue(
                viewModel.loadMoreJourneys.isNotEmpty(),
                "Sanity: load-more cache must hold the new journey",
            )
            viewModel.loadMoreJourneys.keys.forEach { id ->
                assertNotNull(
                    viewModel.getRawJourneyById(id),
                    "Load-more journey '$id' must have raw data so the map can resolve coordinates",
                )
            }
        }

    @Test
    fun `GIVEN journeys cached WHEN ModeSelectionChanged THEN raw journey data is cleared`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val initialIds = viewModel.uiState.value.journeyList.map { it.journeyId }
            assertTrue(initialIds.isNotEmpty(), "Sanity: initial journeys must be loaded")
            initialIds.forEach { id ->
                assertNotNull(viewModel.getRawJourneyById(id), "Sanity: initial raw data populated")
            }

            // ModeSelectionChanged inline-resets the caches AND clears rawJourneyDataByJourneyId,
            // then triggers a re-fetch. Old journey IDs must no longer resolve.
            viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(unselectedModes = setOf(NswTransportMode.Bus.productClass)))
            advanceUntilIdle()

            initialIds.forEach { id ->
                if (viewModel.uiState.value.journeyList.none { it.journeyId == id }) {
                    // Only assert clearance for IDs that did NOT survive the re-fetch.
                    // (If the re-fetch happens to produce the same ID, raw data is repopulated.)
                    assertNull(
                        viewModel.getRawJourneyById(id),
                        "Stale raw data for journey '$id' must not survive a mode-selection reset",
                    )
                }
            }
        }

    @Test
    fun `GIVEN journeys cached WHEN DateTimeSelectionChanged THEN raw journey data is cleared via resetPaginationCaches`() =
        runTest {
            val trip = Trip(fromStopId = "stop1", fromStopName = "S1", toStopId = "stop2", toStopName = "S2")
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            val initialIds = viewModel.uiState.value.journeyList.map { it.journeyId }
            assertTrue(initialIds.isNotEmpty(), "Sanity: initial journeys must be loaded")
            initialIds.forEach { id ->
                assertNotNull(viewModel.getRawJourneyById(id), "Sanity: initial raw data populated")
            }

            // DateTimeSelectionChanged → resetPaginationCaches() (which now also clears
            // rawJourneyDataByJourneyId per the fix) + journeys.clear() + re-fetch.
            val newSelection = DateTimeSelectionItem(
                date = Clock.System.now().toLocalDateTime(currentSystemDefault()).date,
                option = JourneyTimeOptions.LEAVE,
                hour = 12,
                minute = 0,
            )
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem = newSelection))
            advanceUntilIdle()

            initialIds.forEach { id ->
                if (viewModel.uiState.value.journeyList.none { it.journeyId == id }) {
                    assertNull(
                        viewModel.getRawJourneyById(id),
                        "Stale raw data for journey '$id' must not survive a date-time reset",
                    )
                }
            }
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

    // region OriginDestinationStopHeaderClicked — analytics for the gesture that
    // opens the leg-scoped stop search. The trip context (tripFromStopId / tripToStopId)
    // comes from the VM's `tripInfo` field, which is set by LoadTimeTable.

    @Test
    fun `GIVEN trip loaded WHEN origin header is clicked THEN stop header click event is tracked with isOrigin true`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.uiState.test {
                skipItems(1)
                // LoadTimeTable populates `tripInfo` so the click handler can
                // include the trip pair in the analytics payload.
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()
                analytics.clear()

                viewModel.onEvent(
                    TimeTableUiEvent.OriginDestinationStopHeaderClicked(
                        stopId = "FROM_STOP_ID_1",
                        stopName = "STOP_NAME_1",
                        isOrigin = true,
                    ),
                )
                advanceUntilIdle()

                val tracked = analytics.getTrackedEvent("timetable_stop_header_click")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.TimeTableStopHeaderClickEvent>(tracked)
                assertEquals("FROM_STOP_ID_1", event.stopId)
                assertEquals("STOP_NAME_1", event.stopName)
                assertTrue(event.isOrigin)
                // Payload anchors to the trip pair so the dashboard can join with
                // PlanTripClickEvent / LoadTimeTableClickEvent.
                assertEquals("FROM_STOP_ID_1", event.tripFromStopId)
                assertEquals("TO_STOP_ID_1", event.tripToStopId)
                // Post-A3 the tap opens the leg-scoped edit search — action lets the
                // dashboard split behaviour before/after the change.
                assertEquals(
                    AnalyticsEvent.TimeTableStopHeaderClickEvent.ACTION_EDIT_SEARCH,
                    event.action,
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN trip loaded WHEN destination header is clicked THEN stop header click event is tracked with isOrigin false`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()
                analytics.clear()

                viewModel.onEvent(
                    TimeTableUiEvent.OriginDestinationStopHeaderClicked(
                        stopId = "TO_STOP_ID_1",
                        stopName = "STOP_NAME_2",
                        isOrigin = false,
                    ),
                )
                advanceUntilIdle()

                val tracked = analytics.getTrackedEvent("timetable_stop_header_click")
                assertNotNull(tracked)
                val event = assertIs<AnalyticsEvent.TimeTableStopHeaderClickEvent>(tracked)
                assertEquals("TO_STOP_ID_1", event.stopId)
                assertEquals("STOP_NAME_2", event.stopName)
                assertFalse(event.isOrigin)
                assertEquals("FROM_STOP_ID_1", event.tripFromStopId)
                assertEquals("TO_STOP_ID_1", event.tripToStopId)
                assertEquals(
                    AnalyticsEvent.TimeTableStopHeaderClickEvent.ACTION_EDIT_SEARCH,
                    event.action,
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region DeparturesIconClicked — analytics for the explicit departures icon
    // that now owns the stop-details sheet (moved off the stop-name tap).

    @Test
    fun `GIVEN trip loaded WHEN departures icon is clicked THEN departures icon click event is tracked`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics

            viewModel.onEvent(
                TimeTableUiEvent.DeparturesIconClicked(
                    stopId = "FROM_STOP_ID_1",
                    stopName = "STOP_NAME_1",
                    isOrigin = true,
                ),
            )
            advanceUntilIdle()

            val tracked = analytics.getTrackedEvent("timetable_departures_icon_click")
            assertNotNull(tracked)
            val event = assertIs<AnalyticsEvent.TimeTableDeparturesIconClickEvent>(tracked)
            assertEquals("FROM_STOP_ID_1", event.stopId)
            assertEquals("STOP_NAME_1", event.stopName)
            assertTrue(event.isOrigin)
        }

    // endregion

    // region TripStopChanged — stop picked from the leg-scoped edit search reloads
    // the timetable in place with the changed leg, keeping the other leg untouched.

    @Test
    fun `GIVEN trip loaded WHEN origin stop is changed THEN timetable reloads with new origin and destination untouched`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()

                viewModel.onEvent(
                    TimeTableUiEvent.TripStopChanged(
                        stopId = "NEW_FROM_STOP_ID",
                        stopName = "NEW_FROM_STOP_NAME",
                        isOrigin = true,
                    ),
                )
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("NEW_FROM_STOP_ID", state.trip?.fromStopId)
                assertEquals("NEW_FROM_STOP_NAME", state.trip?.fromStopName)
                assertEquals("TO_STOP_ID_1", state.trip?.toStopId)
                assertEquals("STOP_NAME_2", state.trip?.toStopName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN trip loaded WHEN destination stop is changed THEN timetable reloads with new destination and origin untouched`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()

                viewModel.onEvent(
                    TimeTableUiEvent.TripStopChanged(
                        stopId = "NEW_TO_STOP_ID",
                        stopName = "NEW_TO_STOP_NAME",
                        isOrigin = false,
                    ),
                )
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("FROM_STOP_ID_1", state.trip?.fromStopId)
                assertEquals("STOP_NAME_1", state.trip?.fromStopName)
                assertEquals("NEW_TO_STOP_ID", state.trip?.toStopId)
                assertEquals("NEW_TO_STOP_NAME", state.trip?.toStopName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN trip loaded WHEN same stop is picked as replacement THEN nothing reloads`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.onEvent(
                    TimeTableUiEvent.TripStopChanged(
                        stopId = "FROM_STOP_ID_1",
                        stopName = "STOP_NAME_1",
                        isOrigin = true,
                    ),
                )
                advanceUntilIdle()

                // Same stop picked — handler bails before touching state, so no
                // new UI state is emitted.
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN stop changed WHEN LoadTimeTable fires again for the original pair THEN original pair reloads fresh`() =
        runTest {
            // Regression: an in-place stop edit must update the VM's trip-identity
            // tracking. Re-opening the original saved trip (same nav key) used to
            // hit the "same trip, preserve state" branch and show the edited pair.
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            viewModel.onEvent(
                TimeTableUiEvent.TripStopChanged(
                    stopId = "NEW_FROM_STOP_ID",
                    stopName = "NEW_FROM_STOP_NAME",
                    isOrigin = true,
                ),
            )
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.journeyList.isNotEmpty())

            // User goes back and taps the original saved trip again.
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            advanceUntilIdle()

            viewModel.uiState.value.run {
                assertEquals("FROM_STOP_ID_1", this.trip?.fromStopId)
                assertEquals("TO_STOP_ID_1", this.trip?.toStopId)
                // The old "same trip, preserve state" branch left isLoading false
                // and never refetched. isLoading=true proves the original pair is
                // being reloaded fresh (the screen shows the loading state, not
                // the edited pair's journeys).
                assertTrue(isLoading)
            }
        }

    @Test
    fun `GIVEN trip reversed WHEN LoadTimeTable fires again for the original pair THEN original pair reloads fresh`() =
        runTest {
            // Reverse shares reloadWithNewTrip with the stop edit — the same
            // stale-VM bug applied: reopening the original saved trip after a
            // reverse used to hit the "same trip, preserve state" branch.
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            tripPlanningService.isSuccess = true

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            viewModel.fetchTrip()
            advanceUntilIdle()

            viewModel.onEvent(TimeTableUiEvent.ReverseTripButtonClicked)
            viewModel.fetchTrip()
            advanceUntilIdle()
            assertEquals("TO_STOP_ID_1", viewModel.uiState.value.trip?.fromStopId)

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
            advanceUntilIdle()

            viewModel.uiState.value.run {
                assertEquals("FROM_STOP_ID_1", this.trip?.fromStopId)
                assertEquals("TO_STOP_ID_1", this.trip?.toStopId)
                assertTrue(isLoading)
            }
        }

    @Test
    fun `GIVEN stop changed WHEN initializeTrip is forced for the same nav key THEN key pair wins`() =
        runTest {
            // Fresh navigation onto a surviving VM: the route key is the source
            // of truth, even though the key equals lastInitializedRouteFromTo.
            tripPlanningService.isSuccess = true
            viewModel.initializeTrip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            advanceUntilIdle()

            viewModel.onEvent(
                TimeTableUiEvent.TripStopChanged(
                    stopId = "NEW_TO_STOP_ID",
                    stopName = "NEW_TO_STOP_NAME",
                    isOrigin = false,
                ),
            )
            advanceUntilIdle()
            assertEquals("NEW_TO_STOP_ID", viewModel.uiState.value.trip?.toStopId)

            viewModel.initializeTrip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
                forceReload = true,
            )
            advanceUntilIdle()

            viewModel.uiState.value.run {
                assertEquals("TO_STOP_ID_1", this.trip?.toStopId)
                assertTrue(isLoading)
            }
        }

    @Test
    fun `GIVEN stop changed WHEN initializeTrip is not forced for the same nav key THEN edited trip is preserved`() =
        runTest {
            // Restored composition (rotation / back-from-map): the VM's current
            // trip wins; the stale nav key must not clobber the edit.
            tripPlanningService.isSuccess = true
            viewModel.initializeTrip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            advanceUntilIdle()

            viewModel.onEvent(
                TimeTableUiEvent.TripStopChanged(
                    stopId = "NEW_TO_STOP_ID",
                    stopName = "NEW_TO_STOP_NAME",
                    isOrigin = false,
                ),
            )
            advanceUntilIdle()

            viewModel.initializeTrip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            advanceUntilIdle()

            assertEquals("NEW_TO_STOP_ID", viewModel.uiState.value.trip?.toStopId)
        }

    @Test
    fun `GIVEN changed trip matches a saved trip WHEN stop is changed THEN isTripSaved reflects the new pair`() =
        runTest {
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            val savedTrip = Trip(
                fromStopId = "NEW_FROM_STOP_ID",
                fromStopName = "NEW_FROM_STOP_NAME",
                toStopId = "TO_STOP_ID_1",
                toStopName = "STOP_NAME_2",
            )
            sandook.insertOrReplaceTrip(
                tripId = savedTrip.tripId,
                fromStopId = savedTrip.fromStopId,
                fromStopName = savedTrip.fromStopName,
                toStopId = savedTrip.toStopId,
                toStopName = savedTrip.toStopName,
            )
            tripPlanningService.isSuccess = true

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip))
                advanceUntilIdle()

                viewModel.onEvent(
                    TimeTableUiEvent.TripStopChanged(
                        stopId = "NEW_FROM_STOP_ID",
                        stopName = "NEW_FROM_STOP_NAME",
                        isOrigin = true,
                    ),
                )
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("NEW_FROM_STOP_ID", state.trip?.fromStopId)
                assertTrue(state.isTripSaved)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region Save-trip prompt (story A2) — one-tap save nudge after loading an
    // unsaved pair. Frequency rules: once per app session, never for saved
    // pairs, gone forever after MAX_SAVE_TRIP_PROMPT_DISMISSALS dismissals.

    private val promptTrip = Trip(
        fromStopId = "FROM_STOP_ID_1",
        fromStopName = "STOP_NAME_1",
        toStopId = "TO_STOP_ID_1",
        toStopName = "STOP_NAME_2",
    )

    private fun loadPromptTrip() {
        tripPlanningService.isSuccess = true
        viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(promptTrip))
        viewModel.fetchTrip()
    }

    @Test
    fun `GIVEN unsaved pair WHEN timetable loads THEN save prompt is shown and shown event fires once`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics

            // Prompt shows at load time, while journeys are still loading.
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(promptTrip))
            viewModel.uiState.value.run {
                assertTrue(showSaveTripPrompt)
                assertTrue(isLoading)
            }

            // Journeys arriving must keep the prompt visible.
            tripPlanningService.isSuccess = true
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSaveTripPrompt)
            val tracked = analytics.getTrackedEvent("save_trip_prompt_shown")
            assertNotNull(tracked)
            val event = assertIs<AnalyticsEvent.SaveTripPromptShownEvent>(tracked)
            assertEquals(AnalyticsEvent.SaveTripPromptShownEvent.VARIANT_PLAIN, event.variant)
        }

    @Test
    fun `GIVEN already-saved pair WHEN timetable loads THEN no prompt is shown`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics
            sandook.insertOrReplaceTrip(
                tripId = promptTrip.tripId,
                fromStopId = promptTrip.fromStopId,
                fromStopName = promptTrip.fromStopName,
                toStopId = promptTrip.toStopId,
                toStopName = promptTrip.toStopName,
            )

            loadPromptTrip()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSaveTripPrompt)
            assertFalse(analytics.isEventTracked("save_trip_prompt_shown"))
        }

    @Test
    fun `GIVEN a different trip is already saved WHEN timetable loads THEN no prompt is shown`() =
        runTest {
            // The prompt only targets users with ZERO saved trips — once any
            // trip is saved the user has discovered the save feature.
            val analytics = fakeAnalytics as FakeAnalytics
            sandook.insertOrReplaceTrip(
                tripId = "OTHER_TRIP_ID",
                fromStopId = "OTHER_FROM",
                fromStopName = "Other From",
                toStopId = "OTHER_TO",
                toStopName = "Other To",
            )

            loadPromptTrip()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSaveTripPrompt)
            assertFalse(analytics.isEventTracked("save_trip_prompt_shown"))
        }

    @Test
    fun `GIVEN prompt visible WHEN accepted THEN trip saved and accepted plus save click events fire`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics
            loadPromptTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showSaveTripPrompt)

            viewModel.onEvent(TimeTableUiEvent.SaveTripPromptAccepted)
            advanceUntilIdle()

            viewModel.uiState.value.run {
                assertTrue(isTripSaved)
                assertFalse(showSaveTripPrompt)
            }
            assertNotNull(sandook.selectTripById(promptTrip.tripId))

            val saveClick = analytics.getTrackedEvent("save_trip_click")
            val saveClickEvent = assertIs<AnalyticsEvent.SaveTripClickEvent>(assertNotNull(saveClick))
            assertEquals(AnalyticsEvent.SaveTripClickEvent.SOURCE_PROMPT, saveClickEvent.source)

            val accepted = analytics.getTrackedEvent("save_trip_prompt_accepted")
            val acceptedEvent = assertIs<AnalyticsEvent.SaveTripPromptAcceptedEvent>(assertNotNull(accepted))
            assertEquals(AnalyticsEvent.SaveTripPromptShownEvent.VARIANT_PLAIN, acceptedEvent.variant)
        }

    @Test
    fun `GIVEN prompt visible WHEN dismissed THEN prompt hides and dismissal is persisted with count`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics
            loadPromptTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showSaveTripPrompt)

            viewModel.onEvent(TimeTableUiEvent.SaveTripPromptDismissed)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSaveTripPrompt)
            assertEquals(
                1L,
                fakePreferences.getLong(
                    SandookPreferences.KEY_SAVE_TRIP_PROMPT_DISMISSALS_PREFIX + promptTrip.tripId,
                ),
            )
            val dismissed = analytics.getTrackedEvent("save_trip_prompt_dismissed")
            val dismissedEvent =
                assertIs<AnalyticsEvent.SaveTripPromptDismissedEvent>(assertNotNull(dismissed))
            assertEquals(1, dismissedEvent.dismissCount)
        }

    @Test
    fun `GIVEN pair dismissed twice before WHEN timetable loads in a new session THEN prompt never returns for that pair`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics
            fakePreferences.setLong(
                SandookPreferences.KEY_SAVE_TRIP_PROMPT_DISMISSALS_PREFIX + promptTrip.tripId,
                TimeTableViewModel.MAX_SAVE_TRIP_PROMPT_DISMISSALS,
            )

            loadPromptTrip()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSaveTripPrompt)
            assertFalse(analytics.isEventTracked("save_trip_prompt_shown"))
        }

    @Test
    fun `GIVEN prompt already shown this session WHEN another unsaved pair loads THEN prompt is not shown again`() =
        runTest {
            val analytics = fakeAnalytics as FakeAnalytics
            loadPromptTrip()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showSaveTripPrompt)
            viewModel.onEvent(TimeTableUiEvent.SaveTripPromptDismissed)
            advanceUntilIdle()
            analytics.clear()

            // Change to a different unsaved pair and let it load.
            viewModel.onEvent(
                TimeTableUiEvent.TripStopChanged(
                    stopId = "OTHER_STOP_ID",
                    stopName = "OTHER_STOP_NAME",
                    isOrigin = false,
                ),
            )
            viewModel.fetchTrip()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSaveTripPrompt)
            assertFalse(analytics.isEventTracked("save_trip_prompt_shown"))
        }

    // endregion
}
