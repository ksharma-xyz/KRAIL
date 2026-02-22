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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
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
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.adaptiveui.AdaptiveScreenContent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.backgroundColorOf
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.StopSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItemState
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
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
    searchQuery: String = "",
    goBack: () -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
    onEvent: (SearchStopUiEvent) -> Unit = {},
) {
    SideEffect { log("[SEARCH_STOP_SCREEN] recomposed") }

    val themeColor by LocalThemeColor.current
    // rememberSaveable so text survives rotation and dark/light mode config changes.
    var textFieldText: String by rememberSaveable { mutableStateOf(searchQuery) }
    // Hoisted here so it survives any config change regardless of which pane is active.
    var showMap by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var backClicked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(backClicked) {
        if (backClicked) {
            goBack()
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
                onStopSelect = onStopSelect,
                onEvent = onEvent,
            )
        },
        dualPaneContent = {
            // Tablet/Foldable layout: List on left, Map on right
            SearchStopScreenDualPane(
                modifier = modifier,
                searchStopState = searchStopState,
                themeColor = themeColor,
                initialText = textFieldText,
                focusRequester = focusRequester,
                keyboard = keyboard,
                onBackClick = { backClicked = true },
                onTextChange = { value: String ->
                    log("value: $value")
                    textFieldText = value
                },
                onStopSelect = onStopSelect,
                onEvent = onEvent,
            )
        },
    )
}

/**
 * Single-pane layout for phones.
 * Shows either list OR map based on local UI state with a toggle button.
 */
@Composable
private fun SearchStopScreenSinglePane(
    searchStopState: SearchStopState,
    themeColor: String,
    initialText: String,
    showMap: Boolean,
    onShowMapChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
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
            "[SEARCH_STOP_SINGLE_PANE] recomposed: showMap=$showMap, " +
                "mapState=${searchStopState.mapUiState?.let { it::class.simpleName }}, " +
                "listState=${searchStopState.listState::class.simpleName}",
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
                onEvent = onEvent,
                onStopSelect = onStopSelect,
            )
        }

        // List — always appears instantly (EnterTransition.None) so there is no fade-in
        // window during which the map (or surface background) would show through.
        // The exit fadeOut is kept for the smooth list→map toggle animation.
        AnimatedVisibility(
            visible = !showMap,
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
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
                    modifier = Modifier.padding(top = topBarHeightDp),
                )
            }
        }

        // TopBar always floats on top — transparent background now shows map beneath it
        SearchTopBar(
            placeholderText = "Search here",
            initialText = initialText,
            focusRequester = focusRequester,
            keyboard = keyboard,
            isMapSelected = showMap,
            isMapAvailable = searchStopState.isMapsAvailable,
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
    initialText: String,
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dual-pane always shows the list — request focus on every fresh composition
    // (including after rotation). Single-pane handles its own focus via LaunchedEffect(showMap).
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
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
                    placeholderText = "Search here",
                    initialText = initialText,
                    focusRequester = focusRequester,
                    keyboard = keyboard,
                    isMapSelected = false,
                    isMapAvailable = false, // Hide map toggle in dual-pane mode
                    onMapToggle = { },
                    onBackClick = onBackClick,
                    onTextChange = onTextChange,
                )

                SearchStopListContent(
                    listState = searchStopState.listState,
                    searchStopState = searchStopState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
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
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (listState) {
        ListState.Recent -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp),
            ) {
                item {
                    SearchListHeader()
                }

                recentSearchStopsList(
                    recentStops = searchStopState.recentStops,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
                )
            }
        }

        is ListState.Results -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp),
            ) {
                item("searching_dots") {
                    SearchingDotsHeader(isLoading = listState.isLoading)
                }

                searchResultsList(
                    searchResults = listState.results,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
                    searchQuery = searchStopState.searchQuery,
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
    onStopSelect: (StopItem) -> Unit,
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Divider()
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
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
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
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    if (recentStops.isNotEmpty()) {
        item("recent_stops_title") {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    style = KrailTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal),
                )

                Image(
                    painter = painterResource(TajRes.drawable.ic_close),
                    contentDescription = "Clear recent stops",
                    colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
                    modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .klickable {
                            onEvent(
                                SearchStopUiEvent.ClearRecentSearchStops(
                                    recentSearchCount = recentStops.size,
                                ),
                            )
                        },
                )
            }
        }
    }

    items(
        items = recentStops,
        key = { it.stopId },
    ) { stop ->
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
            modifier = Modifier.fillMaxWidth(),
        )
        Divider()
    }
}

