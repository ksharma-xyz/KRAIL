package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.ui.components.TrackingCard
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionItem
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionSlot
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The parts of the home stack, in the order [homeStackListBody] lays them out: the tracked trip,
 * the saved trips before the Park & Ride row, the row, then the rest of the trips
 * (`docs/MAP_HOME_UX.md` rule 29). There are no headings: the stack is cards.
 */
internal fun LazyListScope.trackingSection(
    trackedJourney: TrackedJourney?,
    onTrackingCardClick: () -> Unit,
    onStopTracking: () -> Unit,
) {
    if (trackedJourney == null) return

    stickyHeader(key = "tracking_title") {
        SavedTripsTitle {
            Text(text = "Tracking Real Time")
        }
    }

    item(key = "tracking_card") {
        TrackingCard(
            tracked = trackedJourney,
            onCardClick = onTrackingCardClick,
            onStopTracking = onStopTracking,
            modifier = Modifier.padding(horizontal = KrailTheme.dimensions.spacingXL),
        )
        Spacer(modifier = Modifier.height(KrailTheme.dimensions.spacingXL))
    }
}

/** What the cards of the stack do, so the stack items stay under the parameter limit. */
internal class SavedTripActions(
    val onCardClick: (StopItem?, StopItem?) -> Unit,
    val onEvent: (SavedTripUiEvent) -> Unit,
)

/** Everything the stack draws from, so [homeStackItems] takes one thing and not ten. */
internal class HomeStackContext(
    val state: SavedTripsState,
    val expandedMap: SnapshotStateMap<String, Boolean>,
    val reorderState: ReorderableLazyListState,
    val actions: SavedTripActions,
    val suggestion: ParkRideSuggestionSlot? = null,
)

/**
 * The trips and the Park & Ride row in one list, in the order [homeStackKeys] gives: two trips, the
 * row, then the rest (rule 29), or the reorder session's order while one is open. One list means one
 * key space, so the row is dragged among the trips as a block and a trip is dragged past it.
 */
internal fun LazyListScope.homeStackItems(context: HomeStackContext) {
    val state = context.state
    val hasRow = state.parkRideUiState.any { it.facilities.isNotEmpty() }
    // The one-time suggestion takes the row's place while there is no row, and never in reorder mode.
    val showsSuggestion = context.suggestion != null && !hasRow && state.reorder == null
    val keys = withSuggestionKey(
        keys = homeStackKeys(
            tripIds = state.savedTrips.map { it.tripId },
            hasRow = hasRow,
            storedIndex = state.parkRideRowIndex,
            session = state.reorder,
        ),
        show = showsSuggestion,
        storedIndex = state.parkRideRowIndex,
    )
    val tripsById = state.savedTrips.associateBy { it.tripId }
    items(items = keys, key = { it }) { key ->
        when (key) {
            PARK_RIDE_ROW_KEY -> ParkRideRowEntry(context)
            PARK_RIDE_SUGGESTION_KEY -> context.suggestion?.let { ParkRideSuggestionItem(it) }
            else -> tripsById[key]?.let { trip ->
                ReorderableTripItem(
                    trip = trip,
                    stopLabels = state.stopLabels,
                    reorder = state.reorder,
                    reorderState = context.reorderState,
                    actions = context.actions,
                )
            }
        }
    }
}

/**
 * The Park & Ride row as one card of the stack. Open it is the row of tiles, and a long press on a
 * tile begins reorder mode with the row lifted. In reorder mode it is one outlined block with the
 * wide grip, and the whole block is the drag target. Opening a tile opens it alone and closes any
 * other, and each open or close raises the same [SavedTripUiEvent.ParkRideCardClick] the card did, so
 * the refresh set and the poll in the ViewModel stay in step with what is open.
 */
@Composable
private fun LazyItemScope.ParkRideRowEntry(context: HomeStackContext) {
    val state = context.state
    val onEvent = context.actions.onEvent
    val haptic = LocalHapticFeedback.current
    val tiles = remember(state.parkRideUiState) { state.parkRideUiState.toTiles() }
    ReorderableItem(context.reorderState, key = PARK_RIDE_ROW_KEY) { isDragging ->
        Column {
            val row: @Composable (Boolean) -> Unit = { interactive ->
                ParkRideRow(
                    tiles = tiles,
                    openStationId = context.expandedMap.entries.firstOrNull { it.value }?.key,
                    onTileClick = { tile ->
                        toggleTile(tile.stationId, state.parkRideUiState, context.expandedMap, onEvent)
                    },
                    onDirectionsClick = { carPark -> onEvent(directionsEvent(carPark)) },
                    interactive = interactive,
                    onTileLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(SavedTripUiEvent.EnterReorder(PARK_RIDE_ROW_KEY))
                    },
                )
            }
            if (state.reorder != null) {
                ParkRideReorderBlock(
                    lifted = isDragging || state.reorder?.lifted == PARK_RIDE_ROW_KEY,
                    modifier = Modifier.draggableHandle(
                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    ),
                ) { row(false) }
            } else {
                row(true)
            }
            Spacer(modifier = Modifier.height(KrailTheme.dimensions.spacingXL))
        }
    }
}

private fun directionsEvent(carPark: CarParkBlock): SavedTripUiEvent {
    val target = requireNotNull(carPark.directionsTo) { "A car park without a position has no Directions" }
    return SavedTripUiEvent.ParkRideDirectionsClick(
        latitude = target.latitude,
        longitude = target.longitude,
        label = carPark.name,
    )
}

private fun toggleTile(
    tapped: String,
    parkRideStates: ImmutableList<ParkRideUiState>,
    expandedMap: SnapshotStateMap<String, Boolean>,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    val open = expandedMap.filterValues { it }.keys
    val next = openTilesAfterTap(open = open, tapped = tapped)
    (open - next).forEach { id -> setTileOpen(id, false, parkRideStates, expandedMap, onEvent) }
    (next - open).forEach { id -> setTileOpen(id, true, parkRideStates, expandedMap, onEvent) }
}

private fun setTileOpen(
    stationId: String,
    isOpen: Boolean,
    parkRideStates: ImmutableList<ParkRideUiState>,
    expandedMap: SnapshotStateMap<String, Boolean>,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    expandedMap[stationId] = isOpen
    parkRideStates.firstOrNull { it.stopId == stationId }?.let { station ->
        onEvent(SavedTripUiEvent.ParkRideCardClick(parkRideState = station, isExpanded = isOpen))
    }
}
