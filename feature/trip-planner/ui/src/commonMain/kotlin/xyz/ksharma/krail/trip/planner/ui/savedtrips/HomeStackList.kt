package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import app.krail.taj.resources.Res as TajRes

// The map home's list body and its reorderable trip card. The classic Saved Trips screen does not use them:
// it draws savedTripsListBody and SavedTripItem in SavedTripsScreenSections.kt.

/**
 * Full LazyColumn body of the map home, `docs/MAP_HOME_UX.md` rule 29: info tiles, the tracked trip,
 * then the stack: two trips, the one Park & Ride row, then the rest of the trips. With one trip the
 * row follows it, with none it comes first, and with no saved station there is no row.
 */
internal fun LazyListScope.homeStackListBody(
    context: HomeStackContext,
    trackedJourney: TrackedJourney?,
    onTrackingCardClick: () -> Unit,
    onStopTracking: () -> Unit,
) {
    if (context.state.isSavedTripsLoading) return

    val onEvent = context.actions.onEvent
    context.state.infoTiles?.let { infoTiles ->
        infoTiles(
            infoTiles = infoTiles,
            onCtaClick = { tileData -> onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData)) },
            onDismissClick = { tileData -> onEvent(SavedTripUiEvent.DismissInfoTile(tileData)) },
            onTileExpand = { onEvent(SavedTripUiEvent.InfoTileExpand(it.key)) },
        )
    }
    trackingSection(trackedJourney, onTrackingCardClick, onStopTracking)
    homeStackItems(context)
}

/**
 * One saved trip card of the map home. In reorder mode it carries a grip centred on its top edge and
 * the delete circle, the whole card is the drag target, and the card the long press lifted is drawn
 * lifted. Outside it a long press on the card is the one way in: it begins the mode with this card
 * lifted.
 */
@Composable
internal fun LazyItemScope.ReorderableTripItem(
    trip: Trip,
    stopLabels: ImmutableList<StopLabel>,
    reorder: ReorderSession?,
    reorderState: ReorderableLazyListState,
    actions: SavedTripActions,
) {
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current
    val editing = reorder != null

    ReorderableItem(reorderState, key = trip.tripId) { isDragging ->
        Column(modifier = Modifier.padding(top = if (editing) dim.spacingS else dim.spacingNone)) {
            Box(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding)) {
                SavedTripCard(
                    fromDisplay = trip.fromStopDisplay(stopLabels),
                    toDisplay = trip.toStopDisplay(stopLabels),
                    onCardClick = {
                        if (!editing) {
                            actions.onCardClick(
                                StopItem(stopId = trip.fromStopId, stopName = trip.fromStopName),
                                StopItem(stopId = trip.toStopId, stopName = trip.toStopName),
                            )
                        }
                    },
                    editing = editing,
                    onLongClick = if (!editing) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = trip.tripId))
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .testTag(TripPlannerTestTags.savedTripCard(trip.tripId))
                        .liftedLook(isDragging || reorder?.lifted == trip.tripId)
                        .draggableHandle(
                            enabled = editing,
                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        ),
                )

                if (editing) {
                    ReorderGrip(
                        key = trip.tripId,
                        wide = false,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    ReorderDeleteOverlay(
                        onClick = { actions.onEvent(SavedTripUiEvent.ReorderRemove(tripId = trip.tripId)) },
                        tag = TripPlannerTestTags.Reorder.delete(trip.tripId),
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            Spacer(modifier = Modifier.height(dim.spacingXL))
        }
    }
}

/**
 * The delete circle on a trip card in the map home's reorder mode (`docs/MAP_HOME_UX.md` rule 38): a 24 dp
 * circle in the ink colour with a 12 dp X, at the card's top right corner and overhanging it by 6 dp. Its
 * touch target is 48 dp, centred on the circle, so the drawn circle stays small and the tap is not.
 */
@Composable
internal fun ReorderDeleteOverlay(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            // The circle overhangs the corner by 6 dp, so its centre is 6 dp in from the edge and
            // 6 dp down from the top. The 48 dp box that takes the tap is centred on it.
            .offset(x = DELETE_TARGET_OFFSET, y = -DELETE_TARGET_OFFSET)
            .size(DELETE_TARGET)
            // After the offset, so the tag's bounds are where the target is.
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .klickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dim.spacingXXXL)
                .clip(CircleShape)
                .background(KrailTheme.colors.onSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(TajRes.drawable.ic_close),
                contentDescription = "Remove saved trip",
                colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
                modifier = Modifier.size(dim.spacingL),
            )
        }
    }
}

private val DELETE_TARGET = 48.dp

// The 48 dp target is centred on the circle: 24 dp from its edge, less the 6 dp the circle overhangs.
private val DELETE_TARGET_OFFSET = 18.dp
