package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.adaptiveui.AdaptiveScreenContent
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.CloudFieldSpec
import xyz.ksharma.krail.taj.components.CloudGradientBackground
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AssignNewLabelSheet
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardOptions
import xyz.ksharma.krail.trip.planner.ui.stopcard.pickRoleFor

// Less travel than the AI input surface uses. There the field is the screen and can move as
// much as it likes; here it sits behind a list of stop names that a rider is reading, and the
// same amplitude pulls the eye off the words.
private const val SEARCH_FIELD_MOTION_SCALE = 1.6f

private const val SEARCH_DEBOUNCE_MS = 250L

/**
 * A stop the rider picked for a single field, and which field it is for. Only a label, or one leg of a
 * trip being changed, leaves the screen on a pick. On the To and From form a pick stays on the screen.
 */
data class StopPick(
    val stop: StopItem,
    val field: SearchStopFieldType,
)

/**
 * Search Stop (`docs/MAP_HOME_UX.md` rules 8, 9, 24, 35, 36 and 39).
 *
 * It opens as the clean map state: the map, the box with the two pills at the top, the location chip under it while
 * location is not allowed, and the Options button on the bottom row, with no list and no keyboard. Tapping a pill is
 * the list state, with the keyboard and the recents. Back, or a tap on the map, returns to the map
 * state, and back again goes home. The state lives in [SearchStopFlow], held across rotation.
 *
 * On the To and From form a pick of any kind (a recents row, a label shortcut, a result, a stop card
 * action) fills its bar and the screen stays, editing moves to the other bar, and only the go circle
 * leaves, through [onTripChosen] with both stops, and the trip results open over this screen so back from
 * them lands here with both stops still set. Back before the go leaves with nothing, so a half pick is dropped.
 * [presetFrom] and [presetTo] open the form with a stop already held. A label or one leg of a trip is a
 * single field, so its pick leaves at once through [onStopPicked] with the stop and its field.
 * [onStopSelect] is told the stop alone.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun SearchStopScreen(
    searchStopState: SearchStopState,
    modifier: Modifier = Modifier,
    fieldType: SearchStopFieldType = SearchStopFieldType.FROM,
    searchQuery: String = "",
    goBack: () -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
    onStopPicked: (StopPick) -> Unit = { onStopSelect(it.stop) },
    onTripChosen: (from: StopItem, to: StopItem) -> Unit = { _, _ -> },
    presetFrom: StopItem? = null,
    presetTo: StopItem? = null,
    onEvent: (SearchStopUiEvent) -> Unit = {},
    dualPaneMapUiState: MapUiState? = null,
    onDualPaneMapEvent: (MapStopSelectionEvent) -> Unit = {},
    // True when opened from the timetable header to replace one leg of the
    // current trip: scopes the copy to "Change origin" / "Change destination".
    editTripLeg: Boolean = false,
    // The saved name of the label this screen is choosing a stop for, exactly as saved. Only the label field reads it,
    // for its fixed prompt (Search and select Home).
    labelName: String? = null,
    // Manage is a real nav destination (ManageStopLabelsRoute), not a sheet owned by
    // this screen. The caller (SearchStopEntry) wires this to actual navigation.
    onManageLabelsClick: () -> Unit = {},
) {
    SideEffect { log("[SEARCH_STOP_SCREEN] recomposed") }

    val themeColor by LocalThemeColor.current
    // rememberSaveable so text survives rotation and dark/light mode config changes.
    var textFieldText: String by rememberSaveable { mutableStateOf(searchQuery) }
    val fieldState = rememberTextFieldState(searchQuery)

    // Hoisted here so it survives any config change regardless of which pane is active. It opens as
    // the map state; only the single-pane layout reads the mode, dual-pane always shows both.
    var trip by rememberSaveable(stateSaver = SearchStopTripSaver) {
        mutableStateOf(SearchStopTrip(from = presetFrom, to = presetTo))
    }
    var savedFlow by rememberSaveable(stateSaver = SearchStopFlowSaver) {
        mutableStateOf(initialSearchStopFlow(fieldType, editTripLeg, SearchStopTrip(presetFrom, presetTo)))
    }
    // With no map to open on (maps are off for this rider) the list is the screen, so there is no
    // map state to start in and back leaves at once.
    val flow = effectiveFlow(savedFlow, searchStopState.isMapsAvailable)
    val onFlowEvent: (SearchStopFlowEvent) -> Unit = { event ->
        savedFlow = reduceSearchStopFlow(savedFlow, event).flow
    }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var backClicked by rememberSaveable { mutableStateOf(false) }

    val labels = rememberLabelAssignState()
    val location = rememberLocationPillState(searchStopState, onEvent)

    val shape = formShapeFor(fieldType, editTripLeg)
    val autoFrom = rememberAutoFrom(shape, searchStopState)
    // A start that arrives after the screen opened can finish the trip while the From bar waits.
    LaunchedEffect(autoFrom) { savedFlow = settleLateAutoFrom(savedFlow, trip, autoFrom) }
    val fromStop = trip.from ?: autoFrom
    val onPick: (StopItem) -> Unit = { stop ->
        if (shape == FormShape.Single) {
            onStopPicked(StopPick(stop = stop, field = fieldType))
        } else {
            // The pick fills its bar and the screen stays. Editing moves to the other bar, the keyboard
            // follows it, and the text that searched for this stop is cleared for the next one.
            val step = reduceTripPick(trip, savedFlow, pickedBarFor(flow.editing), stop, autoFrom)
            trip = step.trip
            savedFlow = step.flow
            fieldState.clearText()
        }
    }
    val go = tripToGo(trip, autoFrom)
    val form = SearchStopFormModel(
        shape = shape,
        editing = flow.editing,
        toPlaceholder = barPlaceholderFor(ToFromField.To, fieldType, editTripLeg, labelName),
        fromPlaceholder = barPlaceholderFor(ToFromField.From, fieldType, editTripLeg),
        toStop = trip.to,
        fromStop = fromStop,
        fieldState = fieldState,
        focusRequester = focusRequester,
        onFlowEvent = onFlowEvent,
        onGoClick = {
            // The go circle is the only way out with a trip. The keyboard goes first, as the back arrow does.
            go?.let { (from, to) ->
                keyboard?.hide()
                focusRequester.freeFocus()
                // The trip results open over this screen, so it is saved as the map state with nothing
                // edited and the typed text cleared: back from them lands on both bars set, no keyboard.
                onFlowEvent(SearchStopFlowEvent.TripSent)
                fieldState.clearText()
                onTripChosen(from, to)
            }
        },
        onTextChange = { value ->
            log("value: $value")
            textFieldText = value
        },
    )
    val onLabelPillClick = labels.onLabelPillClick(onEvent)
    val rows = StopRowActions(
        keyboard = keyboard,
        focusRequester = focusRequester,
        stopLabels = searchStopState.stopLabels,
        expandedStopKey = labels.expandedStopKey,
        onToggleExpandStop = labels.onToggleExpandStop,
        onStopSelect = onPick,
        onLabelPillClick = onLabelPillClick,
        onNewLabelClick = labels.onNewLabelClick,
        onEvent = onEvent,
        // The card's Save as is the assign path the rows use, and its action is the field being filled in.
        stopCardOptions = StopCardOptions(
            pickRole = pickRoleFor(pickFieldFor(shape, flow.editing, fieldType)),
            labels = searchStopState.stopLabels,
            onSaveAs = { label, stop -> onLabelPillClick(stop, label, LabelAssignSurface.MAP_CARD) },
        ),
    )
    // Same "close keyboard before navigating" order as the back button: otherwise the keyboard stays
    // up mid-navigation and dismisses itself awkwardly once ManageStopLabelsScreen is on screen.
    val effectiveOnManageLabelsClick: () -> Unit = {
        keyboard?.hide()
        focusRequester.freeFocus()
        onManageLabelsClick()
    }

    LaunchedEffect(backClicked) {
        if (backClicked) {
            goBack()
        }
    }

    LaunchedEffect(textFieldText) {
        snapshotFlow { textFieldText.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            // allow blank queries to flow so ViewModel can switch back to Recents
            .mapLatest { text ->
                onEvent(SearchStopUiEvent.SearchTextChanged(text))
            }
            .collectLatest {}
    }

    AdaptiveScreenContent(
        singlePaneContent = {
            SearchStopScreenSinglePane(
                modifier = modifier,
                searchStopState = searchStopState,
                themeColor = themeColor,
                flow = flow,
                form = form,
                rows = rows,
                location = location,
                onBack = {
                    val step = reduceSearchStopFlow(
                        savedFlow,
                        SearchStopFlowEvent.BackPressed(filled = trip.filled(autoFrom)),
                    )
                    savedFlow = step.flow
                    if (step.exit) backClicked = true
                },
                onMapTap = { onFlowEvent(SearchStopFlowEvent.MapTapped) },
                onManageClick = effectiveOnManageLabelsClick,
                onStopSelect = onPick,
                onEvent = onEvent,
                fieldType = fieldType,
            )
        },
        dualPaneContent = {
            SearchStopScreenDualPane(
                modifier = modifier,
                searchStopState = searchStopState,
                themeColor = themeColor,
                form = form,
                rows = rows,
                location = location,
                onBack = { backClicked = true },
                onManageClick = effectiveOnManageLabelsClick,
                fieldType = fieldType,
                dualPaneMapUiState = dualPaneMapUiState,
                onDualPaneMapEvent = onDualPaneMapEvent,
                onStopSelect = onPick,
            )
        },
    )

    labels.NewLabelSheet(stopLabels = searchStopState.stopLabels, onEvent = onEvent)
}

/** The stop nearest the rider that fills the From bar by itself, or null. See [autoFromStop]. */
@Composable
private fun rememberAutoFrom(shape: FormShape, state: SearchStopState): StopItem? {
    val display = (state.mapUiState as? MapUiState.Ready)?.mapDisplay
    val location = display?.userLocation
    val stops = display?.nearbyStops
    return remember(shape, location, stops) {
        if (shape == FormShape.ToAndFrom && location != null && stops != null) {
            autoFromStop(location, stops)
        } else {
            null
        }
    }
}

