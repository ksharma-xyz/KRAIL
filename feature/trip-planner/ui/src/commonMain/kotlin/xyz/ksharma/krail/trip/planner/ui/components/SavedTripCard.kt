package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun SavedTripCard(
    trip: Trip,
    primaryTransportMode: TransportMode?,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .cardBackground()
            .klickable(onClick = onCardClick)
            .padding(vertical = dim.spacingXL, horizontal = dim.spacingXL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        primaryTransportMode?.let {
            TransportModeIcon(
                transportMode = primaryTransportMode,
                modifier = Modifier.padding(end = dim.cardInternalSpacing),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(dim.cardInternalSpacing),
        ) {
            Text(text = trip.fromStopName, style = KrailTheme.typography.bodyMedium)
            Text(text = trip.toStopName, style = KrailTheme.typography.bodyMedium)
        }

        Box(
            modifier = Modifier
                .size(dim.savedTripIconButtonSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Remove Saved Trip",
                    role = Role.Button,
                    onClick = onStarClick,
                )
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_star_filled),
                contentDescription = "Save Trip",
                colorFilter = ColorFilter.tint(
                    if (isAppInDarkMode().not()) {
                        primaryTransportMode?.let { NswTransportConfig.colorFor(it) }
                            ?.hexToComposeColor()
                            ?: themeColor()
                    } else {
                        KrailTheme.colors.onSurface
                    },
                ),
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun SavedTripCardPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            trip = Trip(
                fromStopId = "1",
                fromStopName = "Edmondson Park Station",
                toStopId = "2",
                toStopName = "Harris Park Station",
            ),
            primaryTransportMode = TransportMode.Train,
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@Preview
@Composable
private fun SavedTripCardListPreview() {
    PreviewTheme {
        val dim = KrailTheme.dimensions
        Column(
            modifier = Modifier.padding(dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Edmondson Park Station",
                    toStopId = "2",
                    toStopName = "Harris Park Station",
                ),
                primaryTransportMode = TransportMode.Train,
                onCardClick = {},
                onStarClick = {},
            )

            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Harrington Street, Stand D",
                    toStopId = "2",
                    toStopName = "Albert Rd, Stand A",
                ),
                primaryTransportMode = TransportMode.Bus,
                onCardClick = {},
                onStarClick = {},
            )

            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Manly Wharf",
                    toStopId = "2",
                    toStopName = "Circular Quay Wharf",
                ),
                primaryTransportMode = TransportMode.Ferry,
                onCardClick = {},
                onStarClick = {},
            )

            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Central Station",
                    toStopId = "2",
                    toStopName = "Town Hall Station",
                ),
                primaryTransportMode = TransportMode.Metro,
                onCardClick = {},
                onStarClick = {},
            )
        }
    }
}

// endregion
