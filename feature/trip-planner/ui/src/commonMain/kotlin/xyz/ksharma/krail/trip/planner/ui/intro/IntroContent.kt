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
                    modifier = Modifier.align(Alignment.CenterHorizontally),
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
internal  fun TagLineWithEmoji(
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
            style = KrailTheme.typography.introTagline,
            color = emojiColor,
        )

        Text(
            text = tagline,
            style = KrailTheme.typography.introTagline,
            color = tagColor,
        )
    }
}
