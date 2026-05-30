@file:Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")

package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.ui.components.TrackingCard
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.CityCodeText
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.components.SearchStopRow
import xyz.ksharma.krail.trip.planner.ui.departureboard.departureBoardAccordionSection
import xyz.ksharma.krail.trip.planner.ui.state.departureboard.DepartureBoardUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

private const val LAZY_COLUMN_BOTTOM_PADDING = 300

@Composable
fun SavedTripsScreen(
    savedTripsState: SavedTripsState,
    modifier: Modifier = Modifier,
    trackedJourney: TrackedJourney? = null,
    fromButtonClick: () -> Unit = {},
    toButtonClick: () -> Unit = {},
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onSearchButtonClick: () -> Unit = {},
    onSettingsButtonClick: () -> Unit = {},
    onDiscoverButtonClick: () -> Unit = {},
    onEvent: (SavedTripUiEvent) -> Unit = {},
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    departureBoardEntries: ImmutableList<StopDepartureBoardEntry> = persistentListOf(),
    expandedDepartureBoardStopId: String? = null,
    onDepartureBoardEvent: (DepartureBoardUiEvent) -> Unit = {},
    // Slot for the dual-pane right side. SavedTrips knows nothing about its content —
    // entry passes a MapStopSelectionPane (or anything else). Empty slot = blank pane.
    rightPane: @Composable BoxScope.() -> Unit = {},
    // Fired when the bottom action pill triggers nav.
    onCompactHeightPlanTrip: () -> Unit = {},
    onCompactHeightSetDestination: () -> Unit = {},
    onCompactHeightFindTrip: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val iconColor = if (isAppInDarkMode().not()) themeColor() else KrailTheme.colors.onSurface
    val emptyStateTip = remember {
        buildList {
            add("Tap ★ on a trip to save it here.")
            add("Saved trips also show Park & Ride.")
            add("Find nearby stops on the map.")
        }.random()
    }

    // Pill (phone-portrait only) is shown when there's enough saved content above
    // to justify collapsing the search row out of the way:
    //   • 2+ saved trips, OR
    //   • 1 saved trip + at least one Park & Ride entry.
    // Anything less and the expanded row stays — first-time and lightly-used
    // accounts still get the full search affordance front-and-centre.
    val savedTripsCount = savedTripsState.savedTrips.size
    val hasParkRide = savedTripsState.parkRideUiState.isNotEmpty()

    // Adaptive layout. Three orientation buckets:
    //   - Phone portrait (COMPACT width): single-pane. Existing pill logic
    //     (collapse SearchStopRow into "Plan a trip" pill when many trips).
    //   - Phone landscape (isPhoneLandscape): dual-pane with the new
    //     state-aware nav pill at the bottom (not the SearchStopRow).
    //   - Tablet / foldable-unfolded (isTablet): dual-pane. SearchStopRow is
    //     ALWAYS expanded — plenty of room, no pill collapse regardless of
    //     trip count.
    // See docs/TABLET_FOLDABLE_UX.md §4 + §5.
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
    val dualPane = adaptiveLayoutInfo.shouldShowDualPane
    val isPhoneLandscape = adaptiveLayoutInfo.isPhoneLandscape
    val isTablet = adaptiveLayoutInfo.isTablet

    val showPill = when {
        isTablet -> false
        isPhoneLandscape -> false // phone landscape uses the new pill below, not this one
        else -> savedTripsCount >= 2 || (savedTripsCount >= 1 && hasParkRide)
    }

    // Search row expand / from-highlight state — rememberSaveable survives rotation.
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var isFromHighlighted by rememberSaveable { mutableStateOf(false) }

    // When the pill condition isn't met, always show the expanded row.
    val effectiveIsExpanded = if (showPill) isSearchExpanded else true

    // Edit mode for trip cards — long-press enters, Done button exits.
    var editing by rememberSaveable { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val tripId = from.key as? String ?: return@rememberReorderableLazyListState
        val toTripId = to.key as? String ?: return@rememberReorderableLazyListState
        val fromTripIndex = savedTripsState.savedTrips.indexOfFirst { it.tripId == tripId }
        val toTripIndex = savedTripsState.savedTrips.indexOfFirst { it.tripId == toTripId }
        if (fromTripIndex != -1 && toTripIndex != -1) {
            onEvent(SavedTripUiEvent.MoveSavedTripToIndex(tripId = tripId, targetIndex = toTripIndex))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .onSizeChanged { log("[MAP_STOP_SEL] outerBox size=${it.width}x${it.height}") },
    ) {
        val body: @Composable BoxScope.() -> Unit = {
            // Push content away from a side display cutout in landscape. Horizontal-only
            // inset means portrait is untouched (cutout there is already covered by the
            // status bar inset that TitleBar handles internally), but in landscape the
            // whole column shifts right of the camera notch.
            Column(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
                ),
            ) {
                TitleBar(
                    title = {
                        Text(text = "KRAIL", color = themeColor())
                    },
                    actions = {
                        if (editing) {
                            Button(
                                onClick = { editing = false },
                                colors = ButtonDefaults.monochromeButtonColors(),
                                dimensions = ButtonDefaults.chipButtonSize(),
                                modifier = Modifier.padding(horizontal = dim.spacingM),
                            ) {
                                Text(text = "Done")
                            }
                        } else {
                            if (savedTripsState.isDiscoverAvailable) {
                                RoundIconButton(
                                    showBadge = savedTripsState.displayDiscoverBadge,
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
                    },
                )

                val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = LAZY_COLUMN_BOTTOM_PADDING.dp),
                ) {
                    when {
                        savedTripsState.isSavedTripsLoading -> Unit

                        savedTripsState.savedTrips.isEmpty() -> {
                            savedTripsState.infoTiles?.let { infoTiles ->
                                infoTiles(
                                    infoTiles = infoTiles,
                                    onCtaClick = { tileData ->
                                        onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData))
                                    },
                                    onDismissClick = { tileData ->
                                        onEvent(SavedTripUiEvent.DismissInfoTile(tileData))
                                    },
                                    onTileExpand = {
                                        onEvent(SavedTripUiEvent.InfoTileExpand(it.key))
                                    },
                                )
                            }

                            item(key = "empty_state") {
                                ErrorMessage(
                                    emoji = "🌟",
                                    title = "Let's Go! Sydney",
                                    message = emptyStateTip,
                                    modifier = Modifier
                                        .padding(horizontal = dim.pageHorizontalPadding)
                                        .animateItem(),
                                )
                            }
                        }

                        savedTripsState.savedTrips.isNotEmpty() -> {
                            savedTripsState.infoTiles?.let { infoTiles ->
                                infoTiles(
                                    infoTiles = infoTiles,
                                    onCtaClick = { tileData ->
                                        onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData))
                                    },
                                    onDismissClick = { tileData ->
                                        onEvent(SavedTripUiEvent.DismissInfoTile(tileData))
                                    },
                                    onTileExpand = {
                                        onEvent(SavedTripUiEvent.InfoTileExpand(it.key))
                                    },
                                )
                            }

                            savedTripsContent(
                                savedTripsState = savedTripsState,
                                trackedJourney = trackedJourney,
                                iconColor = iconColor,
                                onEvent = onEvent,
                                onSavedTripCardClick = onSavedTripCardClick,
                                onTrackingCardClick = onTrackingCardClick,
                                onStopTracking = onStopTracking,
                                expandedMap = expandedMap,
                                departureBoardEntries = departureBoardEntries,
                                expandedDepartureBoardStopId = expandedDepartureBoardStopId,
                                onDepartureBoardEvent = onDepartureBoardEvent,
                                editing = editing,
                                reorderState = reorderState,
                                onEnterEditing = { editing = true },
                            )
                        }
                    }
                }
            }

            // Hold the bottom row off-screen until the saved-trips load has emitted at
            // least once. Without this gate, the row renders against an empty
            // savedTrips list (pill condition false → expanded), then flips to the
            // pill once the DB lands — a visible flash of the wrong UI.
            //
            // Reveal is a plain slide-up + fade for both the collapsed pill and the
            // expanded search row. The pill's "alive" pulse lives inside CollapsedPill
            // itself — keeping the row-level reveal calm avoids stacking two bouncy
            // animations (parent scale + child pulse) on top of each other.
            AnimatedVisibility(
                visible = !savedTripsState.isSavedTripsLoading && !editing,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (isPhoneLandscape) {
                    // Phone landscape only: bottom SearchStopRow slides up and feels
                    // weird in this tight geometry, and the pill's inline expand
                    // path makes it worse. Replace with a state-aware navigation
                    // pill that takes the user to the full-screen SearchStop flow.
                    // Tablets in landscape get the regular expanded SearchStopRow
                    // (the `else` branch) — they have plenty of room.
                    SavedTripsCompactHeightActionPill(
                        fromStop = savedTripsState.fromStop,
                        toStop = savedTripsState.toStop,
                        onPlanTrip = onCompactHeightPlanTrip,
                        onSetDestination = onCompactHeightSetDestination,
                        onFindTrip = onCompactHeightFindTrip,
                    )
                } else {
                    SearchStopRow(
                        fromStopItem = savedTripsState.fromStop,
                        toStopItem = savedTripsState.toStop,
                        isExpanded = effectiveIsExpanded,
                        isFromHighlighted = isFromHighlighted,
                        onExpandRequest = {
                            isSearchExpanded = true
                            isFromHighlighted = false
                        },
                        fromButtonClick = {
                            isFromHighlighted = false
                            fromButtonClick()
                        },
                        toButtonClick = toButtonClick,
                        onReverseButtonClick = {
                            onEvent(SavedTripUiEvent.ReverseStopClick)
                        },
                        onSearchButtonClick = { onSearchButtonClick() },
                    )
                }
            }
        }
        log(
            "[MAP_STOP_SEL] SavedTripsScreen dualPane=$dualPane " +
                "isTablet=$isTablet isPhoneLandscape=$isPhoneLandscape",
        )
        if (dualPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { log("[MAP_STOP_SEL] Row size=${it.width}x${it.height}") },
            ) {
                Box(
                    modifier = Modifier
                        .width(SAVED_TRIPS_PANE_MAX_WIDTH)
                        .fillMaxHeight()
                        .onSizeChanged { log("[MAP_STOP_SEL] leftPane size=${it.width}x${it.height}") },
                ) {
                    body()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content = rightPane,
                )
            }
        } else {
            body()
        }
    }
}

