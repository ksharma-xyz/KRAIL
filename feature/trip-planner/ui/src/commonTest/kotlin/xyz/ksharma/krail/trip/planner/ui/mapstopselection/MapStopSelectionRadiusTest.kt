package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The home map looks within the radius `nearby_stops_radius_km` sets, the one Search Stop uses. */
class MapStopSelectionRadiusTest {

    private fun radiusOf(viewModel: MapStopSelectionViewModel): Double {
        val state = viewModel.mapUiState.value
        assertIs<MapUiState.Ready>(state)
        return state.mapDisplay.searchRadiusKm
    }

    @Test
    fun `the map looks within the radius it was given`() {
        val viewModel = MapStopSelectionViewModel(FakeNearbyStopsManagerForMap(), searchRadiusKm = 5.0)

        assertEquals(5.0, radiusOf(viewModel))
    }

    @Test
    fun `with no radius given it looks within the default`() {
        val viewModel = MapStopSelectionViewModel(FakeNearbyStopsManagerForMap())

        assertEquals(NearbyStopsConfig.DEFAULT_RADIUS_KM, radiusOf(viewModel))
    }
}
