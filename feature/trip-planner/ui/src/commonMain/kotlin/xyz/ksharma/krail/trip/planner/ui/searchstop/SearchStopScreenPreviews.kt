package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// Previews: every realistic scenario has the seeded Home/Work labels at minimum,
// since defaults are seeded on first install and Home can't be deleted.

private const val PREVIEW_CENTRAL = "Central Station"
private const val PREVIEW_CENTRAL_ID = "stop_central"
private const val PREVIEW_TOWN_HALL = "Town Hall"
private const val PREVIEW_TOWN_HALL_ID = "stop_town_hall"

private val previewRecentStops: List<SearchStopState.StopResult> = listOf(
    SearchStopState.StopResult(
        PREVIEW_CENTRAL,
        PREVIEW_CENTRAL_ID,
        persistentListOf(TransportMode.Train),
    ),
    SearchStopState.StopResult(
        PREVIEW_TOWN_HALL,
        PREVIEW_TOWN_HALL_ID,
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
        stopId = PREVIEW_CENTRAL_ID,
        stopName = PREVIEW_CENTRAL,
    ),
    StopLabel(emoji = "💼", label = "Work"),
)

private val previewLabelsRich = persistentListOf(
    StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = PREVIEW_CENTRAL_ID,
        stopName = PREVIEW_CENTRAL,
    ),
    StopLabel(
        emoji = "💼",
        label = "Work",
        stopId = PREVIEW_TOWN_HALL_ID,
        stopName = PREVIEW_TOWN_HALL,
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
            stopName = PREVIEW_CENTRAL,
            stopId = PREVIEW_CENTRAL_ID,
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
                    stopId = PREVIEW_CENTRAL_ID,
                    stopName = "Central",
                    stopSequence = 1,
                    transportModeType = persistentListOf(TransportMode.Train),
                ),
                SearchStopState.TripStop(
                    stopId = PREVIEW_TOWN_HALL_ID,
                    stopName = PREVIEW_TOWN_HALL,
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
    // The map state, how the screen opens: the map and one dark pill, no list and no keyboard.
    // PurpleDrip style.
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

/** A form holding a destination, with the query field on the From bar, for the group card previews. */
@Composable
private fun previewFormModel(): SearchStopFormModel = SearchStopFormModel(
    shape = FormShape.ToAndFrom,
    editing = ToFromField.To,
    toPlaceholder = "Choose destination",
    fromPlaceholder = "Choose starting point",
    toStop = StopItem(stopId = PREVIEW_TOWN_HALL_ID, stopName = PREVIEW_TOWN_HALL),
    fromStop = null,
    fieldState = rememberTextFieldState(),
    focusRequester = remember { FocusRequester() },
    onFlowEvent = {},
    onGoClick = {},
    onTextChange = {},
)

@Composable
private fun previewLocationState(pill: LocationPill): LocationPillState = LocationPillState(
    pill = pill,
    allowPermissionRequest = false,
    onPermissionStatus = {},
    onRequestPermission = {},
    userLocationManager = rememberUserLocationManager(),
    onClick = {},
)

/** The flat stand-in for the always light ground the cards float on. */
@Composable
private fun PreviewGround(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(PREVIEW_GROUND))
            .padding(KrailTheme.dimensions.spacingXL),
    ) { content() }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopGroupCard_WithStatusChip() {
    // The top group card, then the location status chip as its own small button under it at the card's left
    // edge. Same in light and dark.
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        PreviewGround {
            SearchStopTopBlock(
                form = previewFormModel(),
                location = previewLocationState(LocationPill.Ask),
                onBack = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopGroupCard_NoStatusChip() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        PreviewGround {
            SearchStopTopBlock(
                form = previewFormModel(),
                location = previewLocationState(LocationPill.Hidden),
                onBack = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopModeBlock_Closed() {
    // Every mode on, so the button has no active look.
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        PreviewGround {
            SearchStopModeButton(expanded = false, filtered = false, onClick = {})
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopModeBlock_Open() {
    // Two modes switched off, so the button shows its active look, with the block open above it.
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        PreviewGround {
            Column(verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM)) {
                SearchStopModeBlock(
                    selectedModes = persistentSetOf(TransportMode.Train.productClass, TransportMode.Metro.productClass),
                    onToggle = {},
                )
                SearchStopModeButton(expanded = true, filtered = true, onClick = {})
            }
        }
    }
}

private const val PREVIEW_GROUND = 0xFFE8E6E1
