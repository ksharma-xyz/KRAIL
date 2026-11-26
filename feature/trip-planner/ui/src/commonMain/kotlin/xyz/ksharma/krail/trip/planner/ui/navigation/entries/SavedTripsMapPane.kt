package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.taj.modifier.cornerSafeTopPadding
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.homemap.homeStopCardOptions
import xyz.ksharma.krail.trip.planner.ui.homemap.rememberHomeLocationState
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationStatusChip
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapEntry
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapOpenTracker
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The right pane of the Saved Trips screen on a wide window: the map, [MapStopSelectionPane] as it always was,
 * with the one place the location permission is asked for on this screen. While the permission is not granted
 * the Search Stop chip ([LocationStatusChip]) sits at the map's top left corner, below the corner safe inset the
 * map keeps its own controls clear of, and a tap asks for it or opens Settings. The map never raises the system
 * dialog on its own, so the chip is the only way the question is asked, and it is not on the list pane or on the
 * phone, which has no map.
 *
 * The chip is a sibling drawn over the map, never a parent of it, so the native map still renders on iOS.
 *
 * [mapOpen] counts each stretch of this map being on screen for analytics, as the `home` entry. Null counts nothing.
 */
@Composable
internal fun SavedTripsMapPane(
    mapUiState: MapUiState,
    userLocationManager: UserLocationManager,
    onMapEvent: (MapStopSelectionEvent) -> Unit,
    onStopSelected: (StopItem) -> Unit,
    labels: ImmutableList<StopLabel>,
    onEvent: (SavedTripUiEvent) -> Unit,
    mapOpen: MapOpenTracker? = null,
) {
    val dim = KrailTheme.dimensions
    val location = rememberHomeLocationState(
        userLocationManager = userLocationManager,
        hasUserLocation = (mapUiState as? MapUiState.Ready)?.mapDisplay?.userLocation != null,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        MapStopSelectionPane(
            mapUiState = mapUiState,
            onEvent = onMapEvent,
            onStopSelected = onStopSelected,
            stopCardOptions = homeStopCardOptions(labels = labels, onEvent = onEvent),
            permissionRequest = location.permissionRequest,
            onPermissionStatus = location.onPermissionStatus,
            mapOpen = mapOpen,
            mapEntry = MapEntry.HOME,
        )
        LocationStatusChip(
            pill = location.pill,
            onClick = location.onPillClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = dim.pageHorizontalPadding, top = cornerSafeTopPadding() + dim.spacingM),
        )
    }
}

/**
 * Go here on the home side map. The home has one search bar and nowhere to hold a stop, so journey search opens with
 * [destination] already its destination, editing where from.
 */
internal fun TripPlannerNavigator.goHereFromHomeMap(destination: StopItem) {
    navigateToSearchStop(fieldType = SearchStopFieldType.FROM, presetTo = destination)
}
