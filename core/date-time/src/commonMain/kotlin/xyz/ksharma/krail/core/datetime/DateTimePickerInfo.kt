package xyz.ksharma.krail.core.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class DateTimePickerInfo(
    val date: String,
    val hour: Int,
    val minute: Int,
)

@OptIn(ExperimentalTime::class)
@Composable
fun rememberCurrentDateTime(): DateTimePickerInfo {
    val currentTime = Clock.System.now()
    val currentDateTime = remember {
        currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    // Formatting the date
    val formattedDate = remember(currentDateTime.date) {
        toReadableDate(currentDateTime.date)
    }

    return DateTimePickerInfo(
        date = formattedDate,
        hour = currentDateTime.hour,
        minute = currentDateTime.minute,
    )
}

fun incrementDateByOneDay(date: LocalDate): LocalDate {
    return date.plus(1, DateTimeUnit.DAY)
}

fun decrementDateByOneDay(date: LocalDate): LocalDate {
    return date.plus(-1, DateTimeUnit.DAY)
}

/**
 * Formats the time in 12-hour format (e.g., 1:30 PM)
 */
fun to12HourTimeString(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    val formattedMinute = if (minute < 10) "0$minute" else minute.toString()
    return "$displayHour:$formattedMinute $amPm"
}

/**
 * Formats the date in a human-readable format (e.g., Today, Tomorrow, Mon 1 Jan)
 */
@OptIn(ExperimentalTime::class)
fun toReadableDate(date: LocalDate): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    return when (date) {
        today -> "Today"
        tomorrow -> "Tomorrow"
        else -> {
            val dayOfWeek = date.dayOfWeek.name.substring(0, 3) // Short day name (e.g., Mon, Tue)
            val month = date.month.name.substring(0, 3) // Short month name (e.g., Jan, Feb)
            "$dayOfWeek, ${date.day} ${month.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
        }
    }
}
