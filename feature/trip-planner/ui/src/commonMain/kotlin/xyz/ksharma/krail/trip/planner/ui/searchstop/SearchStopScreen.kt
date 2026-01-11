package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Feature.Companion.getStringProperty
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
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
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchScreen
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopSelectionType
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
    // derive selection and screen from state
    val selectionType = searchStopState.selectionType
    val screen = searchStopState.screen
    // derive selection and screen from state
    val listState = (screen as? SearchScreen.List)?.listState
    val isLoading = (listState as? ListState.Results)?.isLoading ?: false

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

    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = -33.8727, longitude = 151.2057),
            zoom = 13.0,
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColorOf(themeColor.hexToComposeColor()),
                        KrailTheme.colors.surface,
                    ),
                )
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
            selectionType = selectionType, // or map to UI enum if different type
            onTypeSelected = { type ->
                // user tapped list/map -> forward to viewmodel
                onEvent(SearchStopUiEvent.StopSelectionTypeClicked(type))
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

        when (screen) {
            is SearchScreen.Map -> {
                SearchStopMap(
                    modifier = Modifier.weight(1f),
                    cameraState = camera
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

@Composable
fun SearchStopMap(
    cameraState: CameraState,
    modifier: Modifier = Modifier,
) {
    // Dummy in-memory geojson-like data (hardcoded for now; you can extend it).
    // Contract:
    // - one LineString feature with type="route"
    // - N Point features with type="stop" (rendered as white dots)
    // - onClick reads properties and shows a small overlay UI

    val routeColor = Color(0xFF0055FF)

    val routeFeature = Feature(
        geometry = LineString(
            listOf(
                Position(151.200, -33.875),
                Position(151.206, -33.873),
                Position(151.212, -33.870),
                Position(151.218, -33.867),
            ),
        ),
        properties =
            buildJsonObject {
                put("type", JsonPrimitive("route"))
                put("lineId", JsonPrimitive("T1"))
                put("color", JsonPrimitive("#0055FF"))
            },
    )

    val stopFeatures: List<Feature<*, *>> =
        listOf(
            Feature(
                geometry = Point(Position(151.206, -33.873)),
                properties =
                    buildJsonObject {
                        put("type", JsonPrimitive("stop"))
                        put("stopId", JsonPrimitive("stop_1"))
                        put("stopName", JsonPrimitive("Central"))
                        put("lineId", JsonPrimitive("T1"))
                    },
            ),
            Feature(
                geometry = Point(Position(151.212, -33.870)),
                properties =
                    buildJsonObject {
                        put("type", JsonPrimitive("stop"))
                        put("stopId", JsonPrimitive("stop_2"))
                        put("stopName", JsonPrimitive("Town Hall"))
                        put("lineId", JsonPrimitive("T1"))
                    },
            ),
            Feature(
                geometry = Point(Position(151.218, -33.867)),
                properties =
                    buildJsonObject {
                        put("type", JsonPrimitive("stop"))
                        put("stopId", JsonPrimitive("stop_3"))
                        put("stopName", JsonPrimitive("Wynyard"))
                        put("lineId", JsonPrimitive("T1"))
                    },
            ),
        )

    val featureCollection: FeatureCollection<*, *> =
        FeatureCollection(
            features = (listOf(routeFeature) + stopFeatures) as List<Feature<*, *>>,
        )

    data class SelectedStopUi(
        val id: String?,
        val name: String?,
        val lineId: String?,
    )

    var selectedStop by remember { mutableStateOf<SelectedStopUi?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        MaplibreMap(
            modifier = modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            options =
                MapOptions(
                    ornamentOptions =
                        OrnamentOptions(
                            padding = PaddingValues(0.dp),
                            isLogoEnabled = false,
                            logoAlignment = Alignment.BottomStart,
                            isAttributionEnabled = true,
                            attributionAlignment = Alignment.BottomEnd,
                            isCompassEnabled = true,
                            compassAlignment = Alignment.TopEnd,
                            isScaleBarEnabled = false,
                            scaleBarAlignment = Alignment.TopStart,
                        ),
                ),
        ) {
            val transitSource =
                rememberGeoJsonSource(
                    data = GeoJsonData.Features(featureCollection),
                )

            LineLayer(
                id = "route",
                source = transitSource,
                filter = get("type").asString() eq const("route"),
                color = const(routeColor),
                width = const(10.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )

            CircleLayer(
                id = "stops-visible",
                source = transitSource,
                filter = get("type").asString() eq const("stop"),
                radius = const(6.dp),
                color = const(Color.White),
                strokeColor = const(routeColor),
                strokeWidth = const(2.dp),
            )

            CircleLayer(
                id = "stops-hit",
                source = transitSource,
                filter = get("type").asString() eq const("stop"),
                radius = const(18.dp),
                color = const(Color.White.copy(alpha = 0.01f)),
                strokeColor = const(Color.Transparent),
                strokeWidth = const(0.dp),
                onClick = { features ->
                    val feature = features.firstOrNull()
                    log("Click dot")
                    selectedStop =
                        feature?.let {
                            SelectedStopUi(
                                id = it.getStringProperty("stopId"),
                                name = it.getStringProperty("stopName"),
                                lineId = it.getStringProperty("lineId"),
                            )
                        }
                    log("selectedStop : $selectedStop")
                    ClickResult.Consume
                },
            )
        }

        if (selectedStop != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(KrailTheme.colors.surface)
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .align(Alignment.BottomCenter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedStop?.name ?: "Selected stop",
                            style = KrailTheme.typography.titleMedium,
                        )
                        Text(
                            text = listOfNotNull(
                                selectedStop?.id,
                                selectedStop?.lineId
                            ).joinToString(" • "),
                            style = KrailTheme.typography.bodySmall,
                            color = KrailTheme.colors.onSurface.copy(alpha = 0.7f),
                        )
                    }

                    Image(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Close",
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .klickable { selectedStop = null }
                                .padding(4.dp),
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    )
                }
            }
        }
    }
}


// region Separate Composables

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

// endregion

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
                        isLoading = true
                    )
                ),
                searchQuery = "Search Query",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = state,
                onEvent = {}
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
                            trip
                        ), isLoading = false
                    )
                ),
                searchQuery = "Central",
                searchResults = persistentListOf(stopResult, trip),
                recentStops = persistentListOf(
                    SearchStopState.StopResult(
                        "Central",
                        "stop_1",
                        persistentListOf(TransportMode.Train())
                    )
                ),
            )

            SearchStopScreen(
                searchQuery = "Central",
                searchStopState = state,
                onEvent = {}
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
                    persistentListOf(TransportMode.Train())
                ),
                SearchStopState.StopResult(
                    "Town Hall",
                    "stop_2",
                    persistentListOf(TransportMode.Train())
                ),
                SearchStopState.StopResult(
                    "Wynyard",
                    "stop_3",
                    persistentListOf(TransportMode.Train())
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
                onEvent = {}
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
                onEvent = {}
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
                onEvent = {}
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
                screen = SearchScreen.Map,
                searchQuery = "",
                searchResults = persistentListOf(),
                recentStops = persistentListOf(),
            )
            // Provide a camera for the map preview
            val camera = rememberCameraState(
                firstPosition = CameraPosition(
                    target = Position(latitude = -33.8727, longitude = 151.2057),
                    zoom = 13.0,
                )
            )
            Column {
                SearchStopScreen(
                    searchQuery = "",
                    searchStopState = state,
                    onEvent = {}
                )
            }
        }
    }
}

// endregion
