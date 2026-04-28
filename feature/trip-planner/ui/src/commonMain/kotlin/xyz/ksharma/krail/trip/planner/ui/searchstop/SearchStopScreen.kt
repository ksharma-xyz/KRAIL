package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import app.krail.taj.resources.ic_close
import app.krail.taj.resources.ic_location
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.core.adaptiveui.AdaptiveScreenContent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.backgroundColorOf
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.AddLabelBottomSheet
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.LabelConflictSheet
import xyz.ksharma.krail.trip.planner.ui.components.SaveStopAsLabelSheet
import xyz.ksharma.krail.trip.planner.ui.components.StopSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItemState
import xyz.ksharma.krail.trip.planner.ui.components.stopLabelIcon
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

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
) {
    SideEffect { log("[SEARCH_STOP_SCREEN] recomposed") }

    val placeholderText = when (fieldType) {
        SearchStopFieldType.FROM -> "Choose starting point"
        SearchStopFieldType.TO -> "Choose destination"
        SearchStopFieldType.LABEL -> "Choose a stop"
    }

    val themeColor by LocalThemeColor.current
    // rememberSaveable so text survives rotation and dark/light mode config changes.
    var textFieldText: String by rememberSaveable { mutableStateOf(searchQuery) }
    // Hoisted here so it survives any config change regardless of which pane is active.
    var showMap by rememberSaveable { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var backClicked by rememberSaveable { mutableStateOf(false) }

    // Label assignment state: non-null when user is selecting a stop to assign to a label.
    // All four pieces of UI orchestration state below use rememberSaveable so rotation,
    // dark-mode toggles and process death don't drop sheets/edit mode/in-flight conflicts.
    var assigningLabel by rememberSaveable(stateSaver = StopLabelSaver) {
        mutableStateOf<StopLabel?>(null)
    }
    var showAddLabelSheet by rememberSaveable { mutableStateOf(false) }
    var stopBeingSaved by rememberSaveable(stateSaver = StopItemSaver) {
        mutableStateOf<StopItem?>(null)
    }
    // Edit mode for the pill row — long-press a pill to enter; pills wiggle and can
    // be drag-reordered (via longPressDraggableHandle). The ✕ overlay lets you delete
    // a label inline. Tap "Done" to exit.
    var editingLabels by rememberSaveable { mutableStateOf(false) }
    // Conflict surfaced when assigning a stop to a label triggers either a stop-side
    // (stop already on another label) or label-side (label already has a different stop)
    // 1:1 invariant violation.
    var pendingConflict by rememberSaveable(stateSaver = LabelConflictSaver) {
        mutableStateOf<LabelConflict?>(null)
    }

    // Wraps onStopSelect to persist the stop as a label assignment when in assigning mode.
    val effectiveOnStopSelect: (StopItem) -> Unit = { stopItem ->
        val labeling = assigningLabel
        if (labeling != null) {
            assigningLabel = null
            onEvent(SearchStopUiEvent.AssignLabelStop(labeling.label, stopItem))
        }
        onStopSelect(stopItem)
    }

    LaunchedEffect(backClicked) {
        if (backClicked) {
            goBack()
        }
    }

    // When the label being assigned is itself satisfied (a stop got attached either via
    // tapping a row or via the save sheet), clear assigningLabel so the contextual hint
    // banner collapses and the row goes back to its idle visuals.
    LaunchedEffect(assigningLabel, searchStopState.stopLabels) {
        val current = assigningLabel ?: return@LaunchedEffect
        val updated = searchStopState.stopLabels.firstOrNull { it.label == current.label }
        if (updated?.isSet == true) {
            assigningLabel = null
        }
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
                assigningLabel = assigningLabel,
                editingLabels = editingLabels,
                onStopSelect = effectiveOnStopSelect,
                onSaveAsLabel = { stopItem -> stopBeingSaved = stopItem },
                onUnsaveLabel = { stopItem ->
                    val match = searchStopState.stopLabels.firstOrNull { it.stopId == stopItem.stopId }
                    if (match != null) onEvent(SearchStopUiEvent.ClearLabelStop(match.label))
                },
                onUnsetLabelClick = { label ->
                    // Toggle: tap an unset pill to enter assigning mode, tap the same
                    // (or another unset) pill again to exit / switch.
                    assigningLabel = if (assigningLabel?.label == label.label) null else label
                },
                onEnterEditing = { editingLabels = true },
                onDeleteLabel = { label ->
                    onEvent(SearchStopUiEvent.DeleteLabel(label.label))
                },
                onMoveLabel = { labelKey, toIndex ->
                    onEvent(SearchStopUiEvent.MoveLabelToIndex(labelKey, toIndex))
                },
                onAddLabelClick = { showAddLabelSheet = true },
                onDoneEditing = { editingLabels = false },
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
                assigningLabel = assigningLabel,
                editingLabels = editingLabels,
                onStopSelect = effectiveOnStopSelect,
                onSaveAsLabel = { stopItem -> stopBeingSaved = stopItem },
                onUnsaveLabel = { stopItem ->
                    val match = searchStopState.stopLabels.firstOrNull { it.stopId == stopItem.stopId }
                    if (match != null) onEvent(SearchStopUiEvent.ClearLabelStop(match.label))
                },
                onUnsetLabelClick = { label ->
                    // Toggle: tap an unset pill to enter assigning mode, tap the same
                    // (or another unset) pill again to exit / switch.
                    assigningLabel = if (assigningLabel?.label == label.label) null else label
                },
                onEnterEditing = { editingLabels = true },
                onDeleteLabel = { label ->
                    onEvent(SearchStopUiEvent.DeleteLabel(label.label))
                },
                onMoveLabel = { labelKey, toIndex ->
                    onEvent(SearchStopUiEvent.MoveLabelToIndex(labelKey, toIndex))
                },
                onAddLabelClick = { showAddLabelSheet = true },
                onDoneEditing = { editingLabels = false },
                onEvent = onEvent,
            )
        },
    )

    if (showAddLabelSheet) {
        AddLabelBottomSheet(
            stopName = null,
            existingLabels = searchStopState.stopLabels,
            onDismiss = { showAddLabelSheet = false },
            onSave = { emoji, name ->
                showAddLabelSheet = false
                val cleaned = xyz.ksharma.krail.trip.planner.ui.components
                    .normaliseLabelName(name)
                if (cleaned.isNotBlank() &&
                    searchStopState.stopLabels.none {
                        xyz.ksharma.krail.trip.planner.ui.components
                            .labelNamesMatch(it.label, cleaned)
                    }
                ) {
                    onEvent(SearchStopUiEvent.CreateLabel(name = cleaned, emoji = emoji))
                    // Drop into assigning mode so the user knows what to do next.
                    assigningLabel = StopLabel(emoji = emoji, label = cleaned)
                }
            },
        )
    }

    stopBeingSaved?.let { stop ->
        SaveStopAsLabelSheet(
            stopName = stop.stopName,
            labels = searchStopState.stopLabels,
            onLabelChosen = { label ->
                val stopOnAnotherLabel = searchStopState.stopLabels.firstOrNull { other ->
                    other.stopId == stop.stopId && other.label != label.label
                }
                val labelHasDifferentStop = label.takeIf {
                    it.isSet && it.stopId != stop.stopId
                }
                when {
                    stopOnAnotherLabel != null -> {
                        pendingConflict = LabelConflict.StopAlreadyOnAnotherLabel(
                            target = label,
                            stop = stop,
                            existingLabel = stopOnAnotherLabel,
                        )
                        stopBeingSaved = null
                    }
                    labelHasDifferentStop != null -> {
                        pendingConflict = LabelConflict.LabelHasDifferentStop(
                            target = label,
                            stop = stop,
                            existingStopName = labelHasDifferentStop.stopName.orEmpty(),
                        )
                        stopBeingSaved = null
                    }
                    else -> {
                        onEvent(SearchStopUiEvent.AssignLabelStop(label.label, stop))
                        stopBeingSaved = null
                    }
                }
            },
            onCreateNewLabel = {
                stopBeingSaved = null
                showAddLabelSheet = true
            },
            onDismiss = { stopBeingSaved = null },
        )
    }

    pendingConflict?.let { conflict ->
        when (conflict) {
            is LabelConflict.StopAlreadyOnAnotherLabel -> LabelConflictSheet(
                title = "Already saved",
                message = "${conflict.stop.stopName} is currently saved as " +
                    "${conflict.existingLabel.label}. Move it to ${conflict.target.label}?",
                confirmLabel = "Move",
                onConfirm = {
                    onEvent(SearchStopUiEvent.ClearLabelStop(conflict.existingLabel.label))
                    onEvent(SearchStopUiEvent.AssignLabelStop(conflict.target.label, conflict.stop))
                    pendingConflict = null
                },
                onCancel = { pendingConflict = null },
            )
            is LabelConflict.LabelHasDifferentStop -> LabelConflictSheet(
                title = "Already in use",
                message = "${conflict.target.label} is currently saved as " +
                    "${conflict.existingStopName}. Replace with ${conflict.stop.stopName}?",
                confirmLabel = "Replace",
                onConfirm = {
                    onEvent(SearchStopUiEvent.AssignLabelStop(conflict.target.label, conflict.stop))
                    pendingConflict = null
                },
                onCancel = { pendingConflict = null },
            )
        }
    }

}

