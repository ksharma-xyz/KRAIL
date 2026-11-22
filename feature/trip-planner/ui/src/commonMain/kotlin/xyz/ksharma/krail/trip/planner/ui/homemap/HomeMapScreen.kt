package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.cornerSafeTopPadding
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ai.AskKrailScreen
import xyz.ksharma.krail.trip.planner.ui.components.ai.rememberAiGreeting
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.parkRideExpansionSaver
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionSlot
import xyz.ksharma.krail.trip.planner.ui.savedtrips.HomeChipRow
import xyz.ksharma.krail.trip.planner.ui.savedtrips.HomeSearchPill
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ReorderTopRow
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsTitleBarActions
import xyz.ksharma.krail.trip.planner.ui.savedtrips.columnGutterFor
import xyz.ksharma.krail.trip.planner.ui.savedtrips.homeChipsFor
import xyz.ksharma.krail.trip.planner.ui.savedtrips.homeStackListBody
import xyz.ksharma.krail.trip.planner.ui.savedtrips.onFirstTouch
import xyz.ksharma.krail.trip.planner.ui.savedtrips.rememberAskKrailFocus
import xyz.ksharma.krail.trip.planner.ui.savedtrips.savedTripsColumn
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.isWayInAvailable
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState

/**
 * Where an open pin card is told to float: below the top row, and a place to report how tall it is so the
 * card stack can stop short of it.
 */
@Immutable
internal class HomeCardPlacement(val topReserve: Dp, val onHeight: (Dp) -> Unit)

/**
 * What the entry fills the map home with: the one map, the notices (drawn above the search pill) and what
 * the status pill does, and the card an open pin shows. Held as slots so the screen knows nothing of the map,
 * of the permission or of the pins.
 *
 * @property map the one map, given the room its controls must keep clear of at the top.
 * @property card the floating card of the open pin, or null with none open.
 */
@Immutable
internal class HomeMapHost(
    val map: @Composable BoxScope.(ornamentTopPadding: Dp) -> Unit,
    val notices: ImmutableList<HomeNotice>,
    val locationPill: LocationPill,
    val onLocationClick: () -> Unit,
    val card: (@Composable BoxScope.(HomeCardPlacement) -> Unit)?,
    val onCardClose: () -> Unit,
)

/**
 * The home screen with the map behind it, `docs/MAP_HOME_UX.md` rules 1, 6, 12, 20 and 24 and the Home structure
 * table. The map is the screen and everything else floats on it: the top row, the card stack, the chips, the
 * notices and the one search pill. There is no bottom sheet, no docked panel and no scrim, and no second bar:
 * the pill is the only search field home ever shows.
 *
 * **The map is a sibling.** On iOS a MapLibre `UIKitView` cannot composite into an offscreen layer, so the map
 * is never a child of anything with a gradient or a blur. The blur Ask KRAIL pulls over the screen sits on the
 * cards, not above the map, and a wide window hands the map to [DualPaneScaffold] as its right pane, the one
 * map the dual pane always drew, never a second one.
 *
 * **Time to content never waits for the map.** The cards, the chips and the pill draw from local state. The map
 * is its own layer and adds nothing the cards read.
 *
 * Reorder mode, the park ride row, the label chips and Ask KRAIL behave as on the classic home. This is only
 * chosen when the maps flag is on, so a build without it keeps the classic home.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomeMapScreen(
    savedTripsState: SavedTripsState,
    host: HomeMapHost,
    modifier: Modifier = Modifier,
    trackedJourney: TrackedJourney? = null,
    aiState: AiSearchInputUiState = AiSearchInputUiState(),
    onAiEvent: (AiSearchInputEvent) -> Unit = {},
    // Read while drawing, not collected: it changes many times a second while a rider speaks.
    aiVoiceLevel: () -> Float = { 0f },
    actions: HomeMapActions = HomeMapActions(),
    // The one-time Park & Ride suggestion, or null with none to show. It takes the row's place in the stack.
    suggestion: ParkRideSuggestionSlot? = null,
) {
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
    val dualPane = adaptiveLayoutInfo.shouldShowDualPane
    val editing = savedTripsState.reorder != null
    val cardOpen = host.card != null
    val askKrailFocus = rememberAskKrailFocus(aiState)

    // Reorder mode and an open card each take the system back first, and the card closes before the screen leaves.
    BackHandler(enabled = editing) { actions.onEvent(SavedTripUiEvent.ReorderCancel) }
    BackHandler(enabled = cardOpen && !editing) { host.onCardClose() }

    var topRowHeight by remember { mutableStateOf(0.dp) }
    var bottomHeight by remember { mutableStateOf(0.dp) }
    var cardHeight by remember { mutableStateOf(0.dp) }
    val navigationBar = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    // A card on a wide window floats over the map beside the list and never over it, so only a single pane
    // card takes the place of the chips and the pill and holds the stack up.
    val cardCoversList = cardOpen && !dualPane
    val bottom = homeBottomFor(
        loading = savedTripsState.isSavedTripsLoading,
        reordering = editing,
        cardOpen = cardCoversList,
    )
    val reserve = stackBottomReserve(
        bottom = bottom,
        blockHeight = bottomHeight,
        cardHeight = cardHeight,
        cardEdge = KrailTheme.dimensions.spacingXL + navigationBar,
    )

    val stack = rememberHomeStack(actions.onEvent, suggestion)
    val chrome = HomeChrome(
        topRowHeight = { topRowHeight = it },
        bottomHeight = { bottomHeight = it },
        stackReserve = reserve,
        chipsShown = !adaptiveLayoutInfo.isPhoneLandscape,
        bottom = bottom,
    )
    val cardPlacement = HomeCardPlacement(topReserve = topRowHeight, onHeight = { cardHeight = it })

    Box(modifier = modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_SCREEN).fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(color = KrailTheme.colors.surface)) {
            val cards: @Composable BoxScope.() -> Unit = {
                HomeCardsLayer(
                    savedTripsState = savedTripsState,
                    host = host,
                    trackedJourney = trackedJourney,
                    stack = stack,
                    chrome = chrome,
                    actions = actions,
                    aiState = aiState,
                    onAiEvent = onAiEvent,
                    blurRadius = askKrailFocus.blurRadius,
                )
            }
            if (dualPane) {
                // The map is the right pane, a sibling of the list and of anything that blurs it. A touch on
                // it while reordering cancels the mode, as it does on the classic home.
                DualPaneScaffold(
                    listPane = { cards() },
                    rightPane = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onFirstTouch(enabled = editing) { actions.onEvent(SavedTripUiEvent.ReorderCancel) },
                        ) {
                            host.map(this, cornerSafeTopPadding())
                            host.card?.invoke(this, cardPlacement)
                        }
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) { host.map(this, topRowHeight) }
                cards()
                host.card?.invoke(this, cardPlacement)
            }
        }

        // The blur's stand-in where it cannot draw: a plain dim between the content and the dialog.
        if (askKrailFocus.fallbackDimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KrailTheme.colors.scrim.copy(alpha = askKrailFocus.fallbackDimAlpha)),
            )
        }

        // The Ask KRAIL dialog, over the screen. Built here because this screen already holds the rider's labels
        // and saved trips, so nothing new is read or stored.
        if (aiState.isInputOpen) {
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

/** The measurements the screen takes from its layers, and what it has decided about them. */
private class HomeChrome(
    val topRowHeight: (Dp) -> Unit,
    val bottomHeight: (Dp) -> Unit,
    val stackReserve: Dp,
    val chipsShown: Boolean,
    val bottom: HomeBottom,
)

/** The list and reorder state the stack of cards needs, held above the branch that changes composition. */
private class HomeStack(
    val lazyListState: LazyListState,
    val reorderState: ReorderableLazyListState,
    val expandedMap: SnapshotStateMap<String, Boolean>,
    val suggestion: ParkRideSuggestionSlot?,
)

@Composable
private fun rememberHomeStack(onEvent: (SavedTripUiEvent) -> Unit, suggestion: ParkRideSuggestionSlot?): HomeStack {
    // Saveable, not remembered: the ViewModel's matching refresh set survives recreation, so losing this half
    // leaves the two out of step. See parkRideExpansionSaver. Declared above the dual pane branch, because the
    // cards are called from two places that are two different composition slots.
    val expandedMap = rememberSaveable(saver = parkRideExpansionSaver()) { mutableStateMapOf<String, Boolean>() }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        onEvent(SavedTripUiEvent.ReorderMove(fromKey = fromKey, toKey = toKey))
    }
    return HomeStack(lazyListState, reorderState, expandedMap, suggestion)
}

/**
 * The cards, the top row, the chips and the pill: everything that floats over the map. The blur Ask KRAIL
 * pulls and the keyboard inset sit here, on the cards, and never above the map.
 */
@Composable
private fun BoxScope.HomeCardsLayer(
    savedTripsState: SavedTripsState,
    host: HomeMapHost,
    trackedJourney: TrackedJourney?,
    stack: HomeStack,
    chrome: HomeChrome,
    actions: HomeMapActions,
    aiState: AiSearchInputUiState,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    blurRadius: Dp,
) {
    val editing = savedTripsState.reorder != null
    Box(modifier = Modifier.fillMaxSize().imePadding().blur(blurRadius)) {
        // Keep everything clear of anything on the sides: a camera notch in landscape, or iPhone Duo's outer
        // display, where the status bar and camera sit on the right edge.
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            if (editing) {
                // The chrome is gone while reordering: no logo, no gear, no notices. One dark Done.
                ReorderTopRow(onDone = { actions.onEvent(SavedTripUiEvent.ReorderDone) })
            } else {
                HomeTopRow(
                    savedTripsState = savedTripsState,
                    actions = actions,
                    onHeight = chrome.topRowHeight,
                )
            }
            HomeCardStack(
                savedTripsState = savedTripsState,
                trackedJourney = trackedJourney,
                stack = stack,
                reserve = chrome.stackReserve,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
        }

        HomeBottomBlock(
            savedTripsState = savedTripsState,
            host = host,
            chrome = chrome,
            actions = actions,
            aiState = aiState,
            onAiEvent = onAiEvent,
        )
    }
}

/**
 * The top row, rules 12 and 20: the KRAIL logo at the left, Discover beside Settings at the right, and nothing
 * else (the notices are in the bottom group, rule 24). The buttons are the title bar's own, solid and the same
 * on both platforms. The card stack never rises into it because it is laid out below it, in the same column.
 */
@Composable
private fun HomeTopRow(
    savedTripsState: SavedTripsState,
    actions: HomeMapActions,
    onHeight: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .testTag(TripPlannerTestTags.HOME_MAP_TOP_ROW)
            .onSizeChanged { onHeight(with(density) { it.height.toDp() }) },
    ) {
        TitleBar(
            title = {
                // The wordmark is the brand, not a label: it carries no contrast obligation and must not be
                // adapted, so it is the rider's colour exactly. It sits on the same surface the Discover and
                // Settings buttons beside it have, in taj's text field button with no click (the way taj draws a
                // read only field at full strength), as wide as the wordmark and 48 dp tall like them.
                TextFieldButton(
                    onClick = null,
                    modifier = Modifier.width(IntrinsicSize.Max).testTag(TripPlannerTestTags.HOME_MAP_LOGO),
                ) {
                    CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.headlineMedium) {
                        Text(text = "KRAIL", color = themeDecorColor())
                    }
                }
            },
            actions = {
                SavedTripsTitleBarActions(
                    // Reorder mode swaps this whole bar for its own top row, so the title bar never shows Done.
                    editing = false,
                    isDiscoverAvailable = savedTripsState.isDiscoverAvailable,
                    displayDiscoverBadge = savedTripsState.displayDiscoverBadge,
                    onDoneClick = {},
                    onDiscoverButtonClick = actions.onDiscoverButtonClick,
                    onSettingsButtonClick = actions.onSettingsButtonClick,
                )
            },
        )
    }
}

