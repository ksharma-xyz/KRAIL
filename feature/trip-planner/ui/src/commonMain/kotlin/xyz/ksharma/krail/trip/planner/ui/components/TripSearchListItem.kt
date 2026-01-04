package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * State for controlling Trip search list item expansion
 */
enum class TripSearchListItemState {
    COLLAPSED,
    EXPANDED
}

/**
 * Displays a trip search result with expandable/collapsible stops list.
 * Shows headsign/direction and summary when collapsed.
 * Shows all stops when expanded.
 * Each Trip result represents a single direction (e.g., "Blacktown to Parramatta").
 *
 * @param trip The trip data to display
 * @param itemState External state controlling expansion (for analytics tracking)
 * @param onCardClick Callback when card is clicked to toggle expansion
 * @param onStopClick Callback when a stop is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun TripSearchListItem(
    trip: SearchStopState.SearchResult.Trip,
    itemState: TripSearchListItemState,
    onCardClick: () -> Unit,
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
            .animateContentSize(), // Smooth expand/collapse animation
    ) {
        // Header - always visible, clickable to expand/collapse
        TripCardHeader(
            trip = trip,
            itemState = itemState,
            onClick = onCardClick,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
        )

        // Expandable content - stops list
        when (itemState) {
            TripSearchListItemState.COLLAPSED -> {
                // Show summary when collapsed
                CollapsedTripContent(
                    stopCount = trip.stops.size,
                    onClick = onCardClick,
                )
            }

            TripSearchListItemState.EXPANDED -> {
                // Show all stops when expanded
                ExpandedTripContent(
                    stops = trip.stops,
                    onStopClick = onStopClick,
                )
            }
        }
    }
}

/**
 * Trip card header with route badge, headsign, and expand/collapse icon
 */
@Composable
private fun TripCardHeader(
    trip: SearchStopState.SearchResult.Trip,
    itemState: TripSearchListItemState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Animate rotation of the arrow icon
    val collapseIconRotationAngle by animateFloatAsState(
        targetValue = if (itemState == TripSearchListItemState.EXPANDED) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "collapse_expand_arrow_rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable(onClick = onClick, indication = null),
        verticalAlignment = Alignment.Top,
    ) {
        TransportModeBadge(
            badgeText = trip.routeShortName,
            backgroundColor = TransportMode.Bus().colorCode.hexToComposeColor(),
            modifier = Modifier.padding(end = 6.dp)
        )

        TransportModeIcon(
            transportMode = TransportMode.Bus(),
            size = TransportModeIconSize.Small,
        )

        // Trip headsign as title
        Text(
            text = trip.headsign,
            style = KrailTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp, start = 8.dp),
        )

        // Expand/Collapse icon with rotation animation
        Box(
            modifier = Modifier.size(32.dp)
                .clip(CircleShape)
                .background(color = KrailTheme.colors.onSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown, // Always use down arrow, rotation handles the visual change
                tint = KrailTheme.colors.surface,
                contentDescription = if (itemState == TripSearchListItemState.EXPANDED) "Collapse" else "Expand",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(collapseIconRotationAngle), // Animated rotation
            )
        }
    }
}

/**
 * Collapsed trip content - shows summary
 */
@Composable
private fun CollapsedTripContent(
    stopCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .klickable(
                onClick = onClick,
                indication = null,
            )
            .padding(bottom = 16.dp).padding(horizontal = 16.dp),
    ) {
        val buttonContainerColor by remember {
            mutableStateOf(TransportMode.Bus().colorCode.hexToComposeColor())
        }

        Button(
            colors = ButtonColors(
                containerColor = buttonContainerColor,
                contentColor = Color.White,
                disabledContainerColor = buttonContainerColor.copy(alpha = DisabledContentAlpha),
                disabledContentColor = Color.White.copy(alpha = DisabledContentAlpha),
            ),
            onClick = onClick,
            dimensions = ButtonDefaults.smallButtonSize(),
        ) {
            Text(text = "$stopCount stops")
        }
    }
}

/**
 * Expanded trip content - shows all stops
 */
@Composable
private fun ExpandedTripContent(
    stops: List<SearchStopState.TripStop>,
    modifier: Modifier = Modifier,
    onStopClick: (StopItem) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Display all stops
        stops.forEachIndexed { index, stop ->
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            TripStopItem(
                stopName = stop.stopName,
                stopId = stop.stopId,
                onClick = {
                    onStopClick(
                        StopItem(stopId = stop.stopId, stopName = stop.stopName)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Individual stop item within a trip result
 */
@Composable
private fun TripStopItem(
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
        verticalAlignment = Alignment.Top,
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
fun TripSearchListItemCollapsedPreview() {
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Collapsed state
            TripSearchListItem(
                trip = SearchStopState.SearchResult.Trip(
                    routeShortName = "702",
                    headsign = "Blacktown to Seven Hills",
                    stops = previewStops,
                ),
                itemState = TripSearchListItemState.COLLAPSED,
                onCardClick = {},
            )

            // Expanded state
            TripSearchListItem(
                trip = SearchStopState.SearchResult.Trip(
                    routeShortName = "702",
                    headsign = "Blacktown to Seven Hills",
                    stops = previewStops,
                ),
                itemState = TripSearchListItemState.EXPANDED,
                onCardClick = {},
            )
        }
    }
}
