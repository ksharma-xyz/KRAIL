@file:Suppress("LongMethod", "LongParameterList")

package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ai.AskKrailScreen
import xyz.ksharma.krail.trip.planner.ui.components.ai.rememberAiGreeting
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.parkRideExpansionSaver
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.isWayInAvailable
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

// Clear of the bottom search row and the chip row above it, so the last card scrolls fully into view.
private const val LAZY_COLUMN_BOTTOM_PADDING = 400

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
    aiState: AiSearchInputUiState = AiSearchInputUiState(),
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    // Read while drawing, not collected: it changes many times a second while a rider speaks.
    aiVoiceLevel: () -> Float = { 0f },
    onSettingsButtonClick: () -> Unit = {},
    onDiscoverButtonClick: () -> Unit = {},
    onEvent: (SavedTripUiEvent) -> Unit = {},
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onAddParkRideClick: () -> Unit = {},
    onLabelChipClick: (labelKey: String) -> Unit = {},
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

    // The Home, Work and Park & Ride chips above the search row (docs/MAP_HOME_UX.md rule 41).
    val homeChips = remember(savedTripsState.stopLabels) { homeChipsFor(savedTripsState.stopLabels) }

    // Reorder mode (docs/MAP_HOME_UX.md rule 31). The session lives in the ViewModel, so a rotation
    // keeps it, Done writes it and leaving the screen drops it. A long press on a card is the one way in.
    val editing = savedTripsState.reorder != null

    // Blur, fallback dim and the row's one-shot handoff spin, owned by rememberAskKrailFocus
    // so this function stays under the complexity ceiling and the three concerns stay one
    // mechanism. See AskKrailFocus.kt for the rules.
    val askKrailFocus = rememberAskKrailFocus(aiState)

    // While reordering, the system back and the swipe back cancel it instead of leaving the app. Nothing
    // is saved and the cards animate back to the order they were in.
    BackHandler(enabled = editing) { onEvent(SavedTripUiEvent.ReorderCancel) }

    // Which Park & Ride cards are open.
    //
    // Saveable, not remembered: the ViewModel's matching refresh set survives recreation, so
    // losing this half leaves the two out of step and the next tap reading as "expand" on a
    // card that is already expanded. See parkRideExpansionSaver.
    //
    // Declared here rather than beside the list it feeds, because `body` below is called from
    // two places — inside DualPaneScaffold and directly — which are two different composition
    // slots. State remembered inside it is dropped the moment a rotation switches which of the
    // two is used, no matter how saveable it is.
    val expandedMap = rememberSaveable(saver = parkRideExpansionSaver()) {
        mutableStateMapOf<String, Boolean>()
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        onEvent(SavedTripUiEvent.ReorderMove(fromKey = fromKey, toKey = toKey))
    }

    Box(modifier = modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_SCREEN).fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Everything behind the Ask KRAIL dialog softens out of focus while it is up
                // and comes back as it leaves; see rememberAskKrailFocus.
                .blur(askKrailFocus.blurRadius)
                // The search row is pinned to the bottom of this box and its AI box is a real text
                // field, so the layout area has to end where the keyboard starts — same root-level
                // imePadding SearchStopScreen uses. Padding the row itself instead only stretches
                // its coloured background downwards.
                //
                // Deliberately NOT applied to the AI surface, which is a sibling below rather than
                // a child: that surface covers the whole screen and handles the keyboard on the one
                // control that needs it. Inheriting this inset shrank it by a whole keyboard.
                .imePadding()
                .background(color = KrailTheme.colors.surface)
                .onSizeChanged { log("[MAP_STOP_SEL] outerBox size=${it.width}x${it.height}") },
        ) {
            val body: @Composable BoxScope.() -> Unit = {
                // Keep the list clear of anything on the sides: a camera notch in landscape, or
                // iPhone Duo's outer display, where the status bar and camera sit on the right
                // edge. displayCutout is not enough there: Compose on iOS reports the cutout on
                // the top edge only in portrait. The keyboard stays with the root's imePadding.
                Column(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    ),
                ) {
                    if (editing) {
                        // The chrome is gone while reordering: no logo, no gear. One dark Done.
                        ReorderTopRow(onDone = { onEvent(SavedTripUiEvent.ReorderDone) })
                    } else {
                        TitleBar(
                            title = {
                                // The wordmark is the brand, not a label: nobody reads it to use
                                // the app, so it carries no contrast obligation and must not be
                                // adapted. Ink pulled it away from the rider's chosen colour on
                                // the themes that needed adapting, which made the one element
                                // that should be exactly their colour the one element that wasn't.
                                Text(text = "KRAIL", color = themeDecorColor())
                            },
                            actions = {
                                SavedTripsTitleBarActions(
                                    isDiscoverAvailable = savedTripsState.isDiscoverAvailable,
                                    displayDiscoverBadge = savedTripsState.displayDiscoverBadge,
                                    onDiscoverButtonClick = onDiscoverButtonClick,
                                    onSettingsButtonClick = onSettingsButtonClick,
                                )
                            },
                        )
                    }

                    // One column, at most 560 dp wide and centred (MAP_HOME_UX rule 45). The
                    // list stays the full width of the pane so a swipe in the gutter still
                    // scrolls it; the gutter is content padding, so the trips, the headings and
                    // Park & Ride all sit on the same two edges. The search row below takes the
                    // same edges from savedTripsColumn().
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val gutter = columnGutterFor(maxWidth)
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_LIST),
                            contentPadding = PaddingValues(
                                start = gutter,
                                end = gutter,
                                // The search row and the chips are gone while reordering, so the stack
                                // takes the height they held.
                                bottom = if (editing) {
                                    KrailTheme.dimensions.spacingXL
                                } else {
                                    LAZY_COLUMN_BOTTOM_PADDING.dp
                                },
                            ),
                        ) {
                            savedTripsListBody(
                                savedTripsState = savedTripsState,
                                trackedJourney = trackedJourney,
                                onEvent = onEvent,
                                onSavedTripCardClick = onSavedTripCardClick,
                                onTrackingCardClick = onTrackingCardClick,
                                onStopTracking = onStopTracking,
                                expandedMap = expandedMap,
                                reorderState = reorderState,
                            )
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
                    onSearchButtonClick = { onSearchButtonClick() },
                    onReverseButtonClick = { onEvent(SavedTripUiEvent.ReverseStopClick) },
                    onAiEvent = onAiEvent,
                    isAiSearchAvailable = aiState.isWayInAvailable,
                    isAiHandoffSettling = askKrailFocus.handoffSpin,
                    dateTimeSelectionText = savedTripsState.dateTimeSelectionItem?.toDateTimeText(),
                    onDateTimeSelectionClear = { onEvent(SavedTripUiEvent.DateTimeSelectionChanged(null)) },
                    homeChips = homeChips,
                    onLabelChipClick = onLabelChipClick,
                    onParkRideChipClick = onAddParkRideClick,
                    modifier = Modifier.align(Alignment.BottomCenter).savedTripsColumn(),
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
                    // A touch on the map pane while reordering cancels it: the rider has turned to the
                    // map, and a touch there is never a drag of a card. The pane stays a sibling of the
                    // list and this wrapper is no graphics layer, so the iOS compositing rule holds.
                    rightPane = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onFirstTouch(enabled = editing) { onEvent(SavedTripUiEvent.ReorderCancel) },
                        ) { rightPane() }
                    },
                )
            } else {
                body()
            }
        }

        // The blur's stand-in where it cannot draw: a plain dim between the content and the
        // dialog. Sequenced here so it covers everything the blur would have softened.
        if (askKrailFocus.fallbackDimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KrailTheme.colors.scrim.copy(alpha = askKrailFocus.fallbackDimAlpha)),
            )
        }

        // A dialog over the screen rather than a mode inside the row: what a rider does here
        // needs more room than two text fields. Outside the ime-padded box on purpose, so it
        // decides for itself which part of it moves for the keyboard. When it resolves a trip
        // it closes itself onto this screen's own row (see closeAfterHandoff in the ViewModel)
        // — the row is where the answer lands, and there is no second confirm surface.
        if (aiState.isInputOpen) {
            // Built here because this screen already holds the rider's labels and saved trips.
            // Nothing new is read, nothing is stored, and the AI ViewModel keeps knowing nothing
            // about greetings.
            val greeting = rememberAiGreeting(
                stopLabels = savedTripsState.stopLabels,
                savedTrips = savedTripsState.savedTrips,
            )
            AskKrailScreen(
                state = aiState,
                suggestion = greeting.suggestion,
                onEvent = onAiEvent,
                onDismiss = { onAiEvent(AiSearchInputEvent.CloseInput) },
                voiceLevel = aiVoiceLevel,
            )
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

/**
 * The delete circle on a trip card in reorder mode (`docs/MAP_HOME_UX.md` rule 38): a 24 dp circle in
 * the ink colour with a 12 dp X, at the card's top right corner and overhanging it by 6 dp. Its touch
 * target is 48 dp, centred on the circle, so the drawn circle stays small and the tap is not.
 */
@Composable
internal fun TripDeleteOverlay(
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