/**
 * Bottom action pill rendered in place of SearchStopRow when isCompactHeight (phone
 * landscape). State-aware label + action — each tap navigates rather than expanding
 * inline, which saves vertical room. See docs/TABLET_FOLDABLE_UX.md §5.
 */
@Composable
private fun SavedTripsCompactHeightActionPill(
    fromStop: StopItem?,
    toStop: StopItem?,
    onPlanTrip: () -> Unit,
    onSetDestination: () -> Unit,
    onFindTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val (label, action) = when {
        fromStop == null -> "Plan a trip" to onPlanTrip
        toStop == null -> "Set destination" to onSetDestination
        else -> "See timetable" to onFindTrip
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = dim.spacingXL,
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = action,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Text(text = label)
        }
    }
}

private fun LazyListScope.infoTiles(
    infoTiles: ImmutableList<InfoTileData>,
    onCtaClick: (InfoTileData) -> Unit,
    onDismissClick: (InfoTileData) -> Unit,
    onTileExpand: (InfoTileData) -> Unit,
) {
    items(
        items = infoTiles,
        key = { item -> item.key },
    ) { tileData ->
        var visible by remember { mutableStateOf(true) }
        AnimatedVisibility(
            visible = visible,
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ),
        ) {
            val dim = KrailTheme.dimensions
            InfoTile(
                infoTileData = tileData,
                onCtaClick = onCtaClick,
                onDismissClick = {
                    onDismissClick(tileData)
                },
                onTileExpand = onTileExpand,
                modifier = Modifier
                    .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM),
            )
        }
    }
}

