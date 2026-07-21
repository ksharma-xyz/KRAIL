package xyz.ksharma.krail.trip.planner.ui.parkride

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.adaptiveui.DualPaneScaffold
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.parkride.map.ParkRideMapPane
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ErrorKind
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideUiEvent

/**
 * The searchable picker of supported Park & Ride stations — the one screen behind the home
 * screen's "+ Add" card.
 *
 * Adding is a row tap, not a per-row button: the list is long enough that a button on every
 * row would be a column of repeated controls. The trailing toggle reports state instead.
 *
 * Rows are full-bleed and separated by dividers rather than sat on cards, and are grouped
 * under sticky alphabetical headers so a long list can be scanned by initial.
 */
@Composable
internal fun AddParkRideScreen(
    state: AddParkRideState,
    onBackClick: () -> Unit,
    onEvent: (AddParkRideUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dualPane = rememberAdaptiveLayoutInfo().shouldShowDualPane

    // Hoisted ABOVE the dual-pane branch on purpose. The list is called from two different
    // call sites, so rotating between single- and dual-pane moves it to a new composition
    // slot and any state remembered inside it is dropped - which silently cleared whatever
    // the rider had typed. Seeded from the ViewModel's query so it also survives process
    // death and the ViewModel outliving the composition.
    val searchFieldState = rememberTextFieldState(state.query)

    // Map pane is a SIBLING of the list, never nested inside it — see DualPaneScaffold for
    // the iOS compositing invariant that requires this.
    if (dualPane) {
        DualPaneScaffold(
            modifier = modifier,
            listPane = { AddParkRideListPane(state, searchFieldState, onBackClick, onEvent) },
            rightPane = {
                ParkRideMapPane(
                    stations = state.visibleStations,
                    selectedStation = state.selectedStation,
                    details = state.selectedStationDetails,
                    isLoadingDetails = state.isLoadingSelectedStation,
                    onStationSelected = { onEvent(AddParkRideUiEvent.StationSelected(it)) },
                    onDismissStation = { onEvent(AddParkRideUiEvent.StationDismissed) },
                    onToggleStation = { onEvent(AddParkRideUiEvent.ToggleStation(it)) },
                    onDirectionsClick = { position, name ->
                        onEvent(AddParkRideUiEvent.DirectionsClicked(position, name))
                    },
                )
            },
        )
    } else {
        AddParkRideListPane(state, searchFieldState, onBackClick, onEvent, modifier)
    }
}

@Composable
private fun AddParkRideListPane(
    state: AddParkRideState,
    searchFieldState: TextFieldState,
    onBackClick: () -> Unit,
    onEvent: (AddParkRideUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        TitleBar(
            modifier = Modifier.fillMaxWidth(),
            onNavActionClick = onBackClick,
            title = { Text(text = "Add Park & Ride") },
        )

        TextField(
            state = searchFieldState,
            placeholder = "Search",
            imeAction = ImeAction.Search,
            // Inverted against the page: a dark bar in light mode, a light bar in dark mode.
            // Matching the surface left the field with no visible edge at all.
            colors = TextFieldDefaults.invertedColors(),
            onTextChange = { onEvent(AddParkRideUiEvent.SearchQueryChanged(it.toString())) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding)
                .padding(bottom = dim.spacingM),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            // No horizontal padding: rows and their dividers span the full width. Items that
            // are not full-bleed (headers, messages) pad themselves.
            contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING),
        ) {
            val error = state.error
            when {
                state.isLoading -> loadingRows(state.loadingEmoji)
                error != null -> errorRow(error, onEvent)
                else -> stationRows(state, onEvent)
            }
        }
    }
}

private val previewStations = persistentListOf(
    previewStation(id = "2155384", name = "Tallawong", carParks = listOf("P1", "P2", "P3")),
    previewStation(id = "207210", name = "Gordon Henry St (north)", carParks = listOf("Gordon")),
    previewStation(id = "221210", name = "Revesby", carParks = listOf("Revesby"), isUserAdded = true),
)

private fun previewStation(
    id: String,
    name: String,
    carParks: List<String>,
    isUserAdded: Boolean = false,
) = AddParkRideState.ParkRideStationPickerItem(
    stationId = id,
    stationName = name,
    stopName = "$name Station",
    carParkNames = carParks,
    mappings = carParks.mapIndexed { index, carPark ->
        AddParkRideState.ParkRideMapping(
            stopId = id,
            facilityId = "$id-$index",
            facilityName = carPark,
        )
    },
    isUserAdded = isUserAdded,
)

@PreviewScreen
@Composable
private fun AddParkRideScreenPreview() {
    PreviewTheme {
        AddParkRideScreen(
            state = AddParkRideState(stations = previewStations, isLoading = false),
            onBackClick = {},
            onEvent = {},
        )
    }
}

@PreviewScreen
@Composable
private fun AddParkRideScreenLoadingPreview() {
    PreviewTheme {
        AddParkRideScreen(state = AddParkRideState(), onBackClick = {}, onEvent = {})
    }
}

@PreviewScreen
@Composable
private fun AddParkRideScreenErrorPreview() {
    PreviewTheme {
        AddParkRideScreen(
            state = AddParkRideState(isLoading = false, error = ErrorKind.NoFacilities),
            onBackClick = {},
            onEvent = {},
        )
    }
}

// Clear of the gesture area, and enough that the last row is never the screen edge.
private val LIST_BOTTOM_PADDING = 96.dp