private sealed interface LabelConflict {
    data class StopAlreadyOnAnotherLabel(
        val target: StopLabel,
        val stop: StopItem,
        val existingLabel: StopLabel,
    ) : LabelConflict

    data class LabelHasDifferentStop(
        val target: StopLabel,
        val stop: StopItem,
        val existingStopName: String,
    ) : LabelConflict
}

// region Savers — keep UI orchestration state alive across rotation / process death.
//
// Saver<T?, Any> shape (rather than mapSaver which requires `Original : Any`) so we can
// hold nullable MutableState backed by a Saver. Returning null from save tells the
// framework "nothing to persist"; restore is only called when there IS something saved.

private val StopLabelSaver: Saver<StopLabel?, Any> = Saver(
    save = { label ->
        label?.let {
            mapOf<String, Any?>(
                "emoji" to it.emoji,
                "label" to it.label,
                "stopId" to it.stopId,
                "stopName" to it.stopName,
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val map = saved as Map<String, Any?>
        StopLabel(
            emoji = map["emoji"] as String,
            label = map["label"] as String,
            stopId = map["stopId"] as String?,
            stopName = map["stopName"] as String?,
        )
    },
)

private val StopItemSaver: Saver<StopItem?, Any> = Saver(
    save = { item ->
        item?.let {
            mapOf<String, Any?>(
                "stopId" to it.stopId,
                "stopName" to it.stopName,
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val map = saved as Map<String, Any?>
        StopItem(
            stopId = map["stopId"] as String,
            stopName = map["stopName"] as String,
        )
    },
)

private val LabelConflictSaver: Saver<LabelConflict?, Any> = Saver(
    save = { conflict ->
        when (conflict) {
            null -> null
            is LabelConflict.StopAlreadyOnAnotherLabel -> mapOf<String, Any?>(
                "kind" to "stop",
                "targetEmoji" to conflict.target.emoji,
                "targetLabel" to conflict.target.label,
                "targetStopId" to conflict.target.stopId,
                "targetStopName" to conflict.target.stopName,
                "stopId" to conflict.stop.stopId,
                "stopName" to conflict.stop.stopName,
                "existingEmoji" to conflict.existingLabel.emoji,
                "existingLabel" to conflict.existingLabel.label,
                "existingStopId" to conflict.existingLabel.stopId,
                "existingStopName" to conflict.existingLabel.stopName,
            )
            is LabelConflict.LabelHasDifferentStop -> mapOf<String, Any?>(
                "kind" to "label",
                "targetEmoji" to conflict.target.emoji,
                "targetLabel" to conflict.target.label,
                "targetStopId" to conflict.target.stopId,
                "targetStopName" to conflict.target.stopName,
                "stopId" to conflict.stop.stopId,
                "stopName" to conflict.stop.stopName,
                "existingStopName" to conflict.existingStopName,
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val map = saved as Map<String, Any?>
        when (map["kind"] as String) {
            "stop" -> LabelConflict.StopAlreadyOnAnotherLabel(
                target = StopLabel(
                    emoji = map["targetEmoji"] as String,
                    label = map["targetLabel"] as String,
                    stopId = map["targetStopId"] as String?,
                    stopName = map["targetStopName"] as String?,
                ),
                stop = StopItem(
                    stopId = map["stopId"] as String,
                    stopName = map["stopName"] as String,
                ),
                existingLabel = StopLabel(
                    emoji = map["existingEmoji"] as String,
                    label = map["existingLabel"] as String,
                    stopId = map["existingStopId"] as String?,
                    stopName = map["existingStopName"] as String?,
                ),
            )
            "label" -> LabelConflict.LabelHasDifferentStop(
                target = StopLabel(
                    emoji = map["targetEmoji"] as String,
                    label = map["targetLabel"] as String,
                    stopId = map["targetStopId"] as String?,
                    stopName = map["targetStopName"] as String?,
                ),
                stop = StopItem(
                    stopId = map["stopId"] as String,
                    stopName = map["stopName"] as String,
                ),
                existingStopName = map["existingStopName"] as String,
            )
            else -> null
        }
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
    assigningLabel: StopLabel?,
    editingLabels: Boolean,
    onStopSelect: (StopItem) -> Unit,
    onSaveAsLabel: (StopItem) -> Unit,
    onUnsaveLabel: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    onMoveLabel: (labelKey: String, toIndex: Int) -> Unit,
    onAddLabelClick: () -> Unit,
    onDoneEditing: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColorOf(themeColor.hexToComposeColor()),
                                KrailTheme.colors.surface,
                            ),
                        ),
                    ),
            ) {
                SearchStopListContent(
                    listState = searchStopState.listState,
                    searchStopState = searchStopState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    assigningLabel = assigningLabel,
                    editingLabels = editingLabels,
                    onStopSelect = onStopSelect,
                    onSaveAsLabel = onSaveAsLabel,
                    onUnsaveLabel = onUnsaveLabel,
                    onUnsetLabelClick = onUnsetLabelClick,
                    onEnterEditing = onEnterEditing,
                    onDeleteLabel = onDeleteLabel,
                    onMoveLabel = onMoveLabel,
                    onAddLabelClick = onAddLabelClick,
                    onDoneEditing = onDoneEditing,
                    onEvent = onEvent,
                    isMapsAvailable = searchStopState.isMapsAvailable,
                    onOpenMap = {
                        onShowMapChange(true)
                        onEvent(SearchStopUiEvent.MapToggleClicked(true))
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
                onEvent(SearchStopUiEvent.MapToggleClicked(shouldShowMap))
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
                    onEvent(SearchStopUiEvent.MapToggleClicked(false))
                }
                onTextChange(value)
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
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
    assigningLabel: StopLabel?,
    editingLabels: Boolean,
    onStopSelect: (StopItem) -> Unit,
    onSaveAsLabel: (StopItem) -> Unit,
    onUnsaveLabel: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    onMoveLabel: (labelKey: String, toIndex: Int) -> Unit,
    onAddLabelClick: () -> Unit,
    onDoneEditing: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dual-pane always shows the list — request focus on every fresh composition
    // (including after rotation). Single-pane handles its own focus via LaunchedEffect(showMap).
    SideEffect {
        log(
            "[SEARCH_STOP_DUAL_PANE] isMapsAvailable=${searchStopState.isMapsAvailable} " +
                "| mapUiState=${searchStopState.mapUiState?.let { it::class.simpleName } ?: "null"}",
        )
    }

    // Focus and keyboard are one-shot — only needed on first entry.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    // Map init is keyed on isMapsAvailable so it re-runs if the remote config flag arrives
    // after the first composition (the flag defaults to false in the initial state).
    LaunchedEffect(searchStopState.isMapsAvailable) {
        log(
            "[SEARCH_STOP_DUAL_PANE] LaunchedEffect(isMapsAvailable): " +
                "isMapsAvailable=${searchStopState.isMapsAvailable} " +
                "mapUiState=${searchStopState.mapUiState?.let { it::class.simpleName } ?: "null"} " +
                "→ willInitMap=${searchStopState.mapUiState == null && searchStopState.isMapsAvailable}",
        )
        if (searchStopState.mapUiState == null && searchStopState.isMapsAvailable) {
            onEvent(SearchStopUiEvent.InitializeMap)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColorOf(themeColor.hexToComposeColor()),
                        KrailTheme.colors.surface,
                    ),
                ),
            )
            .imePadding(),
    ) {
        // Split view: List on left, Map on right
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Left pane: List with search bar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Search top bar only spans the list width
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
                )

                SearchStopListContent(
                    listState = searchStopState.listState,
                    searchStopState = searchStopState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    assigningLabel = assigningLabel,
                    editingLabels = editingLabels,
                    onStopSelect = onStopSelect,
                    onSaveAsLabel = onSaveAsLabel,
                    onUnsaveLabel = onUnsaveLabel,
                    onUnsetLabelClick = onUnsetLabelClick,
                    onEnterEditing = onEnterEditing,
                    onDeleteLabel = onDeleteLabel,
                    onMoveLabel = onMoveLabel,
                    onAddLabelClick = onAddLabelClick,
                    onDoneEditing = onDoneEditing,
                    onEvent = onEvent,
                    isMapsAvailable = false, // map already visible in right pane
                    onOpenMap = {},
                )
            }

            // Right pane: Map (edge-to-edge)
            // Show map if available and initialized
            searchStopState.mapUiState?.let { mapState ->
                // Push compass/scale bar below the status bar so they don't sit behind it.
                val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                SearchStopMap(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    mapUiState = mapState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    ornamentTopPadding = statusBarTopPadding,
                    onEvent = onEvent,
                    onStopSelect = onStopSelect,
                )
            }
        }
    }
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
    assigningLabel: StopLabel?,
    editingLabels: Boolean,
    isMapsAvailable: Boolean,
    onOpenMap: () -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onSaveAsLabel: (StopItem) -> Unit,
    onUnsaveLabel: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    onMoveLabel: (labelKey: String, toIndex: Int) -> Unit,
    onAddLabelClick: () -> Unit,
    onDoneEditing: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedStopIds = remember(searchStopState.stopLabels) {
        searchStopState.stopLabels.mapNotNull { it.stopId }.toSet()
    }
    val isLoading = listState is ListState.Results && listState.isLoading
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
                    if (searchStopState.stopLabels.isNotEmpty()) {
                        item(key = "label-shortcuts") {
                            LabelShortcutsRow(
                                labels = searchStopState.stopLabels,
                                assigningLabel = assigningLabel,
                                editing = editingLabels,
                                onSetLabelClick = { stopItem ->
                                    keyboard?.hide()
                                    focusRequester.freeFocus()
                                    onStopSelect(stopItem)
                                },
                                onUnsetLabelClick = onUnsetLabelClick,
                                onEnterEditing = onEnterEditing,
                                onDeleteLabel = onDeleteLabel,
                                onMoveLabel = onMoveLabel,
                                onAddLabelClick = onAddLabelClick,
                                onDoneEditing = onDoneEditing,
                            )
                        }
                        item(key = "assigning-banner") {
                            // animateContentSize gives a smooth height collapse/expand
                            // when the contextual banner appears or disappears.
                            val bannerText = pillRowBannerText(
                                editing = editingLabels,
                                assigningLabel = assigningLabel,
                                stopLabels = searchStopState.stopLabels,
                            )
                            Box(modifier = Modifier.animateContentSize()) {
                                if (bannerText != null) {
                                    PillRowInfoBanner(text = bannerText)
                                }
                            }
                        }
                    }

                    if (isMapsAvailable) {
                        item(key = "select-on-map") {
                            SelectOnMapItem(onOpenMap = onOpenMap)
                        }
                    }

                    recentSearchStopsList(
                        recentStops = searchStopState.recentStops,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        savedStopIds = savedStopIds,
                        onStopSelect = onStopSelect,
                        onSaveAsLabel = onSaveAsLabel,
                        onUnsaveLabel = onUnsaveLabel,
                        onEvent = onEvent,
                    )

                    item(key = "pt-note") { PublicTransportNote() }
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
                    if (searchStopState.stopLabels.isNotEmpty()) {
                        item(key = "label-shortcuts") {
                            LabelShortcutsRow(
                                labels = searchStopState.stopLabels,
                                assigningLabel = assigningLabel,
                                editing = editingLabels,
                                onSetLabelClick = { stopItem ->
                                    keyboard?.hide()
                                    focusRequester.freeFocus()
                                    onStopSelect(stopItem)
                                },
                                onUnsetLabelClick = onUnsetLabelClick,
                                onEnterEditing = onEnterEditing,
                                onDeleteLabel = onDeleteLabel,
                                onMoveLabel = onMoveLabel,
                                onAddLabelClick = onAddLabelClick,
                                onDoneEditing = onDoneEditing,
                            )
                        }
                        item(key = "assigning-banner") {
                            val bannerText = pillRowBannerText(
                                editing = editingLabels,
                                assigningLabel = assigningLabel,
                                stopLabels = searchStopState.stopLabels,
                            )
                            Box(modifier = Modifier.animateContentSize()) {
                                if (bannerText != null) {
                                    PillRowInfoBanner(text = bannerText)
                                }
                            }
                        }
                    }

                    if (isMapsAvailable) {
                        item(key = "select-on-map") {
                            SelectOnMapItem(onOpenMap = onOpenMap)
                        }
                    }

                    searchResultsList(
                        searchResults = listState.results,
                        keyboard = keyboard,
                        focusRequester = focusRequester,
                        savedStopIds = savedStopIds,
                        onStopSelect = onStopSelect,
                        onSaveAsLabel = onSaveAsLabel,
                        onUnsaveLabel = onUnsaveLabel,
                        onEvent = onEvent,
                        searchQuery = searchStopState.searchQuery,
                    )

                    item(key = "pt-note") { PublicTransportNote() }
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
            ErrorMessage(
                title = "No match found!",
                message = "Try something else. \uD83D\uDD0D✨",
                modifier = modifier.fillMaxWidth(),
            )
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

@Suppress("LongMethod", "LongParameterList")
private fun LazyListScope.searchResultsList(
    searchResults: List<SearchStopState.SearchResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    searchQuery: String,
    savedStopIds: Set<String>,
    onStopSelect: (StopItem) -> Unit,
    onSaveAsLabel: (StopItem) -> Unit,
    onUnsaveLabel: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    items(
        items = searchResults,
        key = { result ->
            when (result) {
                is SearchStopState.SearchResult.Stop -> result.stopId
                is SearchStopState.SearchResult.Trip -> result.tripId
            }
        },
    ) { result ->
        when (result) {
            is SearchStopState.SearchResult.Stop -> {
                val isSaved = result.stopId in savedStopIds
                StopSearchListItem(
                    stopId = result.stopId,
                    stopName = result.stopName,
                    transportModeSet = result.transportModeType.toImmutableSet(),
                    textColor = KrailTheme.colors.label,
                    onClick = { stopItem ->
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        onStopSelect(stopItem)
                        onEvent(
                            SearchStopUiEvent.TrackStopSelected(
                                stopItem = stopItem,
                                searchQuery = searchQuery,
                            ),
                        )
                    },
                    isSaved = isSaved,
                    onSaveAsLabel = if (!isSaved) onSaveAsLabel else null,
                    onUnsaveLabel = if (isSaved) onUnsaveLabel else null,
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
        }
    }
}

@Suppress("LongMethod")
private fun LazyListScope.recentSearchStopsList(
    recentStops: List<SearchStopState.StopResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    savedStopIds: Set<String>,
    onStopSelect: (StopItem) -> Unit,
    onSaveAsLabel: (StopItem) -> Unit,
    onUnsaveLabel: (StopItem) -> Unit,
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
                        top = dim.spacingM,
                        bottom = dim.spacingS,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    style = KrailTheme.typography.titleMedium,
                    color = KrailTheme.colors.label,
                )

                Text(
                    text = "Clear all",
                    style = KrailTheme.typography.labelLarge,
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
        val isSaved = stop.stopId in savedStopIds
        StopSearchListItem(
            stopId = stop.stopId,
            stopName = stop.stopName,
            transportModeSet = stop.transportModeType.toImmutableSet(),
            textColor = KrailTheme.colors.label,
            onClick = { stopItem ->
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
            isSaved = isSaved,
            onSaveAsLabel = if (!isSaved) onSaveAsLabel else null,
            onUnsaveLabel = if (isSaved) onUnsaveLabel else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(
            modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LabelShortcutsRow(
    labels: ImmutableList<StopLabel>,
    assigningLabel: StopLabel?,
    editing: Boolean,
    onSetLabelClick: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    onMoveLabel: (labelKey: String, toIndex: Int) -> Unit,
    onAddLabelClick: () -> Unit,
    onDoneEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val key = from.key as? String ?: return@rememberReorderableLazyListState
        onMoveLabel(key, to.index)
    }

    LazyRow(
        state = lazyListState,
        modifier = modifier,
        // Just enough top padding for the floating ✕ delete chip not to clip.
        contentPadding = PaddingValues(
            start = dim.pageHorizontalPadding,
            end = dim.pageHorizontalPadding,
            top = dim.spacingS,
            bottom = dim.spacingS,
        ),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        items(items = labels, key = { it.label }) { label ->
            val isAssigning = assigningLabel?.label == label.label
            ReorderableItem(reorderState, key = label.label) { isDragging ->
                val rotation = rememberWiggleRotation(
                    active = editing && !isDragging,
                    seed = label.label.hashCode(),
                )
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotation
                        if (isDragging) {
                            scaleX = 1.05f
                            scaleY = 1.05f
                        }
                    },
                ) {
                    // clip BEFORE the gesture detector so the ripple is contained inside
                    // the rounded shape. longPressDraggableHandle (active only while
                    // editing) handles long-press + drag for reordering. The custom
                    // awaitEachGesture below distinguishes tap (release inside long-press
                    // timeout) from long-press (timeout reached) without competing with
                    // the drag handle for events the way combinedClickable did.
                    val pillModifier = Modifier
                        .clip(RoundedCornerShape(dim.radiusFull))
                        .longPressDraggableHandle(
                            enabled = editing,
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        )
                        .pointerInput(label.label, editing) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val tappedQuickly = withTimeoutOrNull(
                                    viewConfiguration.longPressTimeoutMillis,
                                ) {
                                    waitForUpOrCancellation() != null
                                }
                                when (tappedQuickly) {
                                    true -> {
                                        // Released within the long-press window → tap.
                                        if (label.isSet) {
                                            label.toStopItem()?.let(onSetLabelClick)
                                        } else {
                                            onUnsetLabelClick(label)
                                        }
                                    }
                                    null -> {
                                        // Long-press timeout reached without release.
                                        if (!editing) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEnterEditing()
                                        }
                                        // Wait for release; null return means another
                                        // detector (the drag handle) took over.
                                        waitForUpOrCancellation()
                                    }
                                    false -> {
                                        // Cancellation inside timeout (e.g. drag detector
                                        // consumed the events) — let the drag handle run.
                                    }
                                }
                            }
                        }

                    if (label.isSet) {
                        SetLabelPill(label = label, modifier = pillModifier)
                    } else {
                        UnsetLabelPill(
                            label = label,
                            isAssigning = isAssigning,
                            modifier = pillModifier,
                        )
                    }

                    if (editing && !label.isProtected) {
                        DeleteOverlay(
                            onClick = { onDeleteLabel(label) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }
            }
        }
        item(key = "trailing") {
            if (editing) {
                // Extra leading gap so Done reads as a separate action, not the next pill.
                Box(modifier = Modifier.padding(start = dim.spacingL)) {
                    DonePill(onClick = onDoneEditing)
                }
            } else {
                AddLabelPill(onClick = onAddLabelClick)
            }
        }
    }
}

@Composable
private fun DeleteOverlay(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .offset(x = dim.spacingS, y = -dim.spacingS)
            .size(dim.spacingXXL)
            .clip(CircleShape)
            .background(KrailTheme.colors.onSurface, CircleShape)
            .klickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(TajRes.drawable.ic_close),
            contentDescription = "Delete label",
            colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
            modifier = Modifier.size(dim.spacingL),
        )
    }
}

@Composable
private fun rememberWiggleRotation(active: Boolean, seed: Int): Float {
    val transition = rememberInfiniteTransition(label = "wiggle")
    val angle by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 150 + (kotlin.math.abs(seed) % 4) * 30,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = (kotlin.math.abs(seed) * 47) % 200,
                offsetType = StartOffsetType.Delay,
            ),
        ),
        label = "wiggle-rotation",
    )
    return if (active) angle else 0f
}

