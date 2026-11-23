package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapEntry
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * The map's two existing events carry the id of the map open they belong to, and nothing else about them changes.
 * The ViewModel owns the tracker, so these drive it through [SearchStopViewModel.mapOpen] as the map composable does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStopViewModelMapSessionTest {

    private val analytics = FakeAnalytics()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SearchStopViewModel

    private val stop = NearbyStopFeature(
        stopId = "200060",
        stopName = "Central Station",
        position = LatLng(-33.8841, 151.2060),
        transportModes = persistentListOf(NswTransportMode.Train, NswTransportMode.Bus),
    )

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

    private fun openMap() = viewModel.mapOpen.viewed(
        entry = MapEntry.SEARCH_STOP,
        hadUserLocation = true,
        locationPermission = LocationResult.GRANTED,
        stopCount = 1,
    )

    private fun pickFromMap() = viewModel.onEvent(
        SearchStopUiEvent.TrackStopSelectedFromMap(
            stopId = "200060",
            searchRadiusKm = 3.0,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = true,
        ),
    )

    private fun tapMarker() = viewModel.onEvent(SearchStopUiEvent.NearbyStopClicked(stop))

    private fun marker() = analytics.getTrackedEvents("nearby_stop_click")
        .filterIsInstance<AnalyticsEvent.NearbyStopClickEvent>()

    private fun picks() = analytics.getTrackedEvents("stop_selected_from_map")
        .filterIsInstance<AnalyticsEvent.StopSelectedFromMapEvent>()

    @Test
    fun `a marker tap during an open carries that open's id`() = runTest {
        openMap()
        val id = viewModel.mapOpen.sessionId

        tapMarker()
        advanceUntilIdle()

        assertEquals(id, marker().single().mapSessionId)
        assertEquals("200060", marker().single().stopId)
        assertEquals(2, marker().single().transportModesCount)
    }

    @Test
    fun `a map pick during an open carries that open's id and every param it had`() = runTest {
        openMap()
        val id = viewModel.mapOpen.sessionId

        pickFromMap()
        advanceUntilIdle()

        val pick = picks().single()
        assertEquals(id, pick.mapSessionId)
        assertEquals("200060", pick.stopId)
        assertEquals(3.0, pick.searchRadiusKm)
        assertEquals(4, pick.enabledModesCount)
        assertEquals(12, pick.nearbyStopsCount)
        assertEquals(true, pick.hadUserLocation)
    }

    @Test
    fun `the tap and the pick of one open carry the same id as its view and its end`() = runTest {
        openMap()

        tapMarker()
        pickFromMap()
        viewModel.mapOpen.stopPicked()
        advanceUntilIdle()

        val ids = analytics.events.mapNotNull { it.properties?.get("mapSessionId") }
        assertEquals(4, ids.size)
        assertEquals(1, ids.toSet().size)
    }

    @Test
    fun `with no open in progress neither event carries an id`() = runTest {
        tapMarker()
        pickFromMap()
        advanceUntilIdle()

        assertFalse(marker().single().properties.orEmpty().containsKey("mapSessionId"))
        assertFalse(picks().single().properties.orEmpty().containsKey("mapSessionId"))
    }

    @Test
    fun `after the open ends a tap carries no id`() = runTest {
        openMap()
        viewModel.mapOpen.dismissed()

        tapMarker()
        advanceUntilIdle()

        assertFalse(marker().single().properties.orEmpty().containsKey("mapSessionId"))
    }

    @Test
    fun `a new open's events carry the new id and not the old one`() = runTest {
        openMap()
        val first = viewModel.mapOpen.sessionId
        tapMarker()
        viewModel.mapOpen.dismissed()

        openMap()
        val second = viewModel.mapOpen.sessionId
        tapMarker()
        advanceUntilIdle()

        assertNotEquals(first, second)
        assertEquals(listOf(first, second), marker().map { it.mapSessionId })
    }

    @Test
    fun `the ids sent on the two events are 16 lowercase hex characters`() = runTest {
        openMap()

        tapMarker()
        pickFromMap()
        advanceUntilIdle()

        val ids = listOf(marker().single().mapSessionId, picks().single().mapSessionId)
        ids.forEach { id ->
            assertEquals(16, id?.length)
            assertEquals(true, id?.all { it in '0'..'9' || it in 'a'..'f' })
        }
    }
}
