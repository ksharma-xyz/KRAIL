package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_ios_share
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.datetime.rememberCurrentDateTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.alerts.CollapsibleAlert
import xyz.ksharma.krail.trip.planner.ui.components.LegView
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.JourneyTimeOptionsGroup
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.TimeSelection
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
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
        modifier = modifier
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

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83C\uDF1F",
            tagColor = style.hexToComposeColor()
        )
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegView(
                routeText = "Manly to Circular Quay",
                transportModeLine = TransportModeLine(
                    transportMode = TransportMode.Ferry(),
                    lineName = "MFF",
                ),
                stops = stopsList(),
            )
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "\uD83D\uDE80",
            tagColor = style.hexToComposeColor()
        )
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            var displayAlert by rememberSaveable { mutableStateOf(false) }

            AlertButton(
                dimensions = ButtonDefaults.smallButtonSize(),
                onClick = { displayAlert = !displayAlert },
            ) { Text(text = "2 Alerts") }

            AnimatedAlerts(displayAlert)
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "⚠", emojiColor = style.hexToComposeColor(),
            tagColor = style.hexToComposeColor()
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroContentPlanTrip(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var journeyTimeOption by remember { mutableStateOf(JourneyTimeOptions.LEAVE) }
            val currentDateTime = rememberCurrentDateTime()
            val timePickerState = rememberTimePickerState(
                initialHour = currentDateTime.hour,
                initialMinute = currentDateTime.minute,
                is24Hour = false,
            )

            JourneyTimeOptionsGroup(
                selectedOption = journeyTimeOption,
                themeColor = style.hexToComposeColor(),
                onOptionSelected = { journeyTimeOption = it },
            )
            val themeColor = rememberSaveable { mutableStateOf(TransportMode.Coach().colorCode) }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalThemeColor provides themeColor,
                LocalDensity provides Density(
                    (density.density - 0.6f).coerceIn(1.5f, 3f),
                    fontScale = 1f
                ),
            ) {
                TimeSelection(
                    timePickerState = timePickerState,
                    displayTitle = false,
                    modifier = Modifier.padding(vertical = 12.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "\uD83D\uDD2E",
            tagColor = style.hexToComposeColor()
        )
    }
}

@Composable
fun IntroContentInviteFriends(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "RIDING SOLO\nNAH THANKS",
                style = KrailTheme.typography.headlineMedium,
                color = style.hexToComposeColor(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color = style.hexToComposeColor())
                    .clickable(
                        indication = null,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onShareClick
                    ),
                contentAlignment = Alignment.Center,
            ) { // TODO - show diff. image for ios / android
                Image(
                    painter = painterResource(Res.drawable.ic_ios_share),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline, emoji = "\uD83D\uDC95",
            tagColor = style.hexToComposeColor()
        )
    }
}

@Composable
fun IntroContentSelectTransportMode(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var selectedModes by rememberSaveable { mutableStateOf(setOf<TransportMode>()) }

            TransportMode.values().forEach { mode ->
                TransportModeChip(
                    transportMode = mode,
                    selected = selectedModes.contains(mode),
                    onClick = {
                        selectedModes = if (selectedModes.contains(mode)) {
                            selectedModes - mode
                        } else selectedModes + mode
                    },
                    //modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83D\uDE0E",
            tagColor = style.hexToComposeColor()
        )
    }
}


@Composable
private fun TagLineWithEmoji(
    tagline: String,
    emoji: String,
    emojiColor: Color? = null,
    tagColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 20.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emoji,
            style = KrailTheme.typography.displayMedium,
            color = emojiColor,
        )

        Text(
            text = tagline,
            style = KrailTheme.typography.displayMedium,
            color = tagColor,
        )
    }
}

private fun stopsList() = persistentListOf(
    TimeTableState.JourneyCardInfo.Stop(
        name = "Manly, Wharf 2",
        time = "10:10 AM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Circular Quay, Wharf 2",
        time = "10:30 AM",
        isWheelchairAccessible = true,
    ),
)