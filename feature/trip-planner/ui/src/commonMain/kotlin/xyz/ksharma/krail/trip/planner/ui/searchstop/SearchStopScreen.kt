@file:Suppress("StringLiteralDuplication")

package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import krail.feature.trip_planner.ui.generated.resources.ic_map
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.adaptiveui.AdaptiveScreenContent
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.CloudGradientBackground
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.AddressSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.AssignNewLabelSheet
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItemState
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import krail.feature.trip_planner.ui.generated.resources.Res as TripPlannerRes

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun SearchStopScreen(
    searchStopState: SearchStopState,
    modifier: Modifier = Modifier,
    fieldType: SearchStopFieldType = SearchStopFieldType.FROM,
    searchQuery: String = "",
    goBack: () -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
    onEvent: (SearchStopUiEvent) -> Unit = {},
    dualPaneMapUiState: MapUiState? = null,
    onDualPaneMapEvent: (MapStopSelectionEvent) -> Unit = {},
    // True when opened from the timetable header to replace one leg of the
    // current trip — scopes the copy to "Change origin" / "Change destination".
    editTripLeg: Boolean = false,
    // Manage is a real nav destination (ManageStopLabelsRoute), not a sheet owned by
    // this screen — the caller (SearchStopEntry) wires this to actual navigation.
    onManageLabelsClick: () -> Unit = {},
) {
    SideEffect { log("[SEARCH_STOP_SCREEN] recomposed") }

    val themeColor by LocalThemeColor.current
    // rememberSaveable so text survives rotation and dark/light mode config changes.
    var textFieldText: String by rememberSaveable { mutableStateOf(searchQuery) }
    // Hoisted here so it survives any config change regardless of which pane is active.
    var showMap by rememberSaveable { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var backClicked by rememberSaveable { mutableStateOf(false) }

    // v4 inline assign (StopLabelAssignRow, see STOP_LABEL_UX_REDESIGN_PROPOSAL.md):
    // the stop currently creating a brand-new label via the "+ New label" pill. Assign
    // is otherwise instant/no-confirm from this row — Remove/Replace/Delete only exist
    // in Manage now, not on the search row.
    var newLabelTarget by rememberSaveable(stateSaver = NewLabelTargetSaver) {
        mutableStateOf<NewLabelTarget?>(null)
    }

    // Only one StopLabelAssignRow can be expanded (its "+ New label" wall open) at a
    // time, across recents/empty-state/search-results — keyed by stopId since that's
    // stable across all three lists.
    var expandedStopKey by rememberSaveable { mutableStateOf<String?>(null) }
    val onToggleExpandStop: (String) -> Unit = { stopId ->
        expandedStopKey = if (expandedStopKey == stopId) null else stopId
    }

    val placeholderText = searchFieldPlaceholder(
        fieldType = fieldType,
        editTripLeg = editTripLeg,
    )

    LaunchedEffect(backClicked) {
        if (backClicked) {
            goBack()
        }
    }

    // Same "close keyboard before navigating" order as the back button (SearchTopBar's
    // NavActionButton) — otherwise the keyboard stays up mid-navigation and dismisses
    // itself awkwardly once ManageStopLabelsScreen is already on screen.
    val effectiveOnManageLabelsClick: () -> Unit = {
        keyboard?.hide()
        focusRequester.freeFocus()
        onManageLabelsClick()
    }

    LaunchedEffect(textFieldText) {
        snapshotFlow { textFieldText.trim() }
            .distinctUntilChanged()
            .debounce(250)
            // allow blank queries to flow so ViewModel can switch back to Recents
            .mapLatest { text ->
                onEvent(SearchStopUiEvent.SearchTextChanged(text))
            }
            .collectLatest {}
    }

    // v4 inline assign handler, shared by every StopLabelAssignRow (search results,
    // recents, empty-state stops). The row's wall only ever shows unset labels, so
    // there's no conflict to classify here — every tap is a direct, no-confirm assign.
    val onLabelPillClick: (StopItem, StopLabel, LabelAssignSurface) -> Unit = { stopItem, label, surface ->
        onEvent(
            SearchStopUiEvent.AssignLabelStop(
                labelKey = label.label,
                stopItem = stopItem,
                surface = surface,
            ),
        )
    }
    val onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit = { stopItem, modes, surface ->
        newLabelTarget = NewLabelTarget(stop = stopItem, transportModeSet = modes, surface = surface)
    }

    AdaptiveScreenContent(
        singlePaneContent = {
            // Phone layout: Single pane with toggle between List and Map
            SearchStopScreenSinglePane(
                modifier = modifier,
                searchStopState = searchStopState,
                themeColor = themeColor,
                placeholderText = placeholderText,
                initialText = textFieldText,
                showMap = showMap,
                onShowMapChange = { showMap = it },
                focusRequester = focusRequester,
                keyboard = keyboard,
                onBackClick = { backClicked = true },
                onTextChange = { value: String ->
                    log("value: $value")
                    textFieldText = value
                },
                expandedStopKey = expandedStopKey,
                onToggleExpandStop = onToggleExpandStop,
                onStopSelect = onStopSelect,
                onLabelPillClick = onLabelPillClick,
                onNewLabelClick = onNewLabelClick,
                onManageClick = effectiveOnManageLabelsClick,
                onEvent = onEvent,
            )
        },
        dualPaneContent = {
            // Tablet/Foldable layout: List on left, Map on right
            SearchStopScreenDualPane(
                modifier = modifier,
                searchStopState = searchStopState,
                themeColor = themeColor,
                placeholderText = placeholderText,
                initialText = textFieldText,
                focusRequester = focusRequester,
                keyboard = keyboard,
                onBackClick = { backClicked = true },
                onTextChange = { value: String ->
                    log("value: $value")
                    textFieldText = value
                },
                expandedStopKey = expandedStopKey,
                onToggleExpandStop = onToggleExpandStop,
                onStopSelect = onStopSelect,
                onLabelPillClick = onLabelPillClick,
                onNewLabelClick = onNewLabelClick,
                onManageClick = effectiveOnManageLabelsClick,
                onEvent = onEvent,
                dualPaneMapUiState = dualPaneMapUiState,
                onDualPaneMapEvent = onDualPaneMapEvent,
            )
        },
    )

    val newLabelStop = newLabelTarget
    if (newLabelStop != null) {
        AssignNewLabelSheet(
            stopName = newLabelStop.stop.stopName,
            transportModeSet = newLabelStop.transportModeSet,
            existingLabelNames = searchStopState.stopLabels.map { it.label }.toImmutableList(),
            onDismiss = { newLabelTarget = null },
            onSave = { name ->
                newLabelTarget = null
                // AssignNewLabelSheet already ran name through normaliseLabelName before
                // calling onSave, so CreateLabel's stored key and this AssignLabelStop's
                // labelKey are guaranteed to match — see AssignNewLabelSheetContent.
                if (name.isNotBlank()) {
                    onEvent(
                        SearchStopUiEvent.CreateLabel(
                            name = name,
                            emoji = NEW_LABEL_DEFAULT_EMOJI,
                            surface = newLabelStop.surface,
                        ),
                    )
                    onEvent(
                        SearchStopUiEvent.AssignLabelStop(
                            labelKey = name,
                            stopItem = newLabelStop.stop,
                            surface = newLabelStop.surface,
                            isNewLabel = true,
                        ),
                    )
                }
            },
        )
    }
}

private const val NEW_LABEL_DEFAULT_EMOJI = "📍"

/** The stop (+ its transport modes, for the sheet's mode roundel) creating a brand-new label.
 * [surface] is the row kind whose "+ New label" chip opened the sheet. */
internal data class NewLabelTarget(
    val stop: StopItem,
    val transportModeSet: List<TransportMode>,
    val surface: LabelAssignSurface,
)

// region Savers — keep UI orchestration state alive across rotation / process death.
//
// Saver<T?, Any> shape (rather than mapSaver which requires `Original : Any`) so we can
// hold nullable MutableState backed by a Saver. Returning null from save tells the
// framework "nothing to persist"; restore is only called when there IS something saved.

/**
 * Search-field placeholder. Doubles as the screen's scope label: when [editTripLeg] is
 * true the search replaces one leg of the trip shown in the timetable, so the copy
 * switches to "Change origin" / "Change destination".
 */
private fun searchFieldPlaceholder(
    fieldType: SearchStopFieldType,
    editTripLeg: Boolean,
): String = when (fieldType) {
    SearchStopFieldType.FROM -> if (editTripLeg) "Change origin" else "Choose starting point"
    SearchStopFieldType.TO -> if (editTripLeg) "Change destination" else "Choose destination"
    SearchStopFieldType.LABEL -> "Choose a stop"
}

private val NewLabelTargetSaver: Saver<NewLabelTarget?, Any> = Saver(
    save = { target ->
        target?.let {
            mapOf<String, Any?>(
                "stopId" to it.stop.stopId,
                "stopName" to it.stop.stopName,
                "productClasses" to it.transportModeSet.map { mode -> mode.productClass },
                "surface" to it.surface.name,
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val map = saved as Map<String, Any?>
        NewLabelTarget(
            stop = StopItem(
                stopId = map["stopId"] as String,
                stopName = map["stopName"] as String,
            ),
            transportModeSet = (map["productClasses"] as List<Int>).mapNotNull { productClass ->
                TransportMode.fromProductClass(productClass)
            },
            surface = (map["surface"] as? String)
                ?.let { name -> LabelAssignSurface.entries.firstOrNull { e -> e.name == name } }
                ?: LabelAssignSurface.SEARCH_RESULT,
        )
    },
)

// endregion

/**
 * Single-pane layout for phones.
 * Shows either list OR map based on local UI state with a toggle button.
 */
@Composable
private fun SearchStopScreenSinglePane(
    searchStopState: SearchStopState,
    themeColor: String,
    placeholderText: String,
    initialText: String,
    showMap: Boolean,
    onShowMapChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel, LabelAssignSurface) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit,
    onManageClick: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // null = permission check not done yet (button hidden); true/false set once check completes.
    // Keeping it null until then prevents the button from flashing in before we know
    // whether to animate it, which would skip the slide-in entirely.
    var animateMapButton: Boolean? by remember { mutableStateOf(null) }
    val userLocationManager = rememberUserLocationManager()
    LaunchedEffect(searchStopState.isMapsAvailable) {
        if (searchStopState.isMapsAvailable) {
            val status = userLocationManager.checkPermissionStatus()
            log("permission status : $status")
            if (status is PermissionStatus.NotDetermined) {
                delay(500)
                animateMapButton = true
            } else {
                animateMapButton = false
            }
        }
    }

    // Capture TopBar height after layout so map ornaments (compass, scale bar) and list
    // content both start below the floating TopBar.
    var topBarHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val topBarHeightDp by remember { derivedStateOf { with(density) { topBarHeightPx.toDp() } } }

    MapAutoInitEffect(
        showMap = showMap,
        isMapsAvailable = searchStopState.isMapsAvailable,
        mapUiState = searchStopState.mapUiState,
        onEvent = onEvent,
    )

    // Hide/show keyboard based on map toggle
    LaunchedEffect(showMap) {
        if (showMap) {
            keyboard?.hide()
            focusRequester.freeFocus()
        } else {
            keyboard?.show()
            focusRequester.requestFocus()
        }
    }

    // Force dark status bar icons when the map is visible (map is always light-themed).
    // Reverts to system default (auto dark/light) when the list is shown.
    StatusBarAppearanceEffect(lightStatusBar = showMap)

    // Recomposition log — SideEffect runs on every recomposition.
    SideEffect {
        log(
            "[SEARCH_STOP_SINGLE_PANE] recomposed: showMap=$showMap " +
                "| isMapsAvailable=${searchStopState.isMapsAvailable} " +
                "| animateMapButton=$animateMapButton " +
                "| mapState=${searchStopState.mapUiState?.let { it::class.simpleName } ?: "null"} " +
                "| listState=${searchStopState.listState::class.simpleName}",
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Map — only composed when map mode is active. Avoids rendering the map (and its
        // heavy MapLibre internals) while the list is visible, which prevents the flash on
        // re-navigation where mapUiState is still non-null from a previous visit.
        val mapState = searchStopState.mapUiState
        if (showMap && mapState != null) {
            SearchStopMap(
                modifier = Modifier.fillMaxSize(),
                mapUiState = mapState,
                keyboard = keyboard,
                focusRequester = focusRequester,
                ornamentTopPadding = topBarHeightDp,
                autoShowOptionsSheet = searchStopState.showMapOptionsOnOpen,
                onShowOptionsSheet = { onEvent(SearchStopUiEvent.MapOptionsFirstTimeShown) },
                onEvent = onEvent,
                onStopSelect = onStopSelect,
            )
        }

        // List — shows when: list mode is active, OR map is desired but not yet initialized
        // (mapState == null). This prevents a blank screen while the map initializes.
        AnimatedVisibility(
            visible = !showMap || mapState == null,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            modifier = Modifier.fillMaxSize(),
        ) {
            CloudGradientBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                themeColor = themeColor.hexToComposeColor(),
            ) {
                SearchStopListContent(
                    listState = searchStopState.listState,
                    searchStopState = searchStopState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    expandedStopKey = expandedStopKey,
                    onToggleExpandStop = onToggleExpandStop,
                    onStopSelect = onStopSelect,
                    onLabelPillClick = onLabelPillClick,
                    onNewLabelClick = onNewLabelClick,
                    onManageClick = onManageClick,
                    onEvent = onEvent,
                    isMapsAvailable = searchStopState.isMapsAvailable,
                    onOpenMap = {
                        onShowMapChange(true)
                        onEvent(SearchStopUiEvent.SelectOnMapButtonClicked)
                        if (searchStopState.mapUiState == null) {
                            onEvent(SearchStopUiEvent.InitializeMap)
                        }
                    },
                    modifier = Modifier.padding(top = topBarHeightDp),
                )
            }
        }

        // TopBar always floats on top — transparent background now shows map beneath it
        SearchTopBar(
            placeholderText = placeholderText,
            initialText = initialText,
            focusRequester = focusRequester,
            keyboard = keyboard,
            isMapSelected = showMap,
            isMapAvailable = false, // map pill removed — "Select on map" button is in the list
            animateMapButton = animateMapButton,
            onMapToggle = { shouldShowMap ->
                onShowMapChange(shouldShowMap)
                if (shouldShowMap && searchStopState.mapUiState == null) {
                    onEvent(SearchStopUiEvent.InitializeMap)
                }
            },
            onBackClick = onBackClick,
            onTextChange = { value ->
                // Typing while map is visible switches back to list.
                // onFocusGained was removed — focus events fire on pane transitions and
                // dark/light mode changes too, which would spuriously reset map mode.
                if (showMap) {
                    onShowMapChange(false)
                }
                onTextChange(value)
            },
            // The top bar floats at the TOP of the screen; the keyboard is at the BOTTOM.
            // They never overlap, so IME padding on the bar is wrong: it inflates topBarHeightDp
            // by the keyboard height, which pushes SearchStopListContent down by the same amount
            // even though the root Box's imePadding() already shrinks the layout area.
            // The root Box handles keyboard avoidance for all children.
            applyImePadding = false,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .onGloballyPositioned { coords -> topBarHeightPx = coords.size.height },
        )
    }
}

/**
 * Dual-pane layout for tablets and foldables.
 * Shows list on the left and map on the right simultaneously.
 * No map toggle button needed.
 */
@Composable
private fun SearchStopScreenDualPane(
    searchStopState: SearchStopState,
    themeColor: String,
    placeholderText: String,
    initialText: String,
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel, LabelAssignSurface) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit,
    onManageClick: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    dualPaneMapUiState: MapUiState? = null,
    onDualPaneMapEvent: (MapStopSelectionEvent) -> Unit = {},
) {
    // Dual-pane (tablet / foldable / phone landscape): do NOT auto-show the keyboard.
    // On iOS the soft keyboard covers ~50% of the landscape height and, more critically,
    // its show/hide drives a layout-resize storm. Combined with the fixed-width list pane,
    // the map's weight(1f) gets transiently measured at 0 width, which makes MapLibre's iOS
    // UIKitView project a degenerate frame — the camera target runs away to invalid coords
    // (e.g. longitude > 180) and the map renders blank. SavedTrips' dual-pane map is stable
    // precisely because it never auto-shows the keyboard. The map is the primary interaction
    // here; the user taps the search field when they actually want to type.

    // Dual-pane split via the shared DualPaneScaffold so SavedTrips and SearchStop can't drift.
    // The scaffold owns the fixed-width-list / weighted-right-pane contract AND the invariant
    // that the right-pane map is a SIBLING of the list — never nested under the list's
    // CloudGradientBackground. The gradient uses an offscreen graphicsLayer; on iOS a UIKitView
    // (MapLibre's MLNMapView) can't composite into an offscreen buffer and renders blank, so the
    // gradient must wrap ONLY the list. See DualPaneScaffold + docs/TABLET_FOLDABLE_UX.md §2.
    DualPaneScaffold(
        modifier = modifier,
        listPane = {
            CloudGradientBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                themeColor = themeColor.hexToComposeColor(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SearchTopBar(
                        placeholderText = placeholderText,
                        initialText = initialText,
                        focusRequester = focusRequester,
                        keyboard = keyboard,
                        isMapSelected = false,
                        isMapAvailable = false,
                        onMapToggle = { },
                        onBackClick = onBackClick,
                        onTextChange = onTextChange,
                        // Dual-pane: top bar is the first child of the left Column, not a floating
                        // overlay. ime padding here would push the list content down by the keyboard
                        // height (the "content moved down" bug, worst on iOS landscape).
                        applyImePadding = false,
                    )

                    SearchStopListContent(
                        listState = searchStopState.listState,
                        searchStopState = searchStopState,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        expandedStopKey = expandedStopKey,
                        onToggleExpandStop = onToggleExpandStop,
                        onStopSelect = onStopSelect,
                        onLabelPillClick = onLabelPillClick,
                        onNewLabelClick = onNewLabelClick,
                        onManageClick = onManageClick,
                        onEvent = onEvent,
                        isMapsAvailable = false, // map already visible in right pane
                        onOpenMap = {},
                    )
                }
            }
        },
        rightPane = dualPaneMapUiState?.let { mapState ->
            {
                MapStopSelectionPane(
                    mapUiState = mapState,
                    onEvent = onDualPaneMapEvent,
                    onStopSelected = onStopSelect,
                )
            }
        },
    )
}

/**
 * Shared list content component used by both single-pane and dual-pane layouts.
 */
@Composable
private fun SearchStopListContent(
    listState: ListState,
    searchStopState: SearchStopState,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    isMapsAvailable: Boolean,
    onOpenMap: () -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel, LabelAssignSurface) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit,
    onManageClick: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = listState is ListState.Results && listState.isLoading
    val showPillRow = remember(listState, searchStopState.recentStops, searchStopState.stopLabels) {
        shouldShowPillRow(listState, searchStopState.recentStops, searchStopState.stopLabels)
    }
    val showEmptyStateStops = remember(listState, searchStopState.recentStops) {
        shouldShowEmptyStateStops(listState, searchStopState.recentStops)
    }
    when (listState) {
        ListState.Recent -> {
            // Box wrapper lets the floating "searching..." pill sit on the z-axis above
            // the LazyColumn so the loading state doesn't claim a chunk of vertical space.
            Box(modifier = modifier) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = KrailTheme.dimensions.spacingNone,
                        bottom = KrailTheme.dimensions.spacingXXXXL,
                    ),
                ) {
                    pillRowSection(
                        showPillRow = showPillRow,
                        stopLabels = searchStopState.stopLabels,
                        onSetLabelClick = { stopItem ->
                            keyboard?.hide()
                            focusRequester.freeFocus()
                            onStopSelect(stopItem)
                        },
                        onManageClick = onManageClick,
                    )
                    selectOnMapItem(isMapsAvailable = isMapsAvailable, onOpenMap = onOpenMap)
                    recentSearchStopsList(
                        recentStops = searchStopState.recentStops,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        stopLabels = searchStopState.stopLabels,
                        expandedStopKey = expandedStopKey,
                        onToggleExpandStop = onToggleExpandStop,
                        onStopSelect = onStopSelect,
                        onLabelPillClick = { stopItem, label ->
                            onLabelPillClick(
                                stopItem,
                                label,
                                LabelAssignSurface.RECENT,
                            )
                        },
                        onNewLabelClick = { stopItem, modes ->
                            onNewLabelClick(
                                stopItem,
                                modes,
                                LabelAssignSurface.RECENT,
                            )
                        },
                        onEvent = onEvent,
                    )
                    emptyStateStopsList(
                        show = showEmptyStateStops,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        stopLabels = searchStopState.stopLabels,
                        expandedStopKey = expandedStopKey,
                        onToggleExpandStop = onToggleExpandStop,
                        onStopSelect = onStopSelect,
                        onLabelPillClick = { stopItem, label ->
                            onLabelPillClick(
                                stopItem,
                                label,
                                LabelAssignSurface.EMPTY_STATE,
                            )
                        },
                        onNewLabelClick = { stopItem, modes ->
                            onNewLabelClick(
                                stopItem,
                                modes,
                                LabelAssignSurface.EMPTY_STATE,
                            )
                        },
                        onEvent = onEvent,
                    )
                }
                SearchingDotsHeader(
                    isLoading = isLoading,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = KrailTheme.dimensions.spacingS),
                )
            }
        }

        is ListState.Results -> {
            Box(modifier = modifier) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = KrailTheme.dimensions.spacingNone,
                        bottom = KrailTheme.dimensions.spacingXXXXL,
                    ),
                ) {
                    pillRowSection(
                        showPillRow = showPillRow,
                        stopLabels = searchStopState.stopLabels,
                        onSetLabelClick = { stopItem ->
                            keyboard?.hide()
                            focusRequester.freeFocus()
                            onStopSelect(stopItem)
                        },
                        onManageClick = onManageClick,
                    )
                    selectOnMapItem(isMapsAvailable = isMapsAvailable, onOpenMap = onOpenMap)
                    searchResultsList(
                        searchResults = listState.results,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        stopLabels = searchStopState.stopLabels,
                        expandedStopKey = expandedStopKey,
                        onToggleExpandStop = onToggleExpandStop,
                        onStopSelect = onStopSelect,
                        onLabelPillClick = { stopItem, label ->
                            onLabelPillClick(
                                stopItem,
                                label,
                                LabelAssignSurface.SEARCH_RESULT,
                            )
                        },
                        onNewLabelClick = { stopItem, modes ->
                            onNewLabelClick(
                                stopItem,
                                modes,
                                LabelAssignSurface.SEARCH_RESULT,
                            )
                        },
                        onEvent = onEvent,
                    )
                    addressResultsSection(
                        addressResults = searchStopState.addressResults,
                        isLoading = searchStopState.isAddressSearchLoading,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        onStopSelect = onStopSelect,
                        onEvent = onEvent,
                    )
                }
                SearchingDotsHeader(
                    isLoading = listState.isLoading,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = KrailTheme.dimensions.spacingS),
                )
            }
        }

        ListState.NoMatch -> {
            // Local list has no match, but the remote address/POI section is
            // independent of local match state. "No match found" only makes sense
            // when BOTH local and remote have nothing - if the address section has
            // results (or is still loading), skip the error message entirely so the
            // user never sees "No match" sitting above real results.
            val hasAddressContent = searchStopState.addressResults.isNotEmpty() ||
                searchStopState.isAddressSearchLoading
            if (hasAddressContent) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = KrailTheme.dimensions.spacingXXXXL),
                ) {
                    addressResultsSection(
                        addressResults = searchStopState.addressResults,
                        isLoading = searchStopState.isAddressSearchLoading,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        onStopSelect = onStopSelect,
                        onEvent = onEvent,
                    )
                }
            } else {
                ErrorMessage(
                    title = "No match found!",
                    message = "Try something else. \uD83D\uDD0D✨",
                    modifier = modifier.fillMaxWidth(),
                )
            }
        }

        ListState.Error -> {
            ErrorMessage(
                title = "Something went wrong!",
                message = "Let's try searching again.",
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Pill row. Keyed identically across every list state so the row stays mounted (and its
 * drag state survives) when the user transitions between Recent, Loading and Results.
 */
private fun LazyListScope.pillRowSection(
    showPillRow: Boolean,
    stopLabels: kotlinx.collections.immutable.ImmutableList<StopLabel>,
    onSetLabelClick: (StopItem) -> Unit,
    onManageClick: () -> Unit,
) {
    if (!showPillRow || stopLabels.isEmpty()) return
    item(key = "label-shortcuts") {
        LabelShortcutsRow(
            labels = stopLabels,
            onSetLabelClick = onSetLabelClick,
            onManageClick = onManageClick,
        )
    }
}

/** Tappable "Select on map" row — surrounding spacing comes from neighbours. */
private fun LazyListScope.selectOnMapItem(
    isMapsAvailable: Boolean,
    onOpenMap: () -> Unit,
) {
    if (!isMapsAvailable) return
    item(key = "select-on-map") {
        SelectOnMapItem(onOpenMap = onOpenMap)
    }
}

@Suppress("LongMethod", "LongParameterList")
private fun LazyListScope.searchResultsList(
    searchResults: List<SearchStopState.SearchResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    stopLabels: ImmutableList<StopLabel>,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    items(
        items = searchResults,
        key = { result ->
            when (result) {
                is SearchStopState.SearchResult.Stop -> result.stopId
                is SearchStopState.SearchResult.Trip -> result.tripId
                is SearchStopState.SearchResult.Address -> result.addressId
            }
        },
    ) { result ->
        when (result) {
            is SearchStopState.SearchResult.Stop -> {
                val stopItem = StopItem(stopId = result.stopId, stopName = result.stopName)
                val assignedLabel = stopLabels.firstOrNull { it.stopId == result.stopId }
                StopLabelAssignRow(
                    stopName = result.stopName,
                    transportModeSet = result.transportModeType,
                    stopLabels = stopLabels,
                    assignedLabel = assignedLabel,
                    expanded = expandedStopKey == result.stopId,
                    onExpandToggle = { onToggleExpandStop(result.stopId) },
                    onRowClick = {
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        onStopSelect(stopItem)
                        onEvent(
                            SearchStopUiEvent.TrackStopSelected(stopItem = stopItem),
                        )
                    },
                    onLabelPillClick = { label -> onLabelPillClick(stopItem, label) },
                    onNewLabelClick = { onNewLabelClick(stopItem, result.transportModeType) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Divider(
                    modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
                )
            }

            is SearchStopState.SearchResult.Trip -> {
                var itemState by rememberSaveable(result.tripId) {
                    mutableStateOf(TripSearchListItemState.COLLAPSED)
                }

                TripSearchListItem(
                    trip = result,
                    transportMode = result.transportMode,
                    itemState = itemState,
                    onCardClick = {
                        itemState = if (itemState == TripSearchListItemState.COLLAPSED) {
                            TripSearchListItemState.EXPANDED
                        } else {
                            TripSearchListItemState.COLLAPSED
                        }
                    },
                    onStopClick = { stopItem ->
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        onStopSelect(stopItem)
                        onEvent(SearchStopUiEvent.TrackStopSelected(stopItem = stopItem))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
                        .padding(bottom = KrailTheme.dimensions.spacingL),
                )
            }

            // Local search never returns Address results — those come from
            // [addressResultsSection] via the separate remote-search state field.
            // Branch kept only to satisfy sealed-class exhaustiveness.
            is SearchStopState.SearchResult.Address -> Unit
        }
    }
}

/**
 * Address/POI results from the `stop_finder` remote search — always rendered as a
 * trailing section, never merged into or reordering [searchResultsList]. Visibility is
 * driven purely by [addressResults]/[isLoading], independent of the local list's own
 * state (so it can render even when the local list is showing `NoMatch`). See
 * `SEARCH_STOP_LOCATION_AND_ADDRESS` plan notes on the local/remote split.
 */
@Suppress("LongParameterList")
private fun LazyListScope.addressResultsSection(
    addressResults: List<SearchStopState.SearchResult.Address>,
    isLoading: Boolean,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    if (addressResults.isEmpty() && !isLoading) return
    item(key = "address-results-header") {
        val dim = KrailTheme.dimensions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dim.pageHorizontalPadding,
                    vertical = dim.spacingL,
                )
                .clip(RoundedCornerShape(dim.radiusL))
                .background(KrailTheme.colors.discoverChipBackground)
                .padding(horizontal = dim.spacingL, vertical = dim.spacingM),
        ) {
            Text(
                text = "Addresses & places",
                style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = KrailTheme.colors.label,
            )
        }
    }
    items(
        items = addressResults,
        key = { "address_${it.addressId}" },
    ) { address ->
        AddressSearchListItem(
            displayName = address.displayName,
            addressType = address.addressType,
            textColor = KrailTheme.colors.label,
            onClick = {
                val stopItem = StopItem(
                    stopId = address.addressId,
                    stopName = address.displayName,
                    locationKind = LocationKind.ADDRESS,
                    addressType = address.addressType,
                )
                keyboard?.hide()
                focusRequester.freeFocus()
                onStopSelect(stopItem)
                onEvent(
                    SearchStopUiEvent.TrackStopSelected(stopItem = stopItem),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(
            modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
private fun LazyListScope.recentSearchStopsList(
    recentStops: List<SearchStopState.StopResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    stopLabels: ImmutableList<StopLabel>,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    if (recentStops.isNotEmpty()) {
        item("recent_stops_title") {
            val dim = KrailTheme.dimensions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dim.pageHorizontalPadding,
                        end = dim.pageHorizontalPadding,
                        top = dim.spacingXL,
                        bottom = dim.spacingS,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    color = KrailTheme.colors.label,
                )

                Text(
                    text = "Clear all",
                    style = KrailTheme.typography.bodyMedium,
                    color = KrailTheme.colors.label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dim.radiusFull))
                        .klickable {
                            onEvent(
                                SearchStopUiEvent.ClearRecentSearchStops(
                                    recentSearchCount = recentStops.size,
                                ),
                            )
                        }
                        .padding(horizontal = dim.spacingS, vertical = dim.spacingXS),
                )
            }
        }
    }

    items(
        items = recentStops,
        key = { it.stopId },
    ) { stop ->
        val stopItem = StopItem(
            stopId = stop.stopId,
            stopName = stop.stopName,
            locationKind = stop.locationKind,
            addressType = stop.addressType,
        )
        when (stop.locationKind) {
            LocationKind.ADDRESS -> AddressSearchListItem(
                displayName = stop.stopName,
                addressType = stop.addressType.orEmpty(),
                textColor = KrailTheme.colors.label,
                onClick = {
                    keyboard?.hide()
                    focusRequester.freeFocus()
                    onStopSelect(stopItem)
                    onEvent(
                        SearchStopUiEvent.TrackStopSelected(
                            stopItem = stopItem,
                            isRecentSearch = true,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            LocationKind.TRANSIT_STOP -> {
                val assignedLabel = stopLabels.firstOrNull { it.stopId == stop.stopId }
                StopLabelAssignRow(
                    stopName = stop.stopName,
                    transportModeSet = stop.transportModeType,
                    stopLabels = stopLabels,
                    assignedLabel = assignedLabel,
                    expanded = expandedStopKey == stop.stopId,
                    onExpandToggle = { onToggleExpandStop(stop.stopId) },
                    onRowClick = {
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        onStopSelect(stopItem)
                        onEvent(
                            SearchStopUiEvent.TrackStopSelected(
                                stopItem = stopItem,
                                isRecentSearch = true,
                            ),
                        )
                    },
                    onLabelPillClick = { label -> onLabelPillClick(stopItem, label) },
                    onNewLabelClick = { onNewLabelClick(stopItem, stop.transportModeType) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Divider(
            modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    }
}

/**
 * First-open quick-select list. Renders [EMPTY_STATE_STOPS] only when [show] is true
 * (Recent mode with zero recents — see [shouldShowEmptyStateStops]). No header by
 * product decision: a brand-new user just sees a few tappable major stops. Tapping
 * flows through the same [onStopSelect] path as recents, so the chosen stop is saved
 * and shows up under Recents next time (empty-state stops then disappear).
 * See `SEARCH_STOP_UX.md`.
 */
@Suppress("LongParameterList")
private fun LazyListScope.emptyStateStopsList(
    show: Boolean,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    stopLabels: ImmutableList<StopLabel>,
    expandedStopKey: String?,
    onToggleExpandStop: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onLabelPillClick: (StopItem, StopLabel) -> Unit,
    onNewLabelClick: (StopItem, List<TransportMode>) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    if (!show) return
    items(
        items = EMPTY_STATE_STOPS,
        key = { "empty_state_${it.stopId}" },
    ) { stop ->
        val stopItem = StopItem(stopId = stop.stopId, stopName = stop.stopName)
        val assignedLabel = stopLabels.firstOrNull { it.stopId == stop.stopId }
        StopLabelAssignRow(
            stopName = stop.stopName,
            transportModeSet = stop.transportModeType,
            stopLabels = stopLabels,
            assignedLabel = assignedLabel,
            expanded = expandedStopKey == stop.stopId,
            onExpandToggle = { onToggleExpandStop(stop.stopId) },
            onRowClick = {
                keyboard?.hide()
                focusRequester.freeFocus()
                onStopSelect(stopItem)
                onEvent(
                    SearchStopUiEvent.TrackStopSelected(
                        stopItem = stopItem,
                        isRecentSearch = false,
                    ),
                )
            },
            onLabelPillClick = { label -> onLabelPillClick(stopItem, label) },
            onNewLabelClick = { onNewLabelClick(stopItem, stop.transportModeType) },
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(
            modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    }
}

/**
 * Idle pill row — tap-only (set pills select, unset pills enter choose-mode). The
 * trailing "Manage" button opens the full-screen `ManageStopLabelsScreen`, which owns
 * rename / remove-assignment / delete / reorder. See `SEARCH_STOP_UX.md`.
 */
@Composable
private fun LabelShortcutsRow(
    labels: ImmutableList<StopLabel>,
    onSetLabelClick: (StopItem) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dim.pageHorizontalPadding,
            end = dim.pageHorizontalPadding,
            top = dim.spacingS,
            bottom = dim.spacingS,
        ),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        items(items = labels.filter { it.isSet }, key = { it.label }) { label ->
            LabelShortcutPill(
                label = label,
                onSetLabelClick = onSetLabelClick,
            )
        }
        item(key = "trailing-manage") {
            Button(
                onClick = onManageClick,
                colors = ButtonDefaults.monochromeButtonColors(),
                dimensions = ButtonDefaults.chipButtonSize(),
                modifier = Modifier.padding(start = dim.spacingM),
            ) {
                Text(text = "Manage")
            }
        }
    }
}

@Composable
private fun SelectOnMapItem(
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    // Padding matches StopLabelAssignRow so the icon + text leading edge line up with
    // stop rows below. Title uses titleLarge (same as stop names) for the same reason.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable { onOpenMap() }
            .padding(vertical = dim.spacingL, horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(TripPlannerRes.drawable.ic_map),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
            modifier = Modifier.size(dim.spacingXXL),
        )
        Text(
            text = "Select on map",
            color = KrailTheme.colors.onSurface,
            style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun MapAutoInitEffect(
    showMap: Boolean,
    isMapsAvailable: Boolean,
    mapUiState: MapUiState?,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    LaunchedEffect(isMapsAvailable) {
        if (showMap && isMapsAvailable && mapUiState == null) {
            onEvent(SearchStopUiEvent.InitializeMap)
        }
    }
}

// region Previews — every realistic scenario has the seeded Home/Work labels at minimum,
// since defaults are seeded on first install and Home can't be deleted.

private val previewRecentStops: List<SearchStopState.StopResult> = listOf(
    SearchStopState.StopResult(
        "Central Station",
        "stop_central",
        persistentListOf(TransportMode.Train),
    ),
    SearchStopState.StopResult(
        "Town Hall",
        "stop_town_hall",
        persistentListOf(TransportMode.Train, TransportMode.LightRail),
    ),
    SearchStopState.StopResult(
        "Wynyard",
        "stop_wynyard",
        persistentListOf(TransportMode.Train),
    ),
)

private val previewLabelsTypical = persistentListOf(
    StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "stop_central",
        stopName = "Central Station",
    ),
    StopLabel(emoji = "💼", label = "Work"),
)

private val previewLabelsRich = persistentListOf(
    StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "stop_central",
        stopName = "Central Station",
    ),
    StopLabel(
        emoji = "💼",
        label = "Work",
        stopId = "stop_town_hall",
        stopName = "Town Hall",
    ),
    StopLabel(
        emoji = "🏋",
        label = "Gym",
        stopId = "stop_bondi",
        stopName = "Bondi Junction",
    ),
    StopLabel(emoji = "☕", label = "Cafe"),
    StopLabel(emoji = "🏖", label = "Beach"),
)

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_FreshInstall() {
    // Day 1: defaults seeded, nothing else. Train style.
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopScreen(
            searchStopState = SearchStopState(
                listState = ListState.Recent,
                stopLabels = StopLabel.defaults,
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Recent() {
    // Typical idle: Home set, Work unset, recents + Select-on-map. Bus style.
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SearchStopScreen(
            searchStopState = SearchStopState(
                listState = ListState.Recent,
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsTypical,
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_RichLabels() {
    // Power user with several saved labels and recents. Metro style.
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        SearchStopScreen(
            searchStopState = SearchStopState(
                listState = ListState.Recent,
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsRich,
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_SearchLoading() {
    // User typed a query, dots animating. Pill row stays visually anchored. Ferry style.
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        SearchStopScreen(
            searchQuery = "Centra",
            searchStopState = SearchStopState(
                listState = ListState.Results(
                    results = persistentListOf(),
                    isLoading = true,
                ),
                searchQuery = "Centra",
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsTypical,
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_SearchResults() {
    // Stops + a trip route returned for "Central". Train style.
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        val stopResult = SearchStopState.SearchResult.Stop(
            stopName = "Central Station",
            stopId = "stop_central",
            transportModeType = persistentListOf(TransportMode.Train),
        )
        val airportResult = SearchStopState.SearchResult.Stop(
            stopName = "Sydney Airport - International T1",
            stopId = "stop_airport",
            transportModeType = persistentListOf(TransportMode.Train),
        )
        val trip = SearchStopState.SearchResult.Trip(
            tripId = "trip_T1",
            routeShortName = "T1",
            headsign = "To Town Hall",
            stops = persistentListOf(
                SearchStopState.TripStop(
                    stopId = "stop_central",
                    stopName = "Central",
                    stopSequence = 1,
                    transportModeType = persistentListOf(TransportMode.Train),
                ),
                SearchStopState.TripStop(
                    stopId = "stop_town_hall",
                    stopName = "Town Hall",
                    stopSequence = 2,
                    transportModeType = persistentListOf(TransportMode.Train),
                ),
            ),
            transportMode = TransportMode.Train,
        )
        SearchStopScreen(
            searchQuery = "Central",
            searchStopState = SearchStopState(
                listState = ListState.Results(
                    results = persistentListOf(stopResult, airportResult, trip),
                    isLoading = false,
                ),
                searchQuery = "Central",
                searchResults = persistentListOf(stopResult, airportResult, trip),
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsTypical,
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_NoMatch() {
    // Metro style.
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        SearchStopScreen(
            searchQuery = "UnknownStop",
            searchStopState = SearchStopState(
                listState = ListState.NoMatch,
                searchQuery = "UnknownStop",
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsTypical,
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Error() {
    // PurpleDrip style.
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        SearchStopScreen(
            searchQuery = "Query",
            searchStopState = SearchStopState(
                listState = ListState.Error,
                searchQuery = "Query",
                recentStops = previewRecentStops.toImmutableList(),
                stopLabels = previewLabelsTypical,
            ),
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Map() {
    // Single-pane map. PurpleDrip style.
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        SearchStopScreen(
            searchStopState = SearchStopState(
                mapUiState = MapUiState.Ready(),
                stopLabels = previewLabelsTypical,
                isMapsAvailable = true,
            ),
            onEvent = {},
        )
    }
}

// endregion
