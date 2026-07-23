@file:Suppress("LongMethod", "LongParameterList")

package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.ui.components.TrackingCard
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

private const val LAZY_COLUMN_BOTTOM_PADDING = 300

@OptIn(ExperimentalComposeUiApi::class)
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
    onAddParkRideClick: () -> Unit = {},
    // Slot for the dual-pane right side. SavedTrips knows nothing about its content —
    // entry passes a MapStopSelectionPane (or anything else). Empty slot = blank pane.
    rightPane: @Composable BoxScope.() -> Unit = {},
) {
    // Adaptive search-row presentation:
    //   - Phone portrait, tablet, foldable-unfolded: SearchStopRow is ALWAYS expanded —
    //     these form factors have the vertical room, so the full search affordance stays
    //     front-and-centre.
    //   - Phone landscape (compact height): vertical space is scarce, so the row collapses
    //     to a "Plan a trip" button. Tapping it expands the full SearchStopRow; the
    //     collapse handle returns to the button.
    // See docs/TABLET_FOLDABLE_UX.md §4.
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
    val dualPane = adaptiveLayoutInfo.shouldShowDualPane
    val isPhoneLandscape = adaptiveLayoutInfo.isPhoneLandscape
    val isTablet = adaptiveLayoutInfo.isTablet

    // Collapse to the button only in phone-landscape; everything else stays expanded.
    val showPill = isPhoneLandscape

    // Search row expand / from-highlight state — rememberSaveable survives rotation.
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var isFromHighlighted by rememberSaveable { mutableStateOf(false) }

    // When the pill condition isn't met, always show the expanded row.
    val effectiveIsExpanded = if (showPill) isSearchExpanded else true

    // Edit mode for trip cards — long-press enters, Done button exits.
    var editing by rememberSaveable { mutableStateOf(false) }

    // While editing, the system back gesture exits edit mode instead of the app.
    BackHandler(enabled = editing) { editing = false }

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
                        SavedTripsTitleBarActions(
                            editing = editing,
                            isDiscoverAvailable = savedTripsState.isDiscoverAvailable,
                            displayDiscoverBadge = savedTripsState.displayDiscoverBadge,
                            onDoneClick = { editing = false },
                            onDiscoverButtonClick = onDiscoverButtonClick,
                            onSettingsButtonClick = onSettingsButtonClick,
                        )
                    },
                )

                val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = LAZY_COLUMN_BOTTOM_PADDING.dp),
                ) {
                    savedTripsListBody(
                        savedTripsState = savedTripsState,
                        trackedJourney = trackedJourney,
                        onEvent = onEvent,
                        onSavedTripCardClick = onSavedTripCardClick,
                        onTrackingCardClick = onTrackingCardClick,
                        onStopTracking = onStopTracking,
                        expandedMap = expandedMap,
                        editing = editing,
                        reorderState = reorderState,
                        onEnterEditing = { editing = true },
                        onAddParkRideClick = onAddParkRideClick,
                    )
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
            SavedTripsBottomSearchRow(
                visible = !savedTripsState.isSavedTripsLoading && !editing,
                fromStopItem = savedTripsState.fromStop,
                toStopItem = savedTripsState.toStop,
                isExpanded = effectiveIsExpanded,
                isFromHighlighted = isFromHighlighted,
                showPill = showPill,
                onExpandRequest = {
                    isSearchExpanded = true
                    isFromHighlighted = false
                },
                onCollapseRequest = {
                    isSearchExpanded = false
                    isFromHighlighted = false
                },
                fromButtonClick = {
                    isFromHighlighted = false
                    fromButtonClick()
                },
                toButtonClick = toButtonClick,
                onReverseButtonClick = { onEvent(SavedTripUiEvent.ReverseStopClick) },
                onSearchButtonClick = { onSearchButtonClick() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        log(
            "[MAP_STOP_SEL] SavedTripsScreen dualPane=$dualPane " +
                "isTablet=$isTablet isPhoneLandscape=$isPhoneLandscape",
        )
        if (dualPane) {
            // Shared dual-pane split — same component SearchStop uses, so the two screens'
            // two-pane layouts can't drift. Right pane (map) is a sibling of the list, never
            // nested under it; see DualPaneScaffold for the iOS compositing invariant.
            DualPaneScaffold(
                listPane = { body() },
                rightPane = rightPane,
            )
        } else {
            body()
        }
    }
}

internal fun LazyListScope.infoTiles(
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

internal fun LazyListScope.savedTripsContent(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
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
        SavedTripItem(
            trip = trip,
            stopLabels = savedTripsState.stopLabels,
            editing = editing,
            reorderState = reorderState,
            onSavedTripCardClick = onSavedTripCardClick,
            onEnterEditing = onEnterEditing,
            onDelete = { onEvent(SavedTripUiEvent.DeleteSavedTrip(trip)) },
        )
    }
}

@Composable
internal fun TripDeleteOverlay(
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
internal fun rememberWiggleRotation(active: Boolean, seed: Int): Float {
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
internal fun SavedTripsTitle(
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
