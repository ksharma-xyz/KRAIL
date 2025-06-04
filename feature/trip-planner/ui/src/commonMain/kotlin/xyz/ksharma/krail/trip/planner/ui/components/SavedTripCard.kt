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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun SavedTripCard(
    trip: Trip,
    primaryTransportMode: TransportMode? = null,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    onExpandClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {

            // Top part of the card (Trip Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = primaryTransportMode?.let {
                            transportModeBackgroundColor(it)
                        } ?: themeBackgroundColor(),
                    )
                    .klickable(onClick = onCardClick)
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryTransportMode?.let {
                    TransportModeIcon(transportMode = primaryTransportMode)
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
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
                            } else KrailTheme.colors.onSurface,
                        ),
                    )
                }
            }

            // Bottom bar part
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 32.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(color = themeColor())
                    .klickable { onExpandClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // If you have content here
            ) {
                // Content for the bottom bar
                // Spacer(Modifier.weight(1f)) // If you want to push content to the start
            }

            Spacer(
                modifier = Modifier.fillMaxWidth().height(12.dp)
                    .clickable(
                        enabled = false,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
            )
        }

        Box(
            modifier = Modifier
                .padding(end = 16.dp, top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(themeColor())
                .align(Alignment.BottomEnd)
                .klickable {
                    onExpandClick
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = "Expand",
                colorFilter = ColorFilter.tint(getForegroundColor(themeColor())),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}


// Dummy TransportModeIcon if not available
@Composable
fun TransportModeIcon(transportMode: TransportMode, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = when (transportMode) { // Giving it some actual color based on your screenshots
                    is TransportMode.Train -> Color(0xFFD98A26) // Orange like 'T'
                    is TransportMode.Metro -> Color.Gray // Gray for 'M'
                    else -> Color.LightGray
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (transportMode) {
                is TransportMode.Bus -> "B"
                is TransportMode.Train -> "T"
                is TransportMode.Metro -> "M"
                else -> "?"
            },
            color = Color.White, // Ensuring text is visible on colored backgrounds
            style = KrailTheme.typography.bodyMedium
        )
    }
}


// region Previews

@Preview
@Composable
private fun SavedTripCardPreview_Fixed() {
    KrailTheme {
        // This themeColor (light blue/teal) will be used for the bottom bar/button
        val themeColorState =
            remember { mutableStateOf(KrailThemeStyle.Bus.hexColorCode) } // Bus theme is light blue/teal
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(KrailTheme.colors.surface) // Use theme background for the box
            ) {
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "1",
                        fromStopName = "Tallawong Station",
                        toStopId = "2",
                        toStopName = "Central Station",
                    ),
                    primaryTransportMode = TransportMode.Metro(), // Top part BG = Metro, Icon = 'M'
                    onCardClick = {},
                    onStarClick = {},
                    onExpandClick = {},
                    modifier = Modifier, // Modifier for SavedTripCard itself
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedTripCardListPreview_Fixed() {
    KrailTheme {
        // This themeColor (orange) will be used for the bottom bar/button for ALL cards in this list
        val themeColorState =
            remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) } // Train theme is orange
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
            Column(
                modifier = Modifier
                    .background(color = KrailTheme.colors.surface) // Use theme surface for Column BG
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "1",
                        fromStopName = "Edmondson Park Station",
                        toStopId = "2",
                        toStopName = "Harris Park Station",
                    ),
                    primaryTransportMode = TransportMode.Train(), // Top part BG = Train, Icon = 'T'
                    onCardClick = {},
                    onStarClick = {},
                    onExpandClick = {}
                )

                // You can add more cards here to test list behavior
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "3",
                        fromStopName = "Another Station",
                        toStopId = "4",
                        toStopName = "Destination X",
                    ),
                    primaryTransportMode = TransportMode.Bus(), // Top part BG = Bus
                    onCardClick = {},
                    onStarClick = {},
                    onExpandClick = {}
                )
            }
        }
    }
}
// endregion