package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.datetime.decrementDateByOneDay
import xyz.ksharma.krail.core.datetime.incrementDateByOneDay
import xyz.ksharma.krail.core.datetime.rememberCurrentDateTime
import xyz.ksharma.krail.core.datetime.toReadableDate
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelectorScreen(
    dateTimeSelection: DateTimeSelectionItem?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onDateTimeSelected: (DateTimeSelectionItem?) -> Unit = {},
    onResetClick: () -> Unit = {},
) {
    // Colors
    val themeColorHex by LocalThemeColor.current
    val themeColor = remember(themeColorHex) { themeColorHex.hexToComposeColor() }

    // Journey Time Options
    var journeyTimeOption by rememberSaveable {
        mutableStateOf(dateTimeSelection?.option ?: JourneyTimeOptions.LEAVE)
    }

    // Time Selection
    val currentDateTime = rememberCurrentDateTime()
    val timePickerState = rememberTimePickerState(
        initialHour = dateTimeSelection?.hour ?: currentDateTime.hour,
        initialMinute = dateTimeSelection?.minute ?: currentDateTime.minute,
        is24Hour = false,
    )

    // region Date selection variables
    // Cannot use rememberSaveable for LocalDate, so using String
    val todayDateStr: String =
        rememberSaveable {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }
    val today = LocalDate.parse(todayDateStr)

    // Maximum Future Date
    val maxDateStr = rememberSaveable(today) { today.plus(7, DateTimeUnit.DAY).toString() }
    val maxDate = LocalDate.parse(maxDateStr)

    // Selected Date
    var selectedDateStr: String by rememberSaveable(dateTimeSelection) {
        mutableStateOf(
            dateTimeSelection?.date?.toString() ?: today.toString()
        )
    }
    val selectedDate = LocalDate.parse(selectedDateStr)
    // endregion Date selection Variables

    // Reset
    // when dateTimeSelection is null then coming to this screen for first time, so reset should be true.
    var reset: Boolean by rememberSaveable { mutableStateOf(dateTimeSelection == null) }
    LaunchedEffect(timePickerState.hour, timePickerState.minute, journeyTimeOption, selectedDate) {
        val now: LocalDateTime =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // If any of the date / time value changes, then reset is invalid.
        // Should be true - when time and date is same even after updates, coz reset click
        // triggered this change.
        reset = now.hour == timePickerState.hour &&
            now.minute == timePickerState.minute &&
            selectedDate == now.date &&
            journeyTimeOption == JourneyTimeOptions.LEAVE
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .systemBarsPadding(),
    ) {
        TitleBar(
            title = { Text(text = "Plan your trip") },
            onNavActionClick = onBackClick,
            actions = {
                TextButton(
                    onClick = {
                        val now: LocalDateTime = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        selectedDateStr = now.date.toString()
                        timePickerState.hour = now.time.hour
                        timePickerState.minute = now.time.minute
                        journeyTimeOption = JourneyTimeOptions.LEAVE
                        reset = true
                        onResetClick()
                    },
                    dimensions = ButtonDefaults.largeButtonSize(),
                ) {
                    Text("Reset")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                JourneyTimeOptionsGroup(
                    selectedOption = journeyTimeOption,
                    themeColor = themeColor,
                    onOptionSelected = {
                        journeyTimeOption = it
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                DateSelection(
                    themeColor = themeColor,
                    date = toReadableDate(selectedDate),
                    onNextClicked = {
                        if (selectedDate < maxDate) {
                            selectedDateStr = incrementDateByOneDay(selectedDate).toString()
                        }
                    },
                    onPreviousClicked = {
                        if (selectedDate > today) {
                            selectedDateStr = decrementDateByOneDay(selectedDate).toString()
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                )
            }

            item {
                TimeSelection(
                    timePickerState = timePickerState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }

            item {
                Button(
                    onClick = {
                        onDateTimeSelected(
                            if (reset) {
                                null
                            } else {
                                DateTimeSelectionItem(
                                    option = journeyTimeOption,
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute,
                                    date = selectedDate,
                                )
                            }
                        )
                    },
                    dimensions = ButtonDefaults.largeButtonSize(),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = if (reset) {
                            "Leave Now"
                        } else {
                            DateTimeSelectionItem(
                                option = journeyTimeOption,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                date = selectedDate,
                            ).toDateTimeText()
                        },
                    )
                }
            }
        }
    }
}