// region Label assignment, the sheet and its rows

/** The label assign state the rows and the sheet share: one expanded row at a time and the new label sheet. */
internal class LabelAssignState(
    val expandedStopKey: String?,
    private val setExpandedKey: (String?) -> Unit,
    private val target: NewLabelTarget?,
    private val setTarget: (NewLabelTarget?) -> Unit,
) {
    val onToggleExpandStop: (String) -> Unit = { stopId ->
        setExpandedKey(if (expandedStopKey == stopId) null else stopId)
    }

    val onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit = { stopItem, modes, surface ->
        setTarget(NewLabelTarget(stop = stopItem, transportModeSet = modes, surface = surface))
    }

    /** v4 inline assign: the row's wall only shows unset labels, so every tap is a direct, no-confirm assign. */
    fun onLabelPillClick(
        onEvent: (SearchStopUiEvent) -> Unit,
    ): (StopItem, StopLabel, LabelAssignSurface) -> Unit = { stopItem, label, surface ->
        onEvent(
            SearchStopUiEvent.AssignLabelStop(
                labelKey = label.label,
                stopItem = stopItem,
                surface = surface,
            ),
        )
    }

    @Composable
    fun NewLabelSheet(
        stopLabels: ImmutableList<StopLabel>,
        onEvent: (SearchStopUiEvent) -> Unit,
    ) {
        val newLabelStop = target ?: return
        AssignNewLabelSheet(
            stopName = newLabelStop.stop.stopName,
            transportModeSet = newLabelStop.transportModeSet,
            existingLabelNames = stopLabels.map { it.label }.toImmutableList(),
            onDismiss = { setTarget(null) },
            onSave = { name ->
                setTarget(null)
                // AssignNewLabelSheet already ran name through normaliseLabelName before calling
                // onSave, so CreateLabel's stored key and this AssignLabelStop's labelKey are
                // guaranteed to match.
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

@Composable
private fun rememberLabelAssignState(): LabelAssignState {
    // v4 inline assign: the stop currently creating a brand-new label via the "+ New label" pill.
    var newLabelTarget by rememberSaveable(stateSaver = NewLabelTargetSaver) {
        mutableStateOf<NewLabelTarget?>(null)
    }
    // Only one StopLabelAssignRow can be expanded at a time across recents, nearest, suggested and
    // results, keyed by stopId since that is stable across all of them.
    var expandedStopKey by rememberSaveable { mutableStateOf<String?>(null) }
    return LabelAssignState(
        expandedStopKey = expandedStopKey,
        setExpandedKey = { expandedStopKey = it },
        target = newLabelTarget,
        setTarget = { newLabelTarget = it },
    )
}

private const val NEW_LABEL_DEFAULT_EMOJI = "📍"

/** The stop (+ its transport modes, for the sheet's mode roundel) creating a brand-new label.
 * [surface] is the row kind whose "+ New label" chip opened the sheet. */
internal data class NewLabelTarget(
    val stop: StopItem,
    val transportModeSet: List<TransportMode>,
    val surface: LabelAssignSurface,
)

// Saver<T?, Any> shape (rather than mapSaver which requires `Original : Any`) so a nullable
// MutableState can be backed by a Saver. Returning null from save tells the framework "nothing to
// persist"; restore is only called when there IS something saved. Held as strings and ints so no
// unchecked cast is needed to read it back.
private val NewLabelTargetSaver: Saver<NewLabelTarget?, Any> = Saver(
    save = { target ->
        target?.let {
            listOf(
                it.stop.stopId,
                it.stop.stopName,
                it.surface.name,
                it.transportModeSet.joinToString(",") { mode -> mode.productClass.toString() },
            )
        }
    },
    restore = { saved ->
        val parts = (saved as? List<*>).orEmpty().map { it?.toString().orEmpty() }
        NewLabelTarget(
            stop = StopItem(stopId = parts.getOrElse(0) { "" }, stopName = parts.getOrElse(1) { "" }),
            transportModeSet = parts.getOrElse(3) { "" }
                .split(",")
                .mapNotNull { it.toIntOrNull()?.let(TransportMode::fromProductClass) },
            surface = LabelAssignSurface.entries.firstOrNull { e -> e.name == parts.getOrNull(2) }
                ?: LabelAssignSurface.SEARCH_RESULT,
        )
    },
)

// endregion

// region Location

/** The permission and what the pill does with it, shared by both layouts. */
internal class LocationPillState(
    val pill: LocationPill,
    val allowPermissionRequest: Boolean,
    val onPermissionStatus: (PermissionStatus) -> Unit,
    val onRequestPermission: () -> Unit,
    val userLocationManager: UserLocationManager,
    val onClick: () -> Unit,
)

@Composable
private fun rememberLocationPillState(
    searchStopState: SearchStopState,
    onEvent: (SearchStopUiEvent) -> Unit,
): LocationPillState {
    val userLocationManager = rememberUserLocationManager()
    val scope = rememberCoroutineScope()
    // Null until it has been looked up, so the pill never flashes in on a rider who already said yes.
    var status by remember { mutableStateOf<PermissionStatus?>(null) }
    var allowPermissionRequest by remember { mutableStateOf(false) }
    val hasUserLocation = (searchStopState.mapUiState as? MapUiState.Ready)?.mapDisplay?.userLocation != null
    val pill = locationPillFor(status, hasUserLocation)

    // Looked up again on every return to the screen, so coming back from Settings with the
    // permission granted drops the pill at once.
    LifecycleStartEffect(userLocationManager) {
        val job = scope.launch { status = userLocationManager.checkPermissionStatus() }
        onStopOrDispose { job.cancel() }
    }

    return LocationPillState(
        pill = pill,
        allowPermissionRequest = allowPermissionRequest,
        onPermissionStatus = { status = it },
        onRequestPermission = { allowPermissionRequest = true },
        userLocationManager = userLocationManager,
        onClick = {
            when (pill) {
                LocationPill.Ask -> allowPermissionRequest = true
                LocationPill.OpenSettings -> {
                    onEvent(SearchStopUiEvent.LocationPermissionSettingsClicked)
                    userLocationManager.openAppSettings()
                }
                LocationPill.Hidden -> Unit
            }
        },
    )
}

// endregion

/**
 * Single-pane layout for phones. The map state is the map with the box on it, and nothing else but the location
 * chip and the Options row. The list state covers the map with the cloud gradient, the Search Stop list's own
 * background, and the list under the same box. The map is a SIBLING of the gradient and of the floating column,
 * never a child of either: on iOS a MapLibre UIKitView cannot composite into an offscreen graphicsLayer and renders
 * blank. It stays composed under the gradient, so its camera, its open stop card and its nearby stops are as the
 * rider left them when the list state steps back to it, and its semantics are cleared while it is covered so a screen
 * reader never reaches a control it cannot see.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchStopScreenSinglePane(
    searchStopState: SearchStopState,
    themeColor: String,
    flow: SearchStopFlow,
    form: SearchStopFormModel,
    rows: StopRowActions,
    location: LocationPillState,
    fieldType: SearchStopFieldType,
    onBack: () -> Unit,
    onMapTap: () -> Unit,
    onManageClick: () -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapState = searchStopState.mapUiState
    val keyboard = rows.keyboard
    val focusManager = LocalFocusManager.current

    // The map is the screen's background and starts once, as the screen opens.
    LaunchedEffect(searchStopState.isMapsAvailable, mapState == null) {
        if (searchStopState.isMapsAvailable && mapState == null) {
            onEvent(SearchStopUiEvent.InitializeMap)
        }
    }

    ListStateFocusEffect(
        flow = flow,
        keyboard = keyboard,
        focusRequester = form.focusRequester,
        focusManager = focusManager,
    )
    // Two step back. The arrow in the bar and system back are the same step.
    BackHandler(enabled = true, onBack = onBack)

    // The gradient is the screen in the list state, and stands in for the map while there is none.
    val showGradient = flow.mode == SearchStopMode.List || mapState == null

    // Force dark status bar icons while the map is visible (the map is always light-themed).
    StatusBarAppearanceEffect(lightStatusBar = mapState != null && !showGradient)

    // The top block's height after layout, so the map's ornaments (compass, scale bar) and an open stop card start
    // below the box and the chip.
    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeight: Dp by remember { derivedStateOf { with(density) { headerHeightPx.toDp() } } }
    val gaps = rememberSearchStopGaps()
    val ornamentPadding = mapOrnamentPadding(
        gaps = gaps,
        statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        headerHeight = headerHeight,
    )

    // The Options pill's drawn height, which the location control stands level with. It starts at the 100 percent height.
    var optionsPillHeight by remember { mutableStateOf(MAP_OPTIONS_PILL_REST_HEIGHT) }

    // The mode block is open or shut for the map state only.
    var modeBlockOpen by rememberSaveable { mutableStateOf(false) }
    val controlsShown = modeControlsShown(flow.mode, mapReady = mapState is MapUiState.Ready)
    LaunchedEffect(flow.mode) {
        if (flow.mode != SearchStopMode.Map) {
            modeBlockOpen = reduceModeBlock(modeBlockOpen, ModeBlockEvent.LeftMapState)
        }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        if (mapState != null) {
            SearchStopMap(
                modifier = Modifier.fillMaxSize().then(if (showGradient) Modifier.clearAndSetSemantics {} else Modifier),
                mapUiState = mapState,
                keyboard = keyboard,
                focusRequester = form.focusRequester,
                ornamentTopPadding = headerHeight,
                ornamentPadding = ornamentPadding,
                isCovered = showGradient,
                onEvent = onEvent,
                onStopSelect = onStopSelect,
                allowPermissionRequest = location.allowPermissionRequest,
                onPermissionStatus = location.onPermissionStatus,
                onRequestPermission = location.onRequestPermission,
                onMapTap = {
                    modeBlockOpen = reduceModeBlock(modeBlockOpen, ModeBlockEvent.MapTapped)
                    onMapTap()
                },
                stopCardOptions = rows.stopCardOptions,
                locationBottomPadding = { clearance -> locationControlBottomPadding(gaps, optionsPillHeight, clearance) },
                // The controls get out of an open stop card's way by hiding, not by riding above it.
                hidesControlsForCard = true,
                bottomControls = { clearance, controlsHidden ->
                    // A block that was open is shut as the button goes, and stays shut when it returns.
                    LaunchedEffect(controlsHidden) {
                        if (controlsHidden) modeBlockOpen = reduceModeBlock(modeBlockOpen, ModeBlockEvent.ControlsHidden)
                    }
                    if (controlsShown && mapState is MapUiState.Ready) {
                        SearchStopModeControls(
                            open = modeBlockOpen,
                            visible = !controlsHidden,
                            selectedModes = mapState.mapDisplay.selectedTransportModes,
                            onButtonClick = {
                                modeBlockOpen = reduceModeBlock(modeBlockOpen, ModeBlockEvent.ButtonTapped)
                            },
                            onToggleMode = { onEvent(SearchStopUiEvent.TransportModeFilterToggled(it)) },
                            pillHeight = optionsPillHeight,
                            onPillHeight = { optionsPillHeight = it },
                            clearance = clearance,
                        )
                    }
                },
            )
        }
        if (showGradient) {
            // Over the map, so the list state looks as the list always did. It takes every touch, so nothing reaches the
            // map under it.
            CloudGradientBackground(
                modifier = Modifier.fillMaxSize().testTag(TripPlannerTestTags.SEARCH_STOP_LIST_BACKGROUND).pointerInput(Unit) {},
                themeColor = themeColor.hexToComposeColor(),
                spec = CloudFieldSpec.aiPair(themeColorHex = themeColor, motionScale = SEARCH_FIELD_MOTION_SCALE),
            ) {}
        }

        // The box, and the location chip under it while location is not allowed, are one block in both states. The list
        // state adds the list under it, as wide as the screen.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding(),
        ) {
            SearchStopTopBlock(
                form = form,
                location = location,
                onBack = onBack,
                modifier = Modifier.onGloballyPositioned { headerHeightPx = it.size.height },
                showLocationChip = flow.mode == SearchStopMode.Map,
            )
            if (flow.mode == SearchStopMode.List) {
                SearchStopListSection(
                    searchStopState = searchStopState,
                    fieldType = fieldType,
                    rows = rows,
                    onManageClick = onManageClick,
                    onOpenMap = if (searchStopState.isMapsAvailable) onMapTap else null,
                )
            }
        }
    }
}

/**
 * The list under the box, as wide as the screen and as tall as the room allows. [onOpenMap] is Select on map, null
 * where there is no map to go to.
 */
@Composable
private fun ColumnScope.SearchStopListSection(
    searchStopState: SearchStopState,
    fieldType: SearchStopFieldType,
    rows: StopRowActions,
    onManageClick: () -> Unit,
    onOpenMap: (() -> Unit)?,
) {
    val userLocation = (searchStopState.mapUiState as? MapUiState.Ready)?.mapDisplay?.userLocation
    val nearbyStops = (searchStopState.mapUiState as? MapUiState.Ready)?.mapDisplay?.nearbyStops
    val card = remember(
        searchStopState.recentStops,
        searchStopState.curatedStartStops,
        searchStopState.curatedEndStops,
        userLocation,
        nearbyStops,
    ) {
        recentsCardFor(
            recents = searchStopState.recentStops,
            locationKnown = userLocation != null,
            nearest = if (userLocation != null && nearbyStops != null) {
                nearestStopRows(userLocation, nearbyStops)
            } else {
                emptyList()
            },
            curated = searchStopState.curatedStopsFor(fieldType),
        )
    }
    SearchStopListContent(
        listState = searchStopState.listState,
        searchStopState = searchStopState,
        recentsCard = card,
        actions = rows,
        onManageClick = onManageClick,
        onOpenMap = onOpenMap,
        modifier = Modifier.weight(1f, fill = false),
    )
}

/**
 * Moves the keyboard and focus with the state: the list state raises the keyboard on the bar's
 * field, and the map state puts the keyboard away and drops focus. The map state is how the screen
 * opens, so it opens with no keyboard.
 */
@Composable
private fun ListStateFocusEffect(
    flow: SearchStopFlow,
    keyboard: SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
    LaunchedEffect(flow.mode, flow.editing) {
        if (flow.mode == SearchStopMode.List) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }
}

/**
 * Dual-pane layout for tablets and foldables: the form and the list on the left, the map on the
 * right, both always visible. No keyboard is raised on open: on iOS it would drive a layout-resize
 * storm that leaves the map blank. The scaffold keeps the map a SIBLING of the list's gradient.
 * See DualPaneScaffold and `docs/TABLET_FOLDABLE_UX.md` section 2.
 */
@Composable
private fun SearchStopScreenDualPane(
    searchStopState: SearchStopState,
    themeColor: String,
    form: SearchStopFormModel,
    rows: StopRowActions,
    location: LocationPillState,
    fieldType: SearchStopFieldType,
    onBack: () -> Unit,
    onManageClick: () -> Unit,
    onStopSelect: (StopItem) -> Unit,
    modifier: Modifier = Modifier,
    dualPaneMapUiState: MapUiState? = null,
    onDualPaneMapEvent: (MapStopSelectionEvent) -> Unit = {},
) {
    // This layout has no map of its own to track the rider, so a tap that asks for permission asks here.
    LaunchedEffect(location.allowPermissionRequest) {
        if (location.allowPermissionRequest) {
            location.userLocationManager.getCurrentLocation()
            location.onPermissionStatus(location.userLocationManager.checkPermissionStatus())
        }
    }

    DualPaneScaffold(
        modifier = modifier,
        listPane = {
            CloudGradientBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                themeColor = themeColor.hexToComposeColor(),
                spec = CloudFieldSpec.aiPair(themeColorHex = themeColor, motionScale = SEARCH_FIELD_MOTION_SCALE),
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    SearchStopTopBlock(form = form, location = location, onBack = onBack)
                    // The map is already in the right pane, so there is no Select on map to go to.
                    SearchStopListSection(
                        searchStopState = searchStopState,
                        fieldType = fieldType,
                        rows = rows,
                        onManageClick = onManageClick,
                        onOpenMap = null,
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
                    stopCardOptions = rows.stopCardOptions,
                )
            }
        },
    )
}