// region Previews

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_ListLoading() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                listState = ListState.Results(
                    results = persistentListOf(),
                    isLoading = true,
                ),
                searchQuery = "Search Query",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            )
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_ListResults_Maps() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val stopResult = SearchStopState.SearchResult.Stop(
                stopName = "Central",
                stopId = "stop_1",
                transportModeType = persistentListOf(TransportMode.Train()),
            )
            val trip = SearchStopState.SearchResult.Trip(
                tripId = "trip_1",
                routeShortName = "T1",
                headsign = "To Town Hall",
                stops = persistentListOf(
                    SearchStopState.TripStop(
                        stopId = "stop_1",
                        stopName = "Central",
                        stopSequence = 1,
                        transportModeType = persistentListOf(TransportMode.Train()),
                    ),
                    SearchStopState.TripStop(
                        stopId = "stop_2",
                        stopName = "Town Hall",
                        stopSequence = 2,
                        transportModeType = persistentListOf(TransportMode.Train()),
                    ),
                ),
                transportMode = TransportMode.Train(),
            )

            val state = SearchStopState(
                listState = ListState.Results(
                    results = persistentListOf(
                        stopResult,
                        trip,
                    ),
                    isLoading = false,
                ),
                searchQuery = "Central",
                searchResults = persistentListOf(stopResult, trip),
                recentStops = persistentListOf(
                    SearchStopState.StopResult(
                        "Central",
                        "stop_1",
                        persistentListOf(TransportMode.Train()),
                    ),
                ),
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            )

            SearchStopScreen(
                searchQuery = "Central",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_ListResults_NoMaps() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val stopResult = SearchStopState.SearchResult.Stop(
                stopName = "Central",
                stopId = "stop_1",
                transportModeType = persistentListOf(TransportMode.Train()),
            )
            val trip = SearchStopState.SearchResult.Trip(
                tripId = "trip_1",
                routeShortName = "T1",
                headsign = "To Town Hall",
                stops = persistentListOf(
                    SearchStopState.TripStop(
                        stopId = "stop_1",
                        stopName = "Central",
                        stopSequence = 1,
                        transportModeType = persistentListOf(TransportMode.Train()),
                    ),
                    SearchStopState.TripStop(
                        stopId = "stop_2",
                        stopName = "Town Hall",
                        stopSequence = 2,
                        transportModeType = persistentListOf(TransportMode.Train()),
                    ),
                ),
                transportMode = TransportMode.Train(),
            )

            val state = SearchStopState(
                listState = ListState.Results(
                    results = persistentListOf(
                        stopResult,
                        trip,
                    ),
                    isLoading = false,
                ),
                searchQuery = "Central",
                searchResults = persistentListOf(stopResult, trip),
                recentStops = persistentListOf(
                    SearchStopState.StopResult(
                        "Central",
                        "stop_1",
                        persistentListOf(TransportMode.Train()),
                    ),
                ),
                isMapsAvailable = false,
            )

            SearchStopScreen(
                searchQuery = "Central",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Recent() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val recent = listOf(
                SearchStopState.StopResult(
                    "Central",
                    "stop_1",
                    persistentListOf(TransportMode.Train()),
                ),
                SearchStopState.StopResult(
                    "Town Hall",
                    "stop_2",
                    persistentListOf(TransportMode.Train()),
                ),
                SearchStopState.StopResult(
                    "Wynyard",
                    "stop_3",
                    persistentListOf(TransportMode.Train()),
                ),
            )
            val state = SearchStopState(
                listState = ListState.Recent,
                searchQuery = "",
                searchResults = persistentListOf(),
                recentStops = recent.toImmutableList(),
                isMapsAvailable = true,
                mapUiState = MapUiState.Ready(),
            )
            SearchStopScreen(
                searchQuery = "",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_NoMatch() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Metro().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                listState = ListState.NoMatch,
                searchQuery = "UnknownStop",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            SearchStopScreen(
                searchQuery = "UnknownStop",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Error() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                listState = ListState.Error,
                searchQuery = "Query",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            SearchStopScreen(
                searchQuery = "Query",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopScreen_Map() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.LightRail().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                mapUiState = MapUiState.Ready(),
                searchQuery = "",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            Column {
                SearchStopScreen(
                    searchQuery = "",
                    searchStopState = state,
                    onEvent = {},
                )
            }
        }
    }
}

// endregion
