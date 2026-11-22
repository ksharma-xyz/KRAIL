package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableLazyListState
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.ui.components.TrackingCard
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * The parts of the home stack, in the order [savedTripsListBody] lays them out: the tracked trip,
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

/** What a saved trip card does, so the trip items stay under the parameter limit. */
internal class SavedTripActions(
    val onCardClick: (StopItem?, StopItem?) -> Unit,
    val onEnterEditing: () -> Unit,
    val onEvent: (SavedTripUiEvent) -> Unit,
)

/** A run of saved trips, each keyed by its own id so the two runs around the row never collide. */
internal fun LazyListScope.tripItems(
    trips: List<Trip>,
    stopLabels: ImmutableList<StopLabel>,
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    actions: SavedTripActions,
) {
    items(items = trips, key = { trip -> trip.tripId }) { trip ->
        SavedTripItem(
            trip = trip,
            stopLabels = stopLabels,
            editing = editing,
            reorderState = reorderState,
            onSavedTripCardClick = actions.onCardClick,
            onEnterEditing = actions.onEnterEditing,
            onDelete = { actions.onEvent(SavedTripUiEvent.DeleteSavedTrip(trip)) },
        )
    }
}

/**
 * The Park & Ride row, one list item, and only when a station is saved: with none there is no row
 * and no card in its place. Opening a tile opens it alone and closes any other, and each open or
 * close raises the same [SavedTripUiEvent.ParkRideCardClick] the card did, so the refresh set and
 * the poll in the ViewModel stay in step with what is open.
 */
internal fun LazyListScope.parkRideRowItem(
    parkRideStates: ImmutableList<ParkRideUiState>,
    expandedMap: SnapshotStateMap<String, Boolean>,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    if (parkRideStates.none { it.facilities.isNotEmpty() }) return

    item(key = "park_ride_row") {
        val tiles = remember(parkRideStates) { parkRideStates.toTiles() }
        Column {
            ParkRideRow(
                tiles = tiles,
                openStationId = expandedMap.entries.firstOrNull { it.value }?.key,
                onTileClick = { tile -> toggleTile(tile.stationId, parkRideStates, expandedMap, onEvent) },
                onDirectionsClick = { carPark -> onEvent(directionsEvent(carPark)) },
            )
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

/**
 * The two tip lines above the trips. They are still here until reorder mode (branch 9) replaces
 * the long press hint, and appear only for a rider who has trips.
 */
internal fun LazyListScope.reorderTipItems(
    hasSeenReorderTip: Boolean,
    editing: Boolean,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    item(key = "onboarding_reorder_tip") {
        val dim = KrailTheme.dimensions
        AnimatedVisibility(
            visible = !hasSeenReorderTip && !editing,
            enter = expandVertically(animationSpec = tween(durationMillis = 300)) +
                fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            LaunchedEffect(Unit) { onEvent(SavedTripUiEvent.MarkReorderTipSeen) }
            Text(
                text = "💡 Long press any trip to reorder or remove it.",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier
                    .padding(horizontal = dim.pageHorizontalPadding)
                    .padding(bottom = dim.spacingM),
            )
        }
    }

    item(key = "reorder_hint") {
        val dim = KrailTheme.dimensions
        AnimatedVisibility(
            visible = editing,
            enter = expandVertically(animationSpec = tween(durationMillis = 250)) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            Text(
                text = "💡 Long press and drag cards to reorder. Tap Done when finished.",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier
                    .padding(horizontal = dim.pageHorizontalPadding)
                    .padding(bottom = dim.spacingM),
            )
        }
    }
}
