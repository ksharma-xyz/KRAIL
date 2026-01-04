package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Displays a trip search result with all its stops.
 * Shows headsign/direction at top, then ordered list of stops.
 * Each Trip result represents a single direction (e.g., "Blacktown to Parramatta").
 */
@Composable
fun TripSearchListItem(
    trip: SearchStopState.SearchResult.Trip,
    modifier: Modifier = Modifier,
    onStopClick: (StopItem) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = KrailTheme.colors.surface,
                shape = CardShape,
            )
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransportModeBadge(
                badgeText = trip.routeShortName,
                backgroundColor = TransportMode.Bus().colorCode.hexToComposeColor(),
            )

            // Trip headsign as title
            Text(
                text = trip.headsign,
                style = KrailTheme.typography.titleLarge,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display stops - already sorted by ViewModel
        trip.stops.forEachIndexed { index, stop ->
            RouteStopItem(
                stopName = stop.stopName,
                stopId = stop.stopId,
                onClick = {
                    onStopClick(
                        StopItem(stopId = stop.stopId, stopName = stop.stopName)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Add divider between stops (but not after the last one)
            if (index < trip.stops.size - 1) {
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

/**
 * Individual stop item within a route result
 */
@Composable
private fun RouteStopItem(
    stopName: String,
    stopId: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stop name on the left
        Text(
            text = stopName,
            style = KrailTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Stop ID on the right
        Text(
            text = stopId,
            style = KrailTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
fun TripSearchListItemPreview() {
    PreviewTheme {
        val previewStops = kotlinx.collections.immutable.persistentListOf(
            SearchStopState.TripStop(
                stopId = "214733",
                stopName = "Seven Hills Station",
                stopSequence = 0,
                transportModeType = kotlinx.collections.immutable.persistentListOf(
                    TransportMode.Bus()
                ),
            ),
            SearchStopState.TripStop(
                stopId = "214794",
                stopName = "Blacktown Station",
                stopSequence = 1,
                transportModeType = kotlinx.collections.immutable.persistentListOf(
                    TransportMode.Bus()
                ),
            ),
            SearchStopState.TripStop(
                stopId = "214800",
                stopName = "Windsor Road Stop",
                stopSequence = 2,
                transportModeType = kotlinx.collections.immutable.persistentListOf(
                    TransportMode.Bus()
                ),
            ),
        )

        TripSearchListItem(
            trip = SearchStopState.SearchResult.Trip(
                routeShortName = "702",
                headsign = "Blacktown to Seven Hills",
                stops = previewStops,
            ),
        )
    }
}
