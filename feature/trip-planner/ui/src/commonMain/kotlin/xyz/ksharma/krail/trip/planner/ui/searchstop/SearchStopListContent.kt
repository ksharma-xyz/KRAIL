package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * The card the list and the results share (rule 9 of `docs/MAP_HOME_UX.md`): one vertical floating
 * card under the form, with the map visible around it. It is as tall as its rows, up to the room
 * the caller gives it, and then scrolls.
 */
@Composable
internal fun SearchStopListContent(
    listState: ListState,
    searchStopState: SearchStopState,
    recentsCard: RecentsCard?,
    actions: StopRowActions,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showLabelRow = shouldShowLabelRow(listState, searchStopState.stopLabels)
    val hasNothingToShow = listState == ListState.Recent && recentsCard == null && !showLabelRow
    if (hasNothingToShow) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KrailTheme.dimensions.radiusL))
            .background(KrailTheme.colors.surface),
    ) {
        when (listState) {
            ListState.Recent -> RecentsList(showLabelRow, recentsCard, searchStopState, actions, onManageClick)
            is ListState.Results ->
                ResultsList(listState, showLabelRow, searchStopState, actions, onManageClick)
            ListState.NoMatch -> NoMatchContent(searchStopState, actions)
            ListState.Error -> ErrorMessage(
                title = "Something went wrong!",
                message = "Let's try searching again.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentsList(
    showLabelRow: Boolean,
    recentsCard: RecentsCard?,
    searchStopState: SearchStopState,
    actions: StopRowActions,
    onManageClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST),
        contentPadding = PaddingValues(bottom = KrailTheme.dimensions.spacingM),
    ) {
        labelRowSection(
            show = showLabelRow,
            stopLabels = searchStopState.stopLabels,
            actions = actions,
            onManageClick = onManageClick,
        )
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
    onManageClick: () -> Unit,
) {
    Box {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SEARCH_STOP_RESULTS_LIST),
            contentPadding = PaddingValues(bottom = KrailTheme.dimensions.spacingM),
        ) {
            labelRowSection(
                show = showLabelRow,
                stopLabels = searchStopState.stopLabels,
                actions = actions,
                onManageClick = onManageClick,
            )
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
private fun NoMatchContent(searchStopState: SearchStopState, actions: StopRowActions) {
    val hasAddressContent = searchStopState.addressResults.isNotEmpty() ||
        searchStopState.isAddressSearchLoading
    if (hasAddressContent) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = KrailTheme.dimensions.spacingM),
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
