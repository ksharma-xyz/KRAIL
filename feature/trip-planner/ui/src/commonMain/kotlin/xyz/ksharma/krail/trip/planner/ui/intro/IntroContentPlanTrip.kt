package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.krail.core.datetime.rememberCurrentDateTime
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.JourneyTimeOptionsGroup
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.TimeSelection
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroContentPlanTrip(
    tagline: String,
    style: String, // hexCode
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // Only automate if font scale is small
    if (density.fontScale < 1.5f) {
        val allOptions = JourneyTimeOptions.entries.toTypedArray()
        var journeyTimeOption by remember { mutableStateOf(JourneyTimeOptions.LEAVE) }
        val currentDateTime = rememberCurrentDateTime()
        val timeStates = listOf(
            10 to 10, // 10:10 AM
            currentDateTime.hour to currentDateTime.minute // system time
        )
        var timeStep by remember { mutableStateOf(0) }
        val timePickerState = rememberTimePickerState(
            initialHour = timeStates[timeStep].first,
            initialMinute = timeStates[timeStep].second,
            is24Hour = false,
        )

        // Automate toggling
        LaunchedEffect(Unit) {
            var optionStep = 0
            while (true) {
                journeyTimeOption = allOptions[optionStep % allOptions.size]
                timeStep = (optionStep % timeStates.size)
                timePickerState.hour = timeStates[timeStep].first
                timePickerState.minute = timeStates[timeStep].second
                optionStep++
                delay(2500)
            }
        }

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JourneyTimeOptionsGroup(
                    selectedOption = journeyTimeOption,
                    themeColor = style.hexToComposeColor(),
                    onOptionSelected = {
                        journeyTimeOption = it
                    },
                )

                CompositionLocalProvider(
                    LocalThemeColor provides rememberSaveable { mutableStateOf(TransportMode.Coach().colorCode) },
                    LocalDensity provides Density(
                        (density.density - 0.6f).coerceIn(1.5f, 2.5f),
                        fontScale = 1f,
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
                tagline = tagline,
                emoji = "\uD83D\uDD2E",
                tagColor = style.hexToComposeColor()
            )
        }
    }
}
