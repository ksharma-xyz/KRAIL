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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.CityCodeText
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.components.SearchStopRow
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Full LazyColumn body for [SavedTripsScreen], `docs/MAP_HOME_UX.md` rule 29: info tiles, the
 * tracked trip, two trips, the one Park & Ride row, then the rest of the trips. With one trip the
 * row follows it, with none it comes first, and with no saved station there is no row at all.
 */
internal fun LazyListScope.savedTripsListBody(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit,
    onTrackingCardClick: () -> Unit,
    onStopTracking: () -> Unit,
    expandedMap: SnapshotStateMap<String, Boolean>,
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    onEnterEditing: () -> Unit,
) {
    if (savedTripsState.isSavedTripsLoading) return

    savedTripsInfoTiles(savedTripsState, onEvent)
    trackingSection(trackedJourney, onTrackingCardClick, onStopTracking)

    val trips = savedTripsState.savedTrips
    if (trips.isNotEmpty()) {
        reorderTipItems(savedTripsState.hasSeenReorderTip, editing, onEvent)
    }

    val actions = SavedTripActions(onSavedTripCardClick, onEnterEditing, onEvent)
    val (before, after) = trips.splitAroundParkRideRow(rowIndexFor(trips.size))
    tripItems(before, savedTripsState.stopLabels, editing, reorderState, actions)
    parkRideRowItem(savedTripsState.parkRideUiState, expandedMap, onEvent)
    tripItems(after, savedTripsState.stopLabels, editing, reorderState, actions)
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
                    modifier = Modifier
                        .testTag(TripPlannerTestTags.savedTripCard(trip.tripId))
                        .longPressDraggableHandle(
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
            modifier = Modifier
                .testTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE)
                .padding(horizontal = dim.spacingM),
        ) {
            Text(text = "Done")
        }
    } else {
        if (isDiscoverAvailable) {
            RoundIconButton(
                showBadge = displayDiscoverBadge,
                onClick = onDiscoverButtonClick,
                modifier = Modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DISCOVER),
            ) {
                CityCodeText("SYD")
            }
        }

        RoundIconButton(
            onClick = onSettingsButtonClick,
            modifier = Modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS),
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
    onSearchButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReverseButtonClick: () -> Unit = {},
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    isAiSearchAvailable: Boolean = false,
    isAiHandoffSettling: Boolean = false,
    dateTimeSelectionText: String? = null,
    onDateTimeSelectionClear: () -> Unit = {},
    homeChips: ImmutableList<HomeChip> = persistentListOf(),
    onLabelChipClick: (labelKey: String) -> Unit = {},
    onParkRideChipClick: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        modifier = modifier,
    ) {
        Column {
            // Above the row and inside its column, so the chips share the cards' edges. Only while
            // the row is open: phone landscape collapses it to one button because height is short.
            if (isExpanded) {
                HomeChipRow(
                    chips = homeChips,
                    onLabelClick = onLabelChipClick,
                    onParkRideClick = onParkRideChipClick,
                )
            }
            SearchStopRow(
                fromStopItem = fromStopItem,
                toStopItem = toStopItem,
                isExpanded = isExpanded,
                isFromHighlighted = isFromHighlighted,
                onExpandRequest = onExpandRequest,
                onCollapseRequest = if (showPill) onCollapseRequest else null,
                fromButtonClick = fromButtonClick,
                toButtonClick = toButtonClick,
                onSearchButtonClick = onSearchButtonClick,
                onReverseButtonClick = onReverseButtonClick,
                onAiEvent = onAiEvent,
                isAiSearchAvailable = isAiSearchAvailable,
                isAiHandoffSettling = isAiHandoffSettling,
                dateTimeSelectionText = dateTimeSelectionText,
                onDateTimeSelectionClear = onDateTimeSelectionClear,
            )
        }
    }
}
