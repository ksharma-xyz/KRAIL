package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AddressSearchListItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The rows under the label row: the rider's recents under "Recent", the nearest stops under "Near
 * you", or the Intro's suggested stops with no heading at all. A row is the stop's name and its mode
 * badges in the list's own row type. It has no chevron, because a plain recent or suggested stop has
 * nothing to expand; only a result row that opens its label wall draws one. [afterLabelRow] is true when the
 * label row sits above, so the heading leaves a gap under it and none above itself otherwise.
 */
internal fun LazyListScope.recentsCardSection(
    card: RecentsCard?,
    recentCount: Int,
    actions: StopRowActions,
    afterLabelRow: Boolean = false,
) {
    if (card == null) return
    when (card.source) {
        RecentsSource.Recents -> {
            item(key = "recent_stops_title") {
                RecentsHeader(
                    title = "Recent",
                    afterLabelRow = afterLabelRow,
                    // The count is every recent stop the rider has, not the five the card shows.
                    onClearAll = {
                        actions.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = recentCount))
                    },
                )
            }
            items(items = card.rows, key = { it.stopId }) { stop -> RecentStopRow(stop, actions) }
        }

        RecentsSource.Nearest -> {
            item(key = "near_you_title") {
                RecentsHeader(title = "Near you", onClearAll = null, afterLabelRow = afterLabelRow)
            }
            items(items = card.rows, key = { "near_${it.stopId}" }) { stop ->
                SuggestedStopRow(stop, actions)
            }
        }

        RecentsSource.Curated -> items(items = card.rows, key = { "empty_state_${it.stopId}" }) { stop ->
            SuggestedStopRow(stop, actions)
        }
    }
}

/**
 * The card's heading and, on the recents, taj's [TextButton] Clear all at its small size. They are two whole
 * items in one wrapping row, so at large text Clear all drops under the title and neither is squeezed.
 */
@Composable
private fun RecentsHeader(
    title: String,
    onClearAll: (() -> Unit)?,
    afterLabelRow: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
                top = if (afterLabelRow) dim.spacingM else dim.spacingNone,
                bottom = dim.spacingS,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = KrailTheme.colors.label,
        )

        if (onClearAll != null) {
            TextButton(onClick = onClearAll, dimensions = ButtonDefaults.smallButtonSize()) {
                Text(text = "Clear all")
            }
        }
    }
}

/** A recent stop. A recent address is an address row, as it always was. */
@Composable
private fun RecentStopRow(stop: SearchStopState.StopResult, actions: StopRowActions) {
    val stopItem = StopItem(
        stopId = stop.stopId,
        stopName = stop.stopName,
        locationKind = stop.locationKind,
        addressType = stop.addressType,
    )
    when (stop.locationKind) {
        LocationKind.ADDRESS -> AddressSearchListItem(
            displayName = stop.stopName,
            addressType = stop.addressType.orEmpty(),
            textColor = KrailTheme.colors.label,
            onClick = { actions.pick(stopItem, isRecentSearch = true) },
            modifier = Modifier.fillMaxWidth(),
        )

        LocationKind.TRANSIT_STOP -> AssignableStopRow(
            stop = stop,
            surface = LabelAssignSurface.RECENT,
            isRecentSearch = true,
            actions = actions,
            modifier = Modifier.testTag(TripPlannerTestTags.searchStopResult(stop.stopId)),
        )
    }
    Divider(modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding))
}

/**
 * A nearest or suggested stop. It is neither a search nor a recent, so it reports as the empty
 * state surface and as not a recent search, the way the curated first-open stops always did.
 */
@Composable
private fun SuggestedStopRow(stop: SearchStopState.StopResult, actions: StopRowActions) {
    AssignableStopRow(
        stop = stop,
        surface = LabelAssignSurface.EMPTY_STATE,
        isRecentSearch = false,
        actions = actions,
    )
    Divider(modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding))
}

/** One stop as the app draws it in a list: name, mode badges, and a plus that expands to labels. */
@Composable
private fun AssignableStopRow(
    stop: SearchStopState.StopResult,
    surface: LabelAssignSurface,
    isRecentSearch: Boolean,
    actions: StopRowActions,
    modifier: Modifier = Modifier,
) {
    AssignableStopRow(
        stopId = stop.stopId,
        stopName = stop.stopName,
        modes = stop.transportModeType,
        surface = surface,
        isRecentSearch = isRecentSearch,
        actions = actions,
        modifier = modifier,
        showLabelAssign = false,
    )
}

@Composable
internal fun AssignableStopRow(
    stopId: String,
    stopName: String,
    modes: List<TransportMode>,
    surface: LabelAssignSurface,
    isRecentSearch: Boolean,
    actions: StopRowActions,
    modifier: Modifier = Modifier,
    showLabelAssign: Boolean = true,
) {
    val stopItem = StopItem(stopId = stopId, stopName = stopName)
    StopLabelAssignRow(
        stopName = stopName,
        transportModeSet = modes,
        stopLabels = actions.stopLabels,
        assignedLabel = actions.stopLabels.firstOrNull { it.stopId == stopId },
        expanded = actions.expandedStopKey == stopId,
        onExpandToggle = { actions.onToggleExpandStop(stopId) },
        onRowClick = { actions.pick(stopItem, isRecentSearch) },
        onLabelPillClick = { label -> actions.onLabelPillClick(stopItem, label, surface) },
        onNewLabelClick = { actions.onNewLabelClick(stopItem, modes, surface) },
        modifier = modifier.fillMaxWidth(),
        showLabelAssign = showLabelAssign,
    )
}
