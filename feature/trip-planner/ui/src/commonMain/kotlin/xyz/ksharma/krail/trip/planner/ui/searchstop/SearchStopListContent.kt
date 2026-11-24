package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * The list of Search Stop's list state (rule 9 of `docs/MAP_HOME_UX.md`), drawn the way the Search Stop list always was:
 * straight on the cloud gradient, with no card behind it, under the box. In order: the label chips row, Select on map,
 * then the recents (or the nearest or suggested stops) or the results, each stop a [StopLabelAssignRow] with a divider
 * under it. It is as tall as the room allows and scrolls inside. [onOpenMap] is what Select on map does, and null leaves
 * the row out where there is no map to go to.
 */
@Composable
internal fun SearchStopListContent(
    listState: ListState,
    searchStopState: SearchStopState,
    recentsCard: RecentsCard?,
    actions: StopRowActions,
    onOpenMap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val showLabelRow = shouldShowLabelRow(listState, searchStopState.stopLabels)
    when (listState) {
        ListState.Recent -> RecentsList(
            showLabelRow,
            recentsCard,
            searchStopState,
            actions,
            onOpenMap,
            modifier,
        )
        is ListState.Results ->
            ResultsList(listState, showLabelRow, searchStopState, actions, onOpenMap, modifier)
        ListState.NoMatch -> NoMatchContent(searchStopState, actions, modifier)
        ListState.Error -> ErrorMessage(
            title = "Something went wrong!",
            message = "Let's try searching again.",
            modifier = modifier.fillMaxWidth(),
        )
    }
}

/**
 * What the list keeps above its first row and under its last. Above it is one [KrailDimensions.spacingXL] step, 16 dp, so
 * the first row (the label chips, or Select on map when there are none) stands clear of the box and its hairline edge. Under the last row it is what lets the last
 * stop clear the keyboard and the screen's edge, and never less than the navigation bar: a three button bar is taller than
 * the fixed step, and the last row ended under it (found by the device matrix, a 320 dp window with a 48 dp bar).
 */
@Composable
private fun listPadding(): PaddingValues = PaddingValues(
    top = KrailTheme.dimensions.spacingXL,
    bottom = maxOf(
        KrailTheme.dimensions.spacingXXXXL,
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    ),
)

@Composable
private fun RecentsList(
    showLabelRow: Boolean,
    recentsCard: RecentsCard?,
    searchStopState: SearchStopState,
    actions: StopRowActions,
    onOpenMap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST),
        contentPadding = listPadding(),
    ) {
        labelRowSection(
            show = showLabelRow,
            stopLabels = searchStopState.stopLabels,
            actions = actions,
        )
        selectOnMapItem(onOpenMap)
        recentsCardSection(
            card = recentsCard,
            recentCount = searchStopState.recentStops.size,
            actions = actions,
        )
    }
}

@Composable
private fun ResultsList(
    listState: ListState.Results,
    showLabelRow: Boolean,
    searchStopState: SearchStopState,
    actions: StopRowActions,
    onOpenMap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // The searching dots float over the list, so loading does not claim a row of its own.
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SEARCH_STOP_RESULTS_LIST),
            contentPadding = listPadding(),
        ) {
            labelRowSection(
                show = showLabelRow,
                stopLabels = searchStopState.stopLabels,
                actions = actions,
            )
            selectOnMapItem(onOpenMap)
            searchResultsList(searchResults = listState.results, actions = actions)
            addressResultsSection(
                addressResults = searchStopState.addressResults,
                isLoading = searchStopState.isAddressSearchLoading,
                actions = actions,
            )
        }
        SearchingDotsHeader(
            isLoading = listState.isLoading,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = KrailTheme.dimensions.spacingS),
        )
    }
}

/**
 * Local list has no match, but the remote address/POI section is independent of local match state.
 * "No match found" only makes sense when BOTH local and remote have nothing: if the address
 * section has results (or is still loading), skip the message so the rider never sees "No match"
 * sitting above real results.
 */
@Composable
private fun NoMatchContent(searchStopState: SearchStopState, actions: StopRowActions, modifier: Modifier = Modifier) {
    val hasAddressContent = searchStopState.addressResults.isNotEmpty() ||
        searchStopState.isAddressSearchLoading
    if (hasAddressContent) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = listPadding(),
        ) {
            addressResultsSection(
                addressResults = searchStopState.addressResults,
                isLoading = searchStopState.isAddressSearchLoading,
                actions = actions,
            )
        }
    } else {
        ErrorMessage(
            title = "No match found!",
            message = "Try something else. 🔍✨",
            modifier = modifier.fillMaxWidth(),
        )
    }
}
