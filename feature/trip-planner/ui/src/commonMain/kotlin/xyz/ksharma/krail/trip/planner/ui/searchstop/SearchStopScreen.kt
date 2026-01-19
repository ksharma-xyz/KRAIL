package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.backgroundColorOf
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.StopSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItemState
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchScreen
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopSelectionType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.time.ExperimentalTime

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
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
        var runPlaceholderAnimation by rememberSaveable { mutableStateOf(true) }
        var placeholderText by rememberSaveable { mutableStateOf("Search here") }
        var isDeleting by rememberSaveable { mutableStateOf(false) }
        var currentModePriority by rememberSaveable {
            mutableStateOf(
                TransportMode.Train().priority,
            )
        } // Start with Train's priority

        val transportModes = remember {
            TransportMode.values().sortedBy { it.priority }
        }

        // Map priorities to corresponding placeholder texts
        val priorityToTextMapping = remember {
            transportModes.associateBy(
                keySelector = { it.priority },
                valueTransform = { mode ->
                    when (mode) {
                        is TransportMode.Bus -> "Search bus stop id"
                        is TransportMode.Train -> "Search train station"
                        is TransportMode.Metro -> "Search metro station"
                        is TransportMode.Ferry -> "Search ferry wharf"
                        is TransportMode.LightRail -> "Search light rail stop"
                        else -> "Search here"
                    }
                },
            )
        }

        LaunchedEffect(placeholderText, isDeleting, runPlaceholderAnimation) {
            if (!runPlaceholderAnimation) {
                // Reset to initial state if animation is stopped
                currentModePriority = TransportMode.Train().priority
                placeholderText = "Search here"
                isDeleting = false
                return@LaunchedEffect
            }

            val targetText = when {
                isDeleting -> "Search " // Clear text all at once during deletion
                else -> priorityToTextMapping[currentModePriority] ?: "Search here"
            }

            if (placeholderText != targetText) {
                delay(100) // Typing speed
                placeholderText = if (isDeleting) {
                    "Search " // Clear text immediately
                } else {
                    targetText.take(placeholderText.length + 1) // Add characters one by one
                }
            } else {
                if (isDeleting) {
                    isDeleting = false
                } else {
                    delay(500) // Pause before starting delete animation
                    isDeleting = true

                    // Move to the next transport mode based on priority
                    val currentIndex =
                        transportModes.indexOfFirst { it.priority == currentModePriority }
                    val nextIndex = (currentIndex + 1) % transportModes.size
                    currentModePriority = transportModes[nextIndex].priority
                }
            }
        }

        SearchTopBar(
            placeholderText = placeholderText,
            focusRequester = focusRequester,
            keyboard = keyboard,
            // pass selection toggle from state (map if needed to composable enum)
            selectionType = searchStopState.selectionType, // or map to UI enum if different type
            isMapAvailable = searchStopState.isMapsAvailable,
            onTypeSelect = { type ->
                // user tapped list/map -> forward to viewmodel
                onEvent(SearchStopUiEvent.StopSelectionTypeClicked(type))
            },
            onBackClick = {
                backClicked = true
            },
            onTextChange = { value ->
                log("value: $value")
                if (value.isNotBlank()) runPlaceholderAnimation = false
                textFieldText = value
            },
        )

        when (val screen = searchStopState.screen) {
            is SearchScreen.Map -> {
                SearchStopMap(
                    modifier = Modifier.weight(1f),
                    mapUiState = screen.mapUiState,
                )
            }

            is SearchScreen.List -> {
                when (val ls = screen.listState) {
                    ListState.Recent -> {
                        LazyColumn(contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp)) {
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
                        LazyColumn(contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp)) {
                            // Replace manual Column + AnimatedVisibility in Results
                            item("searching_dots") {
                                SearchingDotsHeader(isLoading = ls.isLoading)
                            }

                            searchResultsList(
                                searchResults = ls.results,
                                keyboard = keyboard,
                                focusRequester = focusRequester,
                                onStopSelect = onStopSelect,
                                onEvent = onEvent,
                            )
                        }
                    }

                    ListState.NoMatch -> {
                        ErrorMessage(
                            title = "No match found!",
                            message = "Try something else. \uD83D\uDD0D✨",
                            modifier = Modifier
                                .fillMaxWidth(),
                        )
                    }

                    ListState.Error -> {
                        ErrorMessage(
                            title = "Something went wrong!",
                            message = "Let's try searching again.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod")
private fun LazyListScope.searchResultsList(
    searchResults: List<SearchStopState.SearchResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
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
                        onEvent(SearchStopUiEvent.TrackStopSelected(stopItem = stopItem))
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

@Preview(name = "SearchStop - List Loading")
@Composable
private fun PreviewSearchStopScreen_ListLoading() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                selectionType = StopSelectionType.LIST,
                screen = SearchScreen.List(
                    ListState.Results(
                        results = persistentListOf(),
                        isLoading = true,
                    ),
                ),
                searchQuery = "Search Query",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@Preview(name = "SearchStop - List Results")
@Composable
private fun PreviewSearchStopScreen_ListResults() {
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
                selectionType = StopSelectionType.LIST,
                screen = SearchScreen.List(
                    ListState.Results(
                        results = persistentListOf(
                            stopResult,
                            trip,
                        ),
                        isLoading = false,
                    ),
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
            )

            SearchStopScreen(
                searchQuery = "Central",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@Preview(name = "SearchStop - Recent")
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
                selectionType = StopSelectionType.LIST,
                screen = SearchScreen.List(ListState.Recent),
                searchQuery = "",
                searchResults = persistentListOf(),
                recentStops = recent.toImmutableList(),
            )
            SearchStopScreen(
                searchQuery = "",
                searchStopState = state,
                onEvent = {},
            )
        }
    }
}

@Preview(name = "SearchStop - No Match")
@Composable
private fun PreviewSearchStopScreen_NoMatch() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Metro().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                selectionType = StopSelectionType.LIST,
                screen = SearchScreen.List(ListState.NoMatch),
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

@Preview(name = "SearchStop - Error")
@Composable
private fun PreviewSearchStopScreen_Error() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                selectionType = StopSelectionType.LIST,
                screen = SearchScreen.List(ListState.Error),
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

@Preview(name = "SearchStop - Map Selected")
@Composable
private fun PreviewSearchStopScreen_Map() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.LightRail().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            val state = SearchStopState(
                selectionType = StopSelectionType.MAP,
                screen = SearchScreen.Map(),
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