private fun LazyListScope.savedTripsContent(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    iconColor: Color,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    expandedMap: SnapshotStateMap<String, Boolean>,
    departureBoardEntries: ImmutableList<StopDepartureBoardEntry>,
    expandedDepartureBoardStopId: String?,
    onDepartureBoardEvent: (DepartureBoardUiEvent) -> Unit = {},
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    onEnterEditing: () -> Unit,
) {
    if (trackedJourney != null) {
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

    stickyHeader(key = "saved_trips_title") {
        SavedTripsTitle {
            Text(text = "Saved Trips")
        }
    }

    item(key = "onboarding_reorder_tip") {
        val dim = KrailTheme.dimensions
        AnimatedVisibility(
            visible = !savedTripsState.hasSeenReorderTip && !editing,
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

    items(
        items = savedTripsState.savedTrips,
        key = { trip -> trip.tripId },
    ) { trip ->
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
                        fromDisplay = trip.fromStopDisplay(savedTripsState.stopLabels),
                        toDisplay = trip.toStopDisplay(savedTripsState.stopLabels),
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
                            onClick = { onEvent(SavedTripUiEvent.DeleteSavedTrip(trip)) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dim.spacingXL))
            }
        }
    }

    if (savedTripsState.parkRideUiState.isNotEmpty()) {
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

    if (departureBoardEntries.isNotEmpty()) {
        stickyHeader(key = "departure_board_title") {
            SavedTripsTitle {
                Text(text = "Departure Board")
            }
        }

        departureBoardEntries.forEach { entry ->
            departureBoardAccordionSection(
                entry = entry,
                isExpanded = expandedDepartureBoardStopId == entry.stopId,
                iconColor = iconColor,
                onEvent = onDepartureBoardEvent,
            )
        }

        item(key = "departure_board_bottom_spacer") {
            val dim = KrailTheme.dimensions
            Spacer(modifier = Modifier.height(dim.pageSectionGap))
        }
    }
}

@Composable
private fun TripDeleteOverlay(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .offset(x = dim.spacingS, y = -dim.spacingS)
            .size(dim.spacingXXXL)
            .clip(CircleShape)
            .background(KrailTheme.colors.onSurface, CircleShape)
            .klickable(onClick = onClick),
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

@Composable
private fun rememberWiggleRotation(active: Boolean, seed: Int): Float {
    val transition = rememberInfiniteTransition(label = "trip-wiggle")
    val seedAbs = kotlin.math.abs(seed)
    val angle by transition.animateFloat(
        initialValue = -WIGGLE_ANGLE_DEGREES,
        targetValue = WIGGLE_ANGLE_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WIGGLE_BASE_DURATION_MS +
                    (seedAbs % WIGGLE_DURATION_VARIANCE_BUCKETS) * WIGGLE_DURATION_VARIANCE_STEP_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = (seedAbs * WIGGLE_OFFSET_MULTIPLIER) % WIGGLE_OFFSET_MAX_MS,
                offsetType = StartOffsetType.Delay,
            ),
        ),
        label = "trip-wiggle-rotation",
    )
    return if (active) angle else 0f
}