@Composable
private fun SetLabelPill(
    label: StopLabel,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val themeColor = themeColor()
    val icon = stopLabelIcon(label.label) ?: TajRes.drawable.ic_location
    Row(
        modifier = modifier
            .clip(shape)
            .background(themeColor, shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
            modifier = Modifier.size(dim.spacingXL),
        )
        Text(
            text = label.label,
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.surface,
        )
    }
}

@Composable
private fun UnsetLabelPill(
    label: StopLabel,
    isAssigning: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val themeColor = themeColor()
    val borderColor = if (isAssigning) themeColor else KrailTheme.colors.label
    val contentColor = if (isAssigning) themeColor else KrailTheme.colors.label
    val icon = stopLabelIcon(label.label) ?: TajRes.drawable.ic_location
    Row(
        modifier = modifier
            .clip(shape)
            .border(width = dim.strokeThin, color = borderColor, shape = shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier.size(dim.spacingXL),
        )
        Text(
            text = label.label,
            style = KrailTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun AddLabelPill(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(
                width = dim.strokeThin,
                color = KrailTheme.colors.label,
                shape = shape,
            )
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+ Add",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.label,
        )
    }
}

@Composable
private fun PillRowInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Text(
        text = text,
        style = KrailTheme.typography.bodySmall,
        color = KrailTheme.colors.label,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
                top = dim.spacingXS,
                bottom = dim.spacingS,
            ),
    )
}

