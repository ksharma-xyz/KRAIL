package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.time.Clock
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
            .filter { it.isNotBlank() }
            .mapLatest { text ->
                // log(("Query - $text")
                onEvent(SearchStopUiEvent.SearchTextChanged(text))
            }.collectLatest {}
    }

    var displayNoMatchFound by remember { mutableStateOf(false) }
    var lastQueryTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(
        key1 = textFieldText,
        key2 = searchStopState.searchResults,
        key3 = searchStopState.isLoading,
    ) {
        if (textFieldText.isNotBlank() && searchStopState.searchResults.isEmpty()) {
            // To ensure a smooth transition from the results state to the "No match found" state,
            // track the time of the last query. If new results come in during the delay period,
            // then lastQueryTime will be different, therefore, it will prevent
            // "No match found" message from being displayed.
            val currentQueryTime = Clock.System.now().toEpochMilliseconds()
            lastQueryTime = currentQueryTime
            delay(1000)
            if (lastQueryTime == currentQueryTime && searchStopState.searchResults.isEmpty()) {
                displayNoMatchFound = true
            }
        } else {
            displayNoMatchFound = false
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
        var runPlaceholderAnimation by rememberSaveable { mutableStateOf(true) }
        var currentModePriority by rememberSaveable {
            mutableStateOf(
                TransportMode.Train().priority,
            )
        } // Start with Train's priority
        var placeholderText by rememberSaveable { mutableStateOf("Search here") }
        var isDeleting by rememberSaveable { mutableStateOf(false) }

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
            selectionType = StopSelectionType.LIST,
            onTypeSelected = {
                // TODO: wire selection state when you support MAP mode
            },
            onBackClick = {
                backClicked = true
            },
            onTextChanged = { value ->
                log("value: $value")
                if (value.isNotBlank()) runPlaceholderAnimation = false
                textFieldText = value
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp),
        ) {
            item("searching_dots") {
                Column(
                    modifier = Modifier.height(KrailTheme.typography.bodyLarge.fontSize.value.dp + 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(
                        visible = searchStopState.isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        AnimatedDots(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (searchStopState.isError && textFieldText.isNotBlank() && searchStopState.isLoading.not()) {
                item(key = "Error") {
                    ErrorMessage(
                        title = "Eh! That's not looking right.",
                        message = "Let's try searching again.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            } else if (searchStopState.searchResults.isNotEmpty() && textFieldText.isNotBlank()) {
                // Separate composable for search results list
                searchResultsList(
                    searchResults = searchStopState.searchResults,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
                )
            } else if (textFieldText.isBlank() && searchStopState.recentStops.isNotEmpty()) {
                // Separate composable for recent search stops list
                recentSearchStopsList(
                    recentStops = searchStopState.recentStops,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    onStopSelect = onStopSelect,
                    onEvent = onEvent,
                )
            } else if (displayNoMatchFound && textFieldText.isNotBlank() && searchStopState.isLoading.not()) {
                item(key = "no_match") {
                    ErrorMessage(
                        title = "No match found!",
                        message = "Try something else. \uD83D\uDD0D✨",
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }
        }
    }
}

// region Separate Composables

/**
 * Displays the list of search results (stops and trips)
 */
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
                        // Toggle between expanded and collapsed states
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
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                )
            }
        }
    }
}

/**
 * Displays the list of recent search stops
 */
private fun LazyListScope.recentSearchStopsList(
    recentStops: List<SearchStopState.StopResult>,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    onStopSelect: (StopItem) -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    item("recent_stops_title") {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent",
                style = KrailTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier,
            )

            Image(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = "Clear recent stops",
                colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .klickable(
                        onClick = {
                            onEvent(
                                SearchStopUiEvent.ClearRecentSearchStops(
                                    recentSearchCount = recentStops.size,
                                ),
                            )
                        },
                    ),
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

// endregion

// region Previews

@Preview
@Composable
private fun PreviewSearchStopScreenLoading() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState.copy(isLoading = true),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenError() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState.copy(isLoading = false, isError = true),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenEmpty() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState.copy(
                    isLoading = false,
                    isError = false,
                    searchResults = persistentListOf(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenTrain() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenCoach() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Coach().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenFerry() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenMetro() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Metro().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenLightRail() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.LightRail().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSearchStopScreenBus() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = previewSearchStopState,
            )
        }
    }
}

private val previewSearchStopState = SearchStopState(
    isLoading = false,
    searchResults = persistentListOf(
        SearchStopState.SearchResult.Stop(
            stopId = "123",
            stopName = "Central Station",
            transportModeType = persistentListOf(TransportMode.Train()),
        ),
        SearchStopState.SearchResult.Stop(
            stopId = "235",
            stopName = "Circular Quay",
            transportModeType = persistentListOf(TransportMode.Ferry()),
        ),
        SearchStopState.SearchResult.Stop(
            stopId = "456",
            stopName = "Town Hall",
            transportModeType = persistentListOf(
                TransportMode.Train(),
                TransportMode.Bus(),
            ),
        ),
        SearchStopState.SearchResult.Trip(
            tripId = "preview_trip_702_1",
            routeShortName = "702",
            headsign = "Blacktown to Seven Hills",
            stops = persistentListOf(
                SearchStopState.TripStop(
                    stopId = "214733",
                    stopName = "Seven Hills Station",
                    stopSequence = 0,
                    transportModeType = persistentListOf(TransportMode.Bus()),
                ),
                SearchStopState.TripStop(
                    stopId = "214794",
                    stopName = "Blacktown Station",
                    stopSequence = 1,
                    transportModeType = persistentListOf(TransportMode.Bus()),
                ),
            ),
            transportMode = TransportMode.Bus(),
        ),
    ),
)

// endregion