/**
 * The card stack, in the one column of rule 45. It takes only the height its cards need, up to what is left
 * below the top row and above the bottom, so a short stack leaves the map under it free to touch and the map
 * shows around and between the cards. Pins under a card are not reachable, so nothing a rider must reach is there.
 */
@Composable
private fun HomeCardStack(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    stack: HomeStack,
    reserve: Dp,
    actions: HomeMapActions,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = stack.lazyListState,
            modifier = Modifier
                .testTag(TripPlannerTestTags.SAVED_TRIPS_LIST)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .heightIn(max = stackMaxHeight(available = maxHeight, reserve = reserve)),
            contentPadding = PaddingValues(
                start = columnGutterFor(maxWidth),
                end = columnGutterFor(maxWidth),
                bottom = dim.spacingXL,
            ),
        ) {
            homeStackListBody(
                savedTripsState = savedTripsState,
                trackedJourney = trackedJourney,
                onEvent = actions.onEvent,
                onSavedTripCardClick = actions.onSavedTripCardClick,
                onTrackingCardClick = actions.onTrackingCardClick,
                onStopTracking = actions.onStopTracking,
                expandedMap = stack.expandedMap,
                reorderState = stack.reorderState,
                suggestion = stack.suggestion,
            )
        }
    }
}

/**
 * The bottom group, in the thumb's reach and on the cards' edges, 16 dp above the navigation bar. From the
 * bottom: the one search pill, then above it one flow of whole items, the chips and the notices (the location
 * status and the Map offline status), then the cards. The chips, the notices and the pill share one left and
 * one right edge, the page padding, and one gap, [KrailDimensions.spacingM], separates the rows. The flow wraps
 * whole items when a line is full. The group grows upward and the card stack stops short of all of it. There is
 * never a second search row: a pick, a pin card and an Ask KRAIL answer all open Search Stop, which holds the To
 * and From form.
 */
@Composable
private fun BoxScope.HomeBottomBlock(
    savedTripsState: SavedTripsState,
    host: HomeMapHost,
    chrome: HomeChrome,
    actions: HomeMapActions,
    aiState: AiSearchInputUiState,
    onAiEvent: (AiSearchInputEvent) -> Unit,
) {
    if (chrome.bottom != HomeBottom.Pill) return
    val density = LocalDensity.current
    val dim = KrailTheme.dimensions
    val chips = if (chrome.chipsShown) homeChipsFor(savedTripsState.stopLabels) else persistentListOf()
    val notices = if (noticesShownFor(chrome.bottom)) host.notices else persistentListOf()
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .savedTripsColumn()
            .testTag(TripPlannerTestTags.HOME_MAP_BOTTOM)
            // Measured with the navigation bar and the 16 dp under it, so the stack stops short of all of it.
            .onSizeChanged { chrome.bottomHeight(with(density) { it.height.toDp() }) }
            .navigationBarsPadding()
            .padding(bottom = dim.spacingXL)
            .padding(horizontal = dim.pageHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        if (chips.isNotEmpty() || notices.isNotEmpty()) {
            HomeChipRow(
                chips = chips,
                onLabelClick = actions.onLabelChipClick,
                onParkRideClick = actions.onAddParkRideClick,
                floating = true,
            ) {
                HomeNotices(notices = notices, pill = host.locationPill, onLocationClick = host.onLocationClick)
            }
        }
        HomeSearchPill(
            onClick = actions.toButtonClick,
            isAiAvailable = aiState.isWayInAvailable,
            onAiEvent = onAiEvent,
        )
    }
}
