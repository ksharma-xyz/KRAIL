package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.modifier.cardBorder
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun SavedTripCard(
    trip: Trip,
    primaryTransportMode: TransportMode?,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .cardBackground()
            .klickable(onClick = onCardClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        primaryTransportMode?.let {
            TransportModeIcon(
                transportMode = primaryTransportMode,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = trip.fromStopName, style = KrailTheme.typography.bodyMedium)
            Text(text = trip.toStopName, style = KrailTheme.typography.bodyMedium)
        }

        Box(
            modifier = Modifier
                .size(44.dp)
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
                    if (isSystemInDarkTheme().not()) {
                        primaryTransportMode?.colorCode
                            ?.hexToComposeColor() ?: themeColor()
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
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Bus().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Edmondson Park Station",
                    toStopId = "2",
                    toStopName = "Harris Park Station",
                ),
                primaryTransportMode = TransportMode.Train(),
                onCardClick = {},
                onStarClick = {},
                modifier = Modifier.background(color = KrailTheme.colors.surface),
            )
        }
    }
}

@Preview
@Composable
private fun SavedTripCardListPreview() {
    KrailTheme {
        Column(
            modifier = Modifier
                .background(color = KrailTheme.colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SavedTripCard(
                trip = Trip(
                    fromStopId = "1",
                    fromStopName = "Edmondson Park Station",
                    toStopId = "2",
                    toStopName = "Harris Park Station",
                ),
                primaryTransportMode = TransportMode.Train(),
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
                primaryTransportMode = TransportMode.Bus(),
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
                primaryTransportMode = TransportMode.Ferry(),
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
                primaryTransportMode = null,
                onCardClick = {},
                onStarClick = {},
            )
        }
    }
}

// endregion
