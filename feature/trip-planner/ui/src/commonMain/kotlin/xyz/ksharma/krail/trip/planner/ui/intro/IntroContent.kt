package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.alerts.CollapsibleAlert
import xyz.ksharma.krail.trip.planner.ui.components.LegView
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.ActionButton

val trip = Trip(
    fromStopName = "Wynyard Station",
    toStopName = "Central Station",
    fromStopId = "1",
    toStopId = "2"
)

@Composable
fun IntroContentSaveTrips(
    tagline: String,
    modifier: Modifier = Modifier,
    style: String, // hexCode - // todo - see if it can be color instead.
) {
    Column(
        modifier = modifier.padding(vertical = 20.dp, horizontal = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OriginDestination(
                trip = trip,
                timeLineColor = KrailTheme.colors.onSurface,
            )

            Divider()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TODO if tapped more than 5 times, show confetti of stars.
            var isTripSaved by rememberSaveable { mutableStateOf(false) }
            ActionButton(
                onClick = { isTripSaved = !isTripSaved },
                contentDescription = if (isTripSaved) {
                    "Remove Saved Trip"
                } else {
                    "Save Trip"
                },
                modifier = Modifier.size(80.dp),
            ) {
                Image(
                    painter = if (isTripSaved) {
                        painterResource(Res.drawable.ic_star_filled)
                    } else {
                        painterResource(Res.drawable.ic_star)
                    },
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(style.hexToComposeColor()),
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83C\uDF1F")
    }
}

@Composable
fun IntroContentRealTime(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegView(
                routeText = "Central to Dulwich Hill",
                transportModeLine = TransportModeLine(
                    transportMode = TransportMode.LightRail(),
                    lineName = "L1",
                ),
                stops = stopsList(),
            )
        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83D\uDE80")
    }
}

@Composable
fun IntroContentAlerts(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            var displayAlert by rememberSaveable { mutableStateOf(false) }

            AlertButton(
                dimensions = ButtonDefaults.smallButtonSize(),
                onClick = { displayAlert = !displayAlert },
            ) { Text(text = "2 Alerts") }

            AnimatedAlerts(displayAlert)
        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83D\uDE80")
    }
}

@Composable
fun AnimatedAlerts(displayAlert: Boolean) {
    AnimatedVisibility(
        visible = displayAlert,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = tween(300),
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { -20 },
                    animationSpec = tween(300),
                )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Running late. Please allow extra travel time.", message = "",
                ),
                index = 1,
                collapsed = false,
                onClick = {},
            )

            CollapsibleAlert(
                serviceAlert = ServiceAlert(
                    heading = "Platforms may change, listen for announcements.", message = "",
                ),
                index = 2,
                collapsed = false,
                onClick = {},
            )
        }
    }
}

@Composable
fun IntroContentPlanTrip(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {


        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83D\uDE80")
    }
}

@Composable
fun IntroContentInviteFriends(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("RIDING SOLO\nNAH THANKS", style = KrailTheme.typography.headlineMedium)

        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83D\uDE80")
    }
}

@Composable
fun IntroContentSelectMode(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        }

        TagLineWithEmoji(tagline = tagline, emoji = "\uD83D\uDE80")
    }
}


@Composable
private fun TagLineWithEmoji(tagline: String, emoji: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(text = emoji, style = KrailTheme.typography.displayMedium)

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
        )
    }
}

private fun stopsList() = persistentListOf(
    TimeTableState.JourneyCardInfo.Stop(
        name = "Central Light Rail",
        time = "10:10 AM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Capitol Square Light Rail",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Paddy's Market Light Rail",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Central Station",
        time = "12:00",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        "Glebe Light Rail",
        time = "10:15 AM",
        isWheelchairAccessible = true,
    ),
)