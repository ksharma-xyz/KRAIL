package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
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
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun SavedTripCard(
    trip: Trip,
    primaryTransportMode: TransportMode? = null,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    onLoadParkRideClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expandParkRideCard by remember { mutableStateOf(false) }

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

            if (trip.parkRideUiState is ParkRideUiState.Available ||
                trip.parkRideUiState is ParkRideUiState.Loading ||
                trip.parkRideUiState is ParkRideUiState.Loaded ||
                trip.parkRideUiState is ParkRideUiState.Error
            ) {
                // Bottom bar part
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 32.dp)
                        .heightIn(min = 16.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(color = themeColor())
                        .animateContentSize()
                        .klickable {
                            expandParkRideCard = !expandParkRideCard
                            if (expandParkRideCard) {
                                onLoadParkRideClick()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // If you have content here
                ) {
                    if (expandParkRideCard) {
                        SavedTripParkRideContent(trip.parkRideUiState)
                    }
                }

                // Empty blank area required for dropdown button bottom part.
                Spacer(
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clickable(
                            enabled = false,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}, // Noop here
                        )
                )
            }
        }

        if (trip.parkRideUiState is ParkRideUiState.Available ||
            trip.parkRideUiState is ParkRideUiState.Loading ||
            trip.parkRideUiState is ParkRideUiState.Loaded ||
            trip.parkRideUiState is ParkRideUiState.Error
        ) {
            val scope = rememberCoroutineScope()
            val rotation = remember { Animatable(0f) }
            Box(
                modifier = Modifier
                    .padding(end = 16.dp, top = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(themeColor())
                    .align(Alignment.BottomEnd)
                    .klickable {
                        scope.launch {
                            val current = rotation.value % 360f
                            val target =
                                if (current == 0f) 540f else 0f // 0 -> 540 (360+180), 180 -> 0
                            rotation.animateTo(
                                targetValue = target,
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = { fraction -> (1 - (1 - fraction) * (1 - fraction)) }
                                )
                            )
                        }
                        // TODO - should be a single lambda and reuse the same onClick for both
                        expandParkRideCard = !expandParkRideCard
                        if (expandParkRideCard) {
                            onLoadParkRideClick()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val rotationAngle by animateFloatAsState(
                    targetValue = if (expandParkRideCard) 180f else 0f,
                )

                Image(
                    painter = painterResource(Res.drawable.ic_arrow_down),
                    contentDescription = "Expand",
                    colorFilter = ColorFilter.tint(getForegroundColor(themeColor())),
                    modifier = Modifier.size(20.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                        },
                )
            }
        }
    }
}

@Composable
private fun SavedTripParkRideContent(
    parkRideUiState: ParkRideUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        when (parkRideUiState) {
            is ParkRideUiState.Loading -> {
                Text("Loading Park & Ride...", color = Color.Black)
            }

            is ParkRideUiState.Error -> {
                Text("Error: ${parkRideUiState.message}", color = Color.Red)
            }

            is ParkRideUiState.Loaded -> {
                parkRideUiState.parkRideList.forEach {
                    Text(
                        text = it.facilityName,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Total Spots: ${it.totalSpots}, " +
                                "\nAvailable Spots: ${it.spotsAvailable}, " +
                                "\nPercentage Full: ${it.percentageFull}%, " +
                                "\nLast updated: ${it.timeText}, ",
                                color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            is ParkRideUiState.Available -> {
                Text("Tap to load Park & Ride details", color = Color.Black)
            }

            is ParkRideUiState.NotAvailable -> {
                // Should not be shown, but you can handle if needed
            }
        }
    }
}

// region Previews

@Preview
@Composable
private fun SavedTripCardPreview_Single() {
    KrailTheme {
        val themeColorState =
            remember { mutableStateOf(KrailThemeStyle.Bus.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(KrailTheme.colors.surface)
            ) {
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "1",
                        fromStopName = "Tallawong Station",
                        toStopId = "2",
                        toStopName = "Central Station",
                    ),
                    primaryTransportMode = TransportMode.Metro(),
                    onCardClick = {},
                    onStarClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedTripCardListPreview_List() {
    KrailTheme {
        val themeColorState =
            remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
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

                // You can add more cards here to test list behavior
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "3",
                        fromStopName = "Another Station",
                        toStopId = "4",
                        toStopName = "Destination X",
                        parkRideUiState = ParkRideUiState.Loaded(
                            parkRideList = listOf(parkRideStatePreview).toImmutableList()
                        ),
                    ),
                    primaryTransportMode = TransportMode.Bus(),
                    onCardClick = {},
                    onStarClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedTripCardPreview_Single_ParkRideAvailable() {
    KrailTheme {
        val themeColorState =
            remember { mutableStateOf(KrailThemeStyle.Bus.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(KrailTheme.colors.surface)
            ) {
                SavedTripCard(
                    trip = Trip(
                        fromStopId = "1",
                        fromStopName = "Tallawong Station",
                        toStopId = "2",
                        toStopName = "Central Station",
                        parkRideUiState = ParkRideUiState.Loaded(
                            parkRideList = listOf(parkRideStatePreview).toImmutableList()
                        ),
                    ),
                    primaryTransportMode = TransportMode.Metro(),
                    onCardClick = {},
                    onStarClick = {},
                )
            }
        }
    }
}

private val parkRideStatePreview = ParkRideState(
    facilityName = "Park & Ride Facility",
    spotsAvailable = 50,
    totalSpots = 100,
    percentageFull = 50,
    stopId = "12345",
    timeText = "10:30 AM",
)

// endregion
