package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionSlot
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toTile
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.stopcard.ParkRidePinCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.PickRole
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPickActions
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPlacement
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardWithDepartures
import xyz.ksharma.krail.trip.planner.ui.stopcard.parkRidePickActionFor

/**
 * The map home, wired. Everything the home screen asks of the world comes in as state and callbacks, and the
 * pieces only it knows meet here: the pins, the card a pin opens, the location pill and the one map.
 *
 * The card's key is saved, so a rotation keeps the card open on the same pin, and it is looked up again in the
 * pins on every draw, so a pin that has left the map closes its card. A pin tap and a tap on the map both come
 * from the map as a key, and the screen draws the card above the cards and the chips, never under them.
 */
@Composable
internal fun HomeMapEntryContent(
    savedTripsState: SavedTripsState,
    homeMapState: HomeMapState,
    mapUiState: MapUiState,
    userLocationManager: UserLocationManager,
    onMapEvent: (MapStopSelectionEvent) -> Unit,
    modifier: Modifier = Modifier,
    trackedJourney: TrackedJourney? = null,
    aiState: AiSearchInputUiState = AiSearchInputUiState(),
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    aiVoiceLevel: () -> Float = { 0f },
    actions: HomeMapActions = HomeMapActions(),
    suggestion: ParkRideSuggestionSlot? = null,
) {
    var cardKey by rememberSaveable { mutableStateOf<String?>(null) }
    val ready = mapUiState as? MapUiState.Ready
    val nearby = ready?.mapDisplay?.nearbyStops ?: persistentListOf()
    val pins = remember(nearby, homeMapState.parkRidePins, homeMapState.labelPins) {
        homePinsFor(nearby = nearby, parkRide = homeMapState.parkRidePins, labels = homeMapState.labelPins)
    }
    val location = rememberHomeLocationState(
        userLocationManager = userLocationManager,
        hasUserLocation = ready?.mapDisplay?.userLocation != null,
    )
    val notices = remember(location.pill, homeMapState.mapOffline) {
        homeNoticesFor(location.pill, homeMapState.mapOffline).toImmutableList()
    }
    val closeCard = { cardKey = null }

    val host = HomeMapHost(
        map = { ornamentTopPadding ->
            HomeMapPane(
                mapUiState = mapUiState,
                pins = pins,
                allowPermissionRequest = location.allowPermissionRequest,
                onPermissionStatus = location.onPermissionStatus,
                onMapEvent = onMapEvent,
                onCardKey = { cardKey = it },
                ornamentTopPadding = ornamentTopPadding,
            )
        },
        notices = notices,
        locationPill = location.pill,
        onLocationClick = location.onPillClick,
        card = homeCardContent(
            card = homeCardFor(cardKey, pins),
            state = savedTripsState,
            onEvent = actions.onEvent,
            onClose = closeCard,
        ),
        onCardClose = closeCard,
    )

    HomeMapScreen(
        savedTripsState = savedTripsState,
        host = host,
        modifier = modifier,
        trackedJourney = trackedJourney,
        aiState = aiState,
        onAiEvent = onAiEvent,
        aiVoiceLevel = aiVoiceLevel,
        actions = actions,
        suggestion = suggestion,
    )
}

/** What a tap on the home screen does, given by the entry that owns the navigation and the ViewModel. */
internal data class HomeMapActions(
    val fromButtonClick: () -> Unit = {},
    val toButtonClick: () -> Unit = {},
    val onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    val onSearchButtonClick: () -> Unit = {},
    val onSettingsButtonClick: () -> Unit = {},
    val onDiscoverButtonClick: () -> Unit = {},
    val onEvent: (SavedTripUiEvent) -> Unit = {},
    val onTrackingCardClick: () -> Unit = {},
    val onStopTracking: () -> Unit = {},
    val onAddParkRideClick: () -> Unit = {},
    val onLabelChipClick: (labelKey: String) -> Unit = {},
)

/**
 * The floating card for [card], or null with none open.
 *
 * A stop pin or a label pin opens the stop card with its board closed, so looking at pins fetches nothing. It offers
 * Save as and Go here, which sets the destination as the pin's sheet always did on a wide window. A `P` pin opens
 * the Park & Ride card of the station on the stack, with Go here when the station has a stop in the stops table,
 * Directions to each car park's own position and Remove from home.
 */
private fun homeCardContent(
    card: HomeCard?,
    state: SavedTripsState,
    onEvent: (SavedTripUiEvent) -> Unit,
    onClose: () -> Unit,
): (@Composable BoxScope.(HomeCardPlacement) -> Unit)? = when (card) {
    null -> null
    is HomeCard.Stop -> stopCardContent(card, state, onEvent, onClose)
    is HomeCard.ParkRide -> parkRideCardContent(card, state, onEvent, onClose)
}

private fun stopCardContent(
    card: HomeCard.Stop,
    state: SavedTripsState,
    onEvent: (SavedTripUiEvent) -> Unit,
    onClose: () -> Unit,
): @Composable BoxScope.(HomeCardPlacement) -> Unit = { placement ->
    StopCardPlacement(topReserve = placement.topReserve, onHeight = placement.onHeight) {
        StopCardWithDepartures(
            stop = card.stop,
            onClose = onClose,
            actions = {
                val destination = StopItem(stopId = card.stop.stopId, stopName = card.stop.stopName)
                StopCardPickActions(
                    stop = destination,
                    options = homeStopCardOptions(labels = state.stopLabels, onEvent = onEvent),
                    onPick = {
                        onEvent(SavedTripUiEvent.ToStopChanged(destination.toJsonString()))
                        onClose()
                    },
                )
            },
        )
    }
}

private fun parkRideCardContent(
    card: HomeCard.ParkRide,
    state: SavedTripsState,
    onEvent: (SavedTripUiEvent) -> Unit,
    onClose: () -> Unit,
): (@Composable BoxScope.(HomeCardPlacement) -> Unit)? {
    val saved = state.parkRideUiState.firstOrNull { it.stopId == card.pin.stationId } ?: return null
    val tile = saved.toTile()
    return { placement ->
        StopCardPlacement(topReserve = placement.topReserve, onHeight = placement.onHeight) {
            ParkRidePinCard(
                tile = tile,
                isOnHome = true,
                onToggleHome = {
                    onEvent(SavedTripUiEvent.RemoveParkRideStation(saved))
                    onClose()
                },
                onDirectionsClick = { carPark ->
                    carPark.directionsTo?.let { target ->
                        onEvent(
                            SavedTripUiEvent.ParkRideDirectionsClick(
                                latitude = target.latitude,
                                longitude = target.longitude,
                                label = carPark.name,
                            ),
                        )
                    }
                },
                onClose = onClose,
                pickAction = parkRidePickActionFor(
                    stationName = card.pin.stationName,
                    goHereStopId = card.pin.goHereStopId,
                    role = PickRole.Destination,
                ),
                onPick = { stop ->
                    onEvent(SavedTripUiEvent.ToStopChanged(stop.toJsonString()))
                    onClose()
                },
            )
        }
    }
}
