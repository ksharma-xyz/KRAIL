package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.adaptiveui.AdaptiveScreenContent
import xyz.ksharma.krail.core.log.log
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
    val themeColor by LocalThemeColor.current
    var textFieldText: String by remember { mutableStateOf(searchQuery) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var backClicked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(backClicked) {
        if (backClicked) {
            goBack()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
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
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local UI state controls whether user is viewing list or map
    var showMap by rememberSaveable { mutableStateOf(false) }

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
        SearchTopBar(
            placeholderText = "Search here",
            focusRequester = focusRequester,
            keyboard = keyboard,
            isMapSelected = showMap,
            isMapAvailable = searchStopState.isMapsAvailable,
            onMapToggle = { shouldShowMap ->
                showMap = shouldShowMap
                // Initialize map if user toggles to map view and it's not initialized
                if (shouldShowMap && searchStopState.mapUiState == null) {
                    onEvent(SearchStopUiEvent.InitializeMap)
                }
            },
            onBackClick = onBackClick,
            onTextChange = onTextChange,
        )

        val mapState = searchStopState.mapUiState
        if (showMap && mapState != null) {
            // In single pane, when displaying map, keyboard is not required.
            LaunchedEffect(Unit) {
                keyboard?.hide()
                focusRequester.freeFocus()
            }

            SearchStopMap(
                modifier = Modifier.weight(1f),
                mapUiState = mapState,
                keyboard = keyboard,
                focusRequester = focusRequester,
                onEvent = onEvent,
                onStopSelect = onStopSelect,
            )
        } else {
            LaunchedEffect(Unit) {
                keyboard?.show()
                focusRequester.requestFocus()
            }
            SearchStopListContent(
                listState = searchStopState.listState,
                searchStopState = searchStopState,
                keyboard = keyboard,
                focusRequester = focusRequester,
                onStopSelect = onStopSelect,
                onEvent = onEvent,
            )
        }
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
    focusRequester: FocusRequester,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onBackClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Initialize map when entering dual-pane mode if maps are available
    LaunchedEffect(Unit) {
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
                SearchStopMap(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    mapUiState = mapState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
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

private fun LazyListScope.recentSearchStopsList(
    recentStops: List<SearchStopState.StopResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
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
                painter = painterResource(Res.drawable.ic_close),
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
