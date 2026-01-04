package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Displays a route search result with all its stops.
 * Shows headsign/direction at top, then ordered list of stops.
 * Each Route result now represents a single direction (e.g., "Blacktown to Parramatta").
 */
@Composable
fun RouteSearchListItem(
    route: SearchStopState.SearchResult.Route,
    modifier: Modifier = Modifier,
    onStopClick: (StopItem) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = KrailTheme.colors.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp),
    ) {
        // Get the headsign from the first variant's first trip
        val headsign = route.variants.firstOrNull()?.trips?.firstOrNull()?.headsign
            ?: route.variants.firstOrNull()?.routeName
            ?: "Route ${route.routeShortName}"

        // Route header with headsign
        Text(
            text = headsign,
            style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = KrailTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Route number subtitle
        Text(
            text = "Route ${route.routeShortName}",
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
        )

        Spacer(modifier = Modifier.padding(vertical = 4.dp))

        // Get all stops from this direction (should be only one trip per route now)
        val allStops = route.variants.flatMap { variant ->
            variant.trips.flatMap { trip ->
                trip.stops
            }
        }.distinctBy { it.stopId } // Remove duplicates if any
            .sortedBy { it.stopSequence } // Sort by sequence

        // Display each stop
        allStops.forEachIndexed { index, stop ->
            RouteStopItem(
                stopName = stop.stopName,
                stopId = stop.stopId,
                onClick = {
                    onStopClick(
                        StopItem(
                            stopId = stop.stopId,
                            stopName = stop.stopName,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Add divider between stops (but not after the last one)
            if (index < allStops.size - 1) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stop name on the left
        Text(
            text = stopName,
            style = KrailTheme.typography.bodyLarge,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Stop ID on the right
        Text(
            text = stopId,
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Preview
@Composable
fun RouteSearchListItemPreview() {
    PreviewTheme {
        RouteSearchListItem(
            route = SearchStopState.SearchResult.Route(
                routeShortName = "702",
                variants = kotlinx.collections.immutable.persistentListOf(
                    SearchStopState.RouteVariant(
                        routeId = "2504_702",
                        routeName = "Blacktown to Seven Hills",
                        trips = kotlinx.collections.immutable.persistentListOf(
                            SearchStopState.TripOption(
                                tripId = "2233187",
                                headsign = "Blacktown to Seven Hills",
                                stops = kotlinx.collections.immutable.persistentListOf(
                                    SearchStopState.TripStop(
                                        stopId = "214733",
                                        stopName = "Seven Hills Station",
                                        stopSequence = 0,
                                        transportModeType = kotlinx.collections.immutable.persistentListOf(
                                            xyz.ksharma.krail.trip.planner.ui.state.TransportMode.Bus()
                                        ),
                                    ),
                                    SearchStopState.TripStop(
                                        stopId = "214794",
                                        stopName = "Blacktown Station",
                                        stopSequence = 1,
                                        transportModeType = kotlinx.collections.immutable.persistentListOf(
                                            xyz.ksharma.krail.trip.planner.ui.state.TransportMode.Bus()
                                        ),
                                    ),
                                    SearchStopState.TripStop(
                                        stopId = "214800",
                                        stopName = "Windsor Road Stop",
                                        stopSequence = 2,
                                        transportModeType = kotlinx.collections.immutable.persistentListOf(
                                            xyz.ksharma.krail.trip.planner.ui.state.TransportMode.Bus()
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
