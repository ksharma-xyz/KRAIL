package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A map pick sent by the ViewModel says its radius is the Remote Config number, and carries that number. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStopViewModelRadiusSourceTest {

    private val analytics = FakeAnalytics()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SearchStopViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchStopViewModel(
            analytics = analytics,
            stopResultsManager = FakeStopResultsManager(),
            remoteAddressResultsManager = FakeRemoteAddressResultsManager(),
            flag = FakeFlag(),
            nearbyStopsManager = FakeNearbyStopsManagerForMap(),
            ioDispatcher = testDispatcher,
            sandook = FakeSandook(),
            searchSessionStore = RealSearchSessionStore(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pickFromMap(radiusKm: Double) = viewModel.onEvent(
        SearchStopUiEvent.TrackStopSelectedFromMap(
            stopId = "200060",
            searchRadiusKm = radiusKm,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = true,
        ),
    )

    private fun picks() = analytics.getTrackedEvents("stop_selected_from_map")
        .filterIsInstance<AnalyticsEvent.StopSelectedFromMapEvent>()

    @Test
    fun `a map pick says its radius came from Remote Config`() = runTest {
        pickFromMap(radiusKm = 3.0)
        advanceUntilIdle()

        assertEquals("remote_config", picks().single().properties?.get("radiusSource"))
    }

    @Test
    fun `the radius number is the config value that was passed in`() = runTest {
        pickFromMap(radiusKm = 2.5)
        advanceUntilIdle()

        assertEquals(2.5, picks().single().properties?.get("searchRadiusKm"))
    }

    @Test
    fun `every pick carries the marker, not only the first`() = runTest {
        pickFromMap(radiusKm = 3.0)
        pickFromMap(radiusKm = 5.0)
        advanceUntilIdle()

        assertEquals(
            listOf("remote_config", "remote_config"),
            picks().map { it.properties?.get("radiusSource") },
        )
    }
}
