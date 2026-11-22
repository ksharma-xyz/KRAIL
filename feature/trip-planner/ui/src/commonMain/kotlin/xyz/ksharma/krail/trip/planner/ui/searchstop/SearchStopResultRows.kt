package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AddressSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItem
import xyz.ksharma.krail.trip.planner.ui.components.TripSearchListItemState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

internal fun LazyListScope.searchResultsList(
    searchResults: List<SearchStopState.SearchResult>,
    actions: StopRowActions,
) {
    items(
        items = searchResults,
        // Prefixed per type. The three id spaces come from unrelated sources - stop ids from
        // the GTFS stop table, trip ids raw from the routes proto, address ids from the
        // geocoder - and nothing makes them disjoint, so an unprefixed key would let a trip
        // and a stop collide and crash the list. Matches the prefixing the address and
        // empty-state sections already use.
        key = { result ->
            when (result) {
                is SearchStopState.SearchResult.Stop -> "stop_${result.stopId}"
                is SearchStopState.SearchResult.Trip -> "trip_${result.tripId}"
                is SearchStopState.SearchResult.Address -> "address_${result.addressId}"
            }
        },
    ) { result -> SearchResultItem(result, actions) }
}

@Composable
private fun SearchResultItem(result: SearchStopState.SearchResult, actions: StopRowActions) {
    when (result) {
        is SearchStopState.SearchResult.Stop -> {
            AssignableStopRow(
                stopId = result.stopId,
                stopName = result.stopName,
                modes = result.transportModeType,
                surface = LabelAssignSurface.SEARCH_RESULT,
                isRecentSearch = false,
                actions = actions,
                modifier = Modifier.testTag(TripPlannerTestTags.searchStopResult(result.stopId)),
            )
            Divider(modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding))
        }

        is SearchStopState.SearchResult.Trip -> TripResultRow(result, actions)

        // Local search never returns Address results: those come from [addressResultsSection] via
        // the separate remote-search state field. Branch kept only for sealed exhaustiveness.
        is SearchStopState.SearchResult.Address -> Unit
    }
}

@Composable
private fun TripResultRow(result: SearchStopState.SearchResult.Trip, actions: StopRowActions) {
    var itemState by rememberSaveable(result.tripId) { mutableStateOf(TripSearchListItemState.COLLAPSED) }

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
        onStopClick = { stopItem -> actions.pick(stopItem) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .padding(bottom = KrailTheme.dimensions.spacingL),
    )
}

/**
 * Address/POI results from the `stop_finder` remote search: always a trailing section, never
 * merged into or reordering [searchResultsList]. Visibility is driven purely by [addressResults]
 * and [isLoading], independent of the local list's own state, so it can render even when the local
 * list is showing `NoMatch`.
 */
internal fun LazyListScope.addressResultsSection(
    addressResults: List<SearchStopState.SearchResult.Address>,
    isLoading: Boolean,
    actions: StopRowActions,
) {
    if (addressResults.isEmpty() && !isLoading) return
    item(key = "address-results-header") { AddressResultsHeader() }
    items(items = addressResults, key = { "address_${it.addressId}" }) { address ->
        AddressResultRow(address, actions)
    }
}

@Composable
private fun AddressResultsHeader() {
    val dim = KrailTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL)
            .clip(RoundedCornerShape(dim.radiusL))
            .background(KrailTheme.colors.discoverChipBackground)
            .padding(horizontal = dim.spacingL, vertical = dim.spacingM),
    ) {
        Text(
            text = "Addresses & places",
            style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = KrailTheme.colors.label,
        )
    }
}

@Composable
private fun AddressResultRow(address: SearchStopState.SearchResult.Address, actions: StopRowActions) {
    AddressSearchListItem(
        displayName = address.displayName,
        addressType = address.addressType,
        textColor = KrailTheme.colors.label,
        onClick = {
            actions.pick(
                StopItem(
                    stopId = address.addressId,
                    stopName = address.displayName,
                    locationKind = LocationKind.ADDRESS,
                    addressType = address.addressType,
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Divider(modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding))
}