/**
 * Visually distinct from label pills — uses inverse onSurface/surface so "Done" reads
 * as an action button (like the journey-card map button), not as another pill.
 */
@Composable
private fun DonePill(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(KrailTheme.colors.onSurface, shape)
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Done",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.surface,
        )
    }
}

@Composable
private fun PublicTransportNote(modifier: Modifier = Modifier) {
    Text(
        text = "You can only select public transport stops on KRAIL\u00A0App.",
        style = KrailTheme.typography.bodySmall,
        color = KrailTheme.colors.label,
        modifier = modifier.padding(
            horizontal = KrailTheme.dimensions.pageHorizontalPadding,
            vertical = KrailTheme.dimensions.spacingM,
        ),
    )
}

@Composable
private fun SelectOnMapItem(
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    // Padding matches StopSearchListItem so the icon + text leading edge line up with
    // stop rows below. Title uses titleLarge (same as stop names) for the same reason.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable { onOpenMap() }
            .padding(vertical = dim.spacingM, horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(TajRes.drawable.ic_location),
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
private fun PreviewSearchStopScreen_AssigningMode() {
    // After tapping unset Work pill — Work outline highlighted. BarbiePink style.
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink) {
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
        // Note: actual assigningLabel banner renders only when assigning mode is active
        // at runtime; static preview shows the resting layout.
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
