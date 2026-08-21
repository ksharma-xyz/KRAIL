package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * `MapStateHelper` is the SearchStop map's whole state-reduction layer and was uncovered.
 *
 * Every mutator funnels through a private `withMapState` guard that returns the state untouched
 * when the map is not `Ready`. That guard is the thing worth holding: without it a pan or a mode
 * toggle arriving while the map is still loading would either crash or silently promote a Loading
 * map to Ready with a half-built display.
 */
class MapStateHelperTest {

    private val ready = SearchStopState(mapUiState = MapUiState.Ready())
    private val loading = SearchStopState(mapUiState = MapUiState.Loading)

    @Test
    fun `GIVEN a ready map WHEN reading it THEN the ready state is returned`() {
        assertSame(ready.mapUiState, MapStateHelper.getReadyMapState(ready))
    }

    @Test
    fun `GIVEN a map that is not ready WHEN reading it THEN null comes back instead of a cast crash`() {
        assertNull(MapStateHelper.getReadyMapState(loading))
        assertNull(MapStateHelper.getReadyMapState(SearchStopState(mapUiState = null)))
        assertNull(MapStateHelper.getReadyMapState(SearchStopState(mapUiState = MapUiState.Error("boom"))))
    }

    @Test
    fun `GIVEN a pan WHEN the centre updates THEN only the centre moves`() {
        val moved = MapStateHelper.updateMapCenter(ready, LatLng(latitude = -33.75, longitude = 151.05))

        assertEquals(LatLng(latitude = -33.75, longitude = 151.05), moved.display().mapCenter)
        assertEquals(ready.display().userLocation, moved.display().userLocation)
    }

    @Test
    fun `GIVEN a GPS fix WHEN the user location updates THEN it is stored and can be cleared again`() {
        val located = MapStateHelper.updateUserLocation(ready, LatLng(latitude = -33.87, longitude = 151.21))
        assertEquals(LatLng(latitude = -33.87, longitude = 151.21), located.display().userLocation)

        val cleared = MapStateHelper.updateUserLocation(located, null)
        assertNull(cleared.display().userLocation)
    }

    @Test
    fun `GIVEN nearby stops WHEN they land THEN each becomes a map feature and loading is resolved`() {
        val loadingState = MapStateHelper.setLoadingState(ready, isLoading = true)
        assertTrue(loadingState.readyMap().isLoadingNearbyStops)

        val updated = MapStateHelper.updateNearbyStops(
            state = loadingState,
            stops = listOf(
                NearbyStop(
                    stopId = "200060",
                    stopName = "Central Station",
                    latitude = -33.88,
                    longitude = 151.21,
                    transportModes = listOf(TransportMode.Train, TransportMode.Bus),
                ),
            ),
        )

        val feature = updated.display().nearbyStops.single()
        assertEquals("200060", feature.stopId)
        assertEquals("Central Station", feature.stopName)
        assertEquals(LatLng(latitude = -33.88, longitude = 151.21), feature.position)
        assertEquals(listOf(TransportMode.Train, TransportMode.Bus), feature.transportModes)
        assertTrue(!updated.readyMap().isLoadingNearbyStops)
    }

    @Test
    fun `GIVEN a mode filter WHEN toggled THEN its product class is added and removed again`() {
        val onlyTrains = ready.copy(
            mapUiState = MapUiState.Ready(
                mapDisplay = MapDisplay(selectedTransportModes = persistentSetOf(TransportMode.Train.productClass)),
            ),
        )

        val withFerry = MapStateHelper.toggleTransportMode(onlyTrains, TransportMode.Ferry)
        assertEquals(
            setOf(TransportMode.Train.productClass, TransportMode.Ferry.productClass),
            withFerry.display().selectedTransportModes,
        )

        val ferryOff = MapStateHelper.toggleTransportMode(withFerry, TransportMode.Ferry)
        assertEquals(setOf(TransportMode.Train.productClass), ferryOff.display().selectedTransportModes)
    }

    @Test
    fun `GIVEN the map options sheet WHEN radius and chrome change THEN each lands on the display`() {
        val tuned = MapStateHelper.toggleCompass(
            MapStateHelper.toggleDistanceScale(
                MapStateHelper.updateSearchRadius(ready, radiusKm = 5.0),
                enabled = true,
            ),
            enabled = false,
        )

        assertEquals(5.0, tuned.display().searchRadiusKm)
        assertTrue(tuned.display().showDistanceScale)
        assertTrue(!tuned.display().showCompass)
    }

    @Test
    fun `GIVEN a map that is not ready WHEN any mutator runs THEN the state is returned untouched`() {
        val mutated = listOf(
            MapStateHelper.updateMapCenter(loading, LatLng(latitude = -33.75, longitude = 151.05)),
            MapStateHelper.updateUserLocation(loading, LatLng(latitude = -33.75, longitude = 151.05)),
            MapStateHelper.updateNearbyStops(loading, stops = emptyList(), isLoading = true),
            MapStateHelper.setLoadingState(loading, isLoading = true),
            MapStateHelper.toggleTransportMode(loading, TransportMode.Ferry),
            MapStateHelper.updateSearchRadius(loading, radiusKm = 5.0),
            MapStateHelper.toggleDistanceScale(loading, enabled = true),
            MapStateHelper.toggleCompass(loading, enabled = false),
        )

        assertEquals(List(mutated.size) { loading }, mutated)
    }

    private fun SearchStopState.readyMap() = mapUiState as MapUiState.Ready

    private fun SearchStopState.display() = readyMap().mapDisplay
}
