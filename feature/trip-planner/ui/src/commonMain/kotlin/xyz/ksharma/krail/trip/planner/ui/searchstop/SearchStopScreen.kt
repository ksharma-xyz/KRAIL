package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import kotlinx.datetime.Clock
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.backgroundColorOf
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.NavActionButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.StopSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
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
    var selectedStop: StopItem? by remember { mutableStateOf(null) }

    LaunchedEffect(selectedStop) {
        selectedStop?.let { onStopSelect(it) }
    }

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
        key2 = searchStopState.stops,
        key3 = searchStopState.isLoading,
    ) {
        if (textFieldText.isNotBlank() && searchStopState.stops.isEmpty()) {
            // To ensure a smooth transition from the results state to the "No match found" state,
            // track the time of the last query. If new results come in during the delay period,
            // then lastQueryTime will be different, therefore, it will prevent
            // "No match found" message from being displayed.
            val currentQueryTime = Clock.System.now().toEpochMilliseconds()
            lastQueryTime = currentQueryTime
            delay(1000)
            if (lastQueryTime == currentQueryTime && searchStopState.stops.isEmpty()) {
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
                TransportMode.Train().priority
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
                }
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

        TextField(
            placeholder = placeholderText,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 12.dp)
                .focusRequester(focusRequester)
                .padding(horizontal = 16.dp),
            maxLength = 30,
            filter = { input ->
                input.filter { it.isLetterOrDigit() || it.isWhitespace() }
            },
            leadingIcon = {
                NavActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    iconContentDescription = "Back",
                    onClick = {
                        keyboard?.hide()
                        focusRequester.freeFocus()
                        backClicked = true
                    }
                )
            }
        ) { value ->
            // log(("value: $value")
            log("value: $value")
            if (value.isNotBlank()) runPlaceholderAnimation = false
            textFieldText = value.toString()
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp),
        ) {
            item {
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
            } else if (searchStopState.stops.isNotEmpty() && textFieldText.isNotBlank()) {
                items(
                    items = searchStopState.stops,
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
                            selectedStop = stopItem
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )

                    Divider()
                }
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

// region Previews

@Composable
private fun PreviewSearchStopScreenLoading() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState.copy(isLoading = true),
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenError() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState.copy(isLoading = false, isError = true),
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenEmpty() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState.copy(
                    isLoading = false,
                    isError = false,
                    stops = persistentListOf(),
                ),
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenTrain() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenCoach() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Coach().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenFerry() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenMetro() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Metro().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenLightRail() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.LightRail().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

@Composable
private fun PreviewSearchStopScreenBus() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopScreen(
                searchQuery = "Search Query",
                searchStopState = searchStopState,
            )
        }
    }
}

private val searchStopState = SearchStopState(
    isLoading = false,
    stops = persistentListOf(
        SearchStopState.StopResult(
            stopId = "123",
            stopName = "Stop Name",
            transportModeType = persistentListOf(TransportMode.Bus()),
        ),
        SearchStopState.StopResult(
            stopId = "235",
            stopName = "Stop Name",
            transportModeType = persistentListOf(TransportMode.Ferry()),
        ),
        SearchStopState.StopResult(
            stopId = "235",
            stopName = "Stop Name",
            transportModeType = persistentListOf(
                TransportMode.Train(),
                TransportMode.Bus(),
            ),
        ),
    ),
)

// endregion

@Composable
fun AnimatedPlaceholderTextField(modifier: Modifier) {
    val transportModes = listOf(
        TransportMode.Bus(),
        TransportMode.Train(),
        TransportMode.Metro(),
        TransportMode.Ferry(),
        TransportMode.LightRail()
    )

    var currentModeIndex by remember { mutableStateOf(0) }
    var currentText by remember { mutableStateOf("Search here") }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(currentText, isDeleting) {
        val targetText = when {
            isDeleting -> "Search"
            else -> when (currentModeIndex) {
                0 -> "Search bus stop id"
                1 -> "Search train station"
                2 -> "Search metro station"
                3 -> "Search ferry wharf"
                4 -> "Search light rail stop"
                else -> "Search here"
            }
        }

        if (currentText != targetText) {
            delay(100) // Typing speed
            currentText = if (isDeleting) {
                currentText.dropLast(1)
            } else {
                targetText.take(currentText.length + 1)
            }
        } else {
            if (isDeleting) {
                isDeleting = false
            } else {
                delay(1500) // Pause before starting delete animation
                isDeleting = true
                currentModeIndex = (currentModeIndex + 1) % transportModes.size
            }
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Mode: ${transportModes[currentModeIndex].name}",
            color = transportModes[currentModeIndex].colorCode.hexToComposeColor(),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            placeholder = currentText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
