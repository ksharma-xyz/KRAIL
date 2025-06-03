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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.RoundIconButton
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
    val localDensity = LocalDensity.current
    var buttonSizePx by remember { mutableStateOf(IntSize.Zero) }
    val buttonOffsetY = with(localDensity) { (buttonSizePx.height / 2).toDp() }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)) // Apply clipping to the whole column
                .klickable(onClick = onCardClick) // Apply klickable to the whole column
        ) {

            // Top part of the card (Trip Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = primaryTransportMode?.let { transportModeBackgroundColor(it) }
                            ?: themeBackgroundColor(),
                    )
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp, // Standard padding
                        start = 12.dp,
                        end = 12.dp
                    ),
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

                // Star icon Box
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
                    .height(50.dp) // Or wrapContentHeight if dynamic content
                    .background(color = themeColor()) // Dark green bar
                    .padding(
                        start = 12.dp,
                        end = 12.dp + with(localDensity) { buttonSizePx.width.toDp() } + 8.dp // Make space for button
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Content for the bottom bar, e.g., "Park & Ride info here"
                // Text(
                //     "Park & Ride info here",
                //     color = getForegroundColor(themeColor()),
                //     style = KrailTheme.typography.labelSmall
                // )
            }
        }

        // Expand/Collapse Button (Down Arrow)
        // Positioned to overlap and be on top
        RoundIconButton(
            onClick = onExpandClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .onSizeChanged { buttonSizePx = it }
                .offset(
                    x = (-16).dp,
                    y = buttonOffsetY // Offset by half its height
                )
            // No zIndex needed here if it's the last child of the Box and drawn on top
            ,
            color = themeColor(), // Match the bottom bar color
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = "Expand",
                colorFilter = ColorFilter.tint(getForegroundColor(themeColor()))
            )
        }
    }
}


// region Previews

@Preview
@Composable
private fun SavedTripCardPreview_Fixed() {
    KrailTheme {
        val themeColorState = remember { mutableStateOf(KrailThemeStyle.Bus.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColorState) {
            Box(modifier = Modifier.padding(16.dp)) { // Added padding for preview visibility
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
                    onExpandClick = {}, // Added
                    modifier = Modifier.background(color = KrailTheme.colors.surface),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedTripCardListPreview_Fixed() {
    val themeColorState = remember { mutableStateOf(KrailThemeStyle.Metro.hexColorCode) }
    CompositionLocalProvider(LocalThemeColor provides themeColorState) {
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
                    onExpandClick = {}
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
                    onExpandClick = {}
                )
            }
        }
    }
}
// endregion