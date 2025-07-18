package xyz.ksharma.krail.core.festival.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.ksharma.krail.core.festival.FestivalManager

@Serializable
sealed class Festival {
    abstract val type: String
    abstract val emojiList: List<String>
    abstract val greeting: String
}

val Festival.greetingAndEmoji: Pair<String, String>
    get() = Pair(greeting, emojiList.random())

@Serializable
data class FestivalData(
    @SerialName("confirmedDates")
    val confirmedDates: List<FixedDateFestival> = emptyList(),

    @SerialName("variableDates")
    val variableDates: List<VariableDateFestival> = emptyList(),
)

@Serializable
data class FixedDateFestival(
    @SerialName("type") override val type: String,
    @SerialName("month") val month: Int,
    @SerialName("day") val day: Int,
    @SerialName("emojiList") override val emojiList: List<String>,
    @SerialName("greeting") override val greeting: String,
) : Festival()

@Serializable
data class VariableDateFestival(
    @SerialName("type") override val type: String,

    /**
     * Festival start date in ISO 8601 format (YYYY-MM-DD). ISO 8601 format
     * see [kotlinx.datetime.LocalDate.Formats.ISO]
     */
    @SerialName("startDate") val startDate: String,

    /**
     * Festival end date in ISO 8601 format (YYYY-MM-DD). ISO 8601 format
     */
    @SerialName("endDate") val endDate: String,

    @SerialName("emojiList") override val emojiList: List<String>,

    @SerialName("greeting") override val greeting: String,
) : Festival()

data class NoFestival(
    override val type: String = "NoFestival",
    override val emojiList: List<String> = FestivalManager.commonEmojiList.toList(),
    override val greeting: String = "Hop on, mate!",
) : Festival()
