@file:Suppress("LongMethod", "LongParameterList")

package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.CityCodeText
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.components.SearchStopRow
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Full LazyColumn body for [SavedTripsScreen] — picks the empty / populated branch and renders the
 * matching content. Pure extraction of the original `when` block; behaviour is identical.
 */
internal fun LazyListScope.savedTripsListBody(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    emptyStateTip: String,
    pageHorizontalPadding: Dp,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit,
    onTrackingCardClick: () -> Unit,
    onStopTracking: () -> Unit,
    expandedMap: SnapshotStateMap<String, Boolean>,
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    onEnterEditing: () -> Unit,
) {
    when {
        savedTripsState.isSavedTripsLoading -> Unit

        savedTripsState.savedTrips.isEmpty() -> {
            savedTripsInfoTiles(savedTripsState, onEvent)
            item(key = "empty_state") {
                ErrorMessage(
                    emoji = "🌟",
                    title = "Let's Go! Sydney",
                    message = emptyStateTip,
                    modifier = Modifier
                        .padding(horizontal = pageHorizontalPadding)
                        .animateItem(),
                )
            }
        }

        savedTripsState.savedTrips.isNotEmpty() -> {
            savedTripsInfoTiles(savedTripsState, onEvent)
            savedTripsContent(
                savedTripsState = savedTripsState,
                trackedJourney = trackedJourney,
                onEvent = onEvent,
                onSavedTripCardClick = onSavedTripCardClick,
                onTrackingCardClick = onTrackingCardClick,
                onStopTracking = onStopTracking,
                expandedMap = expandedMap,
                editing = editing,
                reorderState = reorderState,
                onEnterEditing = onEnterEditing,
            )
        }
    }
}

private fun LazyListScope.savedTripsInfoTiles(
    savedTripsState: SavedTripsState,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    savedTripsState.infoTiles?.let { infoTiles ->
        infoTiles(
            infoTiles = infoTiles,
            onCtaClick = { tileData -> onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData)) },
            onDismissClick = { tileData -> onEvent(SavedTripUiEvent.DismissInfoTile(tileData)) },
            onTileExpand = { onEvent(SavedTripUiEvent.InfoTileExpand(it.key)) },
        )
    }
}

internal fun LazyListScope.parkRideSection(
    savedTripsState: SavedTripsState,
    expandedMap: SnapshotStateMap<String, Boolean>,
    onEvent: (SavedTripUiEvent) -> Unit,
) {
    if (savedTripsState.parkRideUiState.isEmpty()) return

    stickyHeader(key = "park_ride_title") {
        SavedTripsTitle {
            Text(text = "Park & Ride")
        }
    }

    items(
        items = savedTripsState.parkRideUiState,
        key = { parkRide -> parkRide.stopId },
    ) { parkRide ->
        val dim = KrailTheme.dimensions
        val isExpanded = expandedMap[parkRide.stopId] ?: false

        ParkRideCard(
            isExpanded = isExpanded,
            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            onClick = {
                val newExpanded = !isExpanded
                expandedMap[parkRide.stopId] = newExpanded
                onEvent(
                    SavedTripUiEvent.ParkRideCardClick(
                        parkRideState = parkRide,
                        isExpanded = newExpanded,
                    ),
                )
            },
            parkRideUiState = parkRide,
        )

        Spacer(modifier = Modifier.height(dim.spacingXL))
    }
}

@Composable
internal fun LazyItemScope.SavedTripItem(
    trip: Trip,
    stopLabels: ImmutableList<StopLabel>,
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit,
    onEnterEditing: () -> Unit,
    onDelete: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current

    ReorderableItem(reorderState, key = trip.tripId) { isDragging ->
        val rotation = rememberWiggleRotation(
            active = editing && !isDragging,
            seed = trip.tripId.hashCode(),
        )

        Column(modifier = Modifier.padding(top = if (editing) dim.spacingS else dim.spacingNone)) {
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotation }
                    .padding(horizontal = dim.pageHorizontalPadding),
            ) {
                SavedTripCard(
                    fromDisplay = trip.fromStopDisplay(stopLabels),
                    toDisplay = trip.toStopDisplay(stopLabels),
                    onCardClick = {
                        if (!editing) {
                            onSavedTripCardClick(
                                StopItem(stopId = trip.fromStopId, stopName = trip.fromStopName),
                                StopItem(stopId = trip.toStopId, stopName = trip.toStopName),
                            )
                        }
                    },
                    editing = editing,
                    onLongClick = if (!editing) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnterEditing()
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.longPressDraggableHandle(
                        enabled = editing,
                        onDragStarted = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    ),
                )

                if (editing) {
                    TripDeleteOverlay(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            Spacer(modifier = Modifier.height(dim.spacingXL))
        }
    }
}

@Composable
internal fun SavedTripsTitleBarActions(
    editing: Boolean,
    isDiscoverAvailable: Boolean,
    displayDiscoverBadge: Boolean,
    onDoneClick: () -> Unit,
    onDiscoverButtonClick: () -> Unit,
    onSettingsButtonClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    if (editing) {
        Button(
            onClick = onDoneClick,
            colors = ButtonDefaults.monochromeButtonColors(),
            dimensions = ButtonDefaults.chipButtonSize(),
            modifier = Modifier.padding(horizontal = dim.spacingM),
        ) {
            Text(text = "Done")
        }
    } else {
        if (isDiscoverAvailable) {
            RoundIconButton(
                showBadge = displayDiscoverBadge,
                onClick = onDiscoverButtonClick,
            ) {
                CityCodeText("SYD")
            }
        }

        RoundIconButton(
            onClick = onSettingsButtonClick,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = "Settings",
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(dim.spacingXXXL),
            )
        }
    }
}

@Composable
internal fun BoxScope.SavedTripsBottomSearchRow(
    visible: Boolean,
    fromStopItem: StopItem?,
    toStopItem: StopItem?,
    isExpanded: Boolean,
    isFromHighlighted: Boolean,
    showPill: Boolean,
    onExpandRequest: () -> Unit,
    onCollapseRequest: () -> Unit,
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    onReverseButtonClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        modifier = modifier,
    ) {
        SearchStopRow(
            fromStopItem = fromStopItem,
            toStopItem = toStopItem,
            isExpanded = isExpanded,
            isFromHighlighted = isFromHighlighted,
            onExpandRequest = onExpandRequest,
            onCollapseRequest = if (showPill) onCollapseRequest else null,
            fromButtonClick = fromButtonClick,
            toButtonClick = toButtonClick,
            onReverseButtonClick = onReverseButtonClick,
            onSearchButtonClick = onSearchButtonClick,
        )
    }
}
