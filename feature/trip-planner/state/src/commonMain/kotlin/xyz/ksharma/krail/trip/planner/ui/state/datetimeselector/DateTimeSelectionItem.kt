package xyz.ksharma.krail.trip.planner.ui.state.datetimeselector

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.datetime.to12HourTimeString
import xyz.ksharma.krail.core.datetime.toReadableDate

@Serializable
data class DateTimeSelectionItem(
    val option: JourneyTimeOptions,
    val hour: Int,
    val minute: Int,
    val date: LocalDate,
) {
    fun toDateTimeText(): String = when (option) {
        JourneyTimeOptions.LEAVE -> {
            "Leave ${toReadableDate(date)} ${to12HourTimeString(hour, minute)}"
        }

        JourneyTimeOptions.ARRIVE -> {
            "Arrive ${toReadableDate(date)} ${to12HourTimeString(hour, minute)}"
        }
    }

    fun toJsonString() = Json.encodeToString(serializer(), this)

    fun toHHMM(): String {
        val hh = hour.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        return "$hh$mm"
    }

    fun toYYYYMMDD(): String {
        val yyyy = date.year.toString()
        val mm = date.monthNumber.toString().padStart(2, '0')
        val dd = date.dayOfMonth.toString().padStart(2, '0')
        return "$yyyy$mm$dd"
    }

    @Suppress("ConstPropertyName")
    companion object {
        private const val serialVersionUID: Long = 1L

        fun fromJsonString(json: String) =
            kotlin.runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}