@Composable
private fun SavedTripsTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = KrailTheme.colors.surface)
            .padding(vertical = dim.spacingXL, horizontal = dim.spacingXL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.titleMedium) {
            content()
        }
    }
}

private const val WIGGLE_ANGLE_DEGREES = 0.75f
private const val WIGGLE_BASE_DURATION_MS = 350
private const val WIGGLE_DURATION_VARIANCE_BUCKETS = 4
private const val WIGGLE_DURATION_VARIANCE_STEP_MS = 70
private const val WIGGLE_OFFSET_MULTIPLIER = 47
private const val WIGGLE_OFFSET_MAX_MS = 200

// Left-pane width cap when SavedTrips is rendered in dual-pane (≥ 600 dp width).
// Keeps the saved-trips list at phone-width proportions; map fills the rest.
// See docs/TABLET_FOLDABLE_UX.md §4.
private val SAVED_TRIPS_PANE_MAX_WIDTH = 480.dp

// region Previews

@Preview(name = "1. Empty - no saved trips")
@Composable
private fun PreviewSavedTripsScreen_Empty() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                isDiscoverAvailable = true,
            ),
        )
    }
}

@Preview(name = "2. With saved trips")
@Composable
private fun PreviewSavedTripsScreen_WithTrips() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                isDiscoverAvailable = false,
            ),
        )
    }
}

@Preview(name = "3. Search row expanded")
@Composable
private fun PreviewSavedTripsScreen_SearchExpanded() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                toStop = StopItem(stopId = "2000001", stopName = "Central Station"),
            ),
        )
    }
}

// endregion
