package xyz.ksharma.krail.trip.planner.ui.components.loading

import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import xyz.ksharma.krail.core.log.log
import kotlin.random.Random

// TODO - Add better logic
//  1. should be able to create range of dates with exhaustive statements.
//  2. Should be able to fetch festival dates / emoji from remote config rather than hard code.
object LoadingEmojiManager {

    private val commonEmojiList = persistentListOf(
        "🛴",
        "🛹",
        "🚀",
        "🛶",
        "\uD83D\uDC2C", // Dolphin
        "⏰", // Alarm Clock
        "\uD83D\uDEFA", // Auto
        "\uD83D\uDEB2", // Bicycle
    )

    private val rareEmojiList = persistentListOf("🐦‍🔥")

    private val festivalEmojiMap = mapOf(
        FestivalType.CHRISTMAS to listOf("🎄", "\uD83C\uDF85", "❄\uFE0F", "🎁"),
        FestivalType.NEW_YEAR to listOf("🎉"),
        FestivalType.NEW_YEAR_EVE to listOf("🎆"),
        FestivalType.ANZAC_DAY to listOf(
            "🌺", // Flower
            "🇦🇺", // Australia Flag
            "\uD83C\uDF96\uFE0F", // Military Medal
            "\uD83C\uDF3F" // Herb Rosemary
        ),
        FestivalType.EASTER to listOf("🐰", "🐣", "🥚"),
        FestivalType.HALLOWEEN to listOf("🎃", "👻"),
        FestivalType.CHINESE_NEW_YEAR to listOf("🧧"),

        FestivalType.ROSE_DAY to listOf("🌹"),
        FestivalType.PROPOSE_DAY to listOf("💍", "💞", "💌"),
        FestivalType.CHOCOLATE_DAY to listOf("🍫", "💞"),
        FestivalType.TEDDY_DAY to listOf("🧸"),
        FestivalType.PROMISE_DAY to listOf("🤝"),
        FestivalType.HUG_DAY to listOf("🤗"),
        FestivalType.KISS_DAY to listOf("😘"),
        FestivalType.VALENTINES_DAY to listOf("❤️", "🌹"),

        FestivalType.AUSTRALIA_DAY to listOf("🇦🇺", "🎉", "🎆"),
        FestivalType.EID to listOf("🌙", "🕌", "🎁"),
        FestivalType.MARDI_GRAS to listOf("🏳️‍🌈", "🪩", "🌈"),
        FestivalType.VIVID_SYDNEY to listOf("🎆", "🌈", "🌟", "✨"),

        // Human centric days
        FestivalType.WOMENS_DAY to listOf("💜", "♀️", "👩", "👩‍🚀", "👩‍🚒", "👩‍✈️"),
        FestivalType.MENS_DAY to listOf("💙", "♂️", "🚹", "👨‍🚒", "👨‍🌾", "👨‍🚀"),
        FestivalType.ENGINEERS_DAY to listOf("⚙️", "🔧"),
        FestivalType.NURSES_DAY to listOf("🏥", "🩺"),
        FestivalType.FRIENDSHIP_DAY to listOf("🤝", "💛"),
        FestivalType.PEACE_DAY to listOf("☮️", "✌️"),
        FestivalType.A11Y_DAY to listOf("♿️"),
        FestivalType.PI_DAY to listOf("🥧", "π"),
    )

    // TODO - test logic add UT
    data class MonthDay(val month: Int, val dayOfMonth: Int) {
        companion object {
            fun of(month: Int, dayOfMonth: Int) = MonthDay(month, dayOfMonth)
        }
    }

    private val knownFestivalDates = mapOf(
        // Sure dates
        MonthDay.of(12, 25) to FestivalType.CHRISTMAS,
        MonthDay.of(12, 26) to FestivalType.BOXING_DAY,
        MonthDay.of(12, 31) to FestivalType.NEW_YEAR_EVE,
        MonthDay.of(1, 1) to FestivalType.NEW_YEAR,
        MonthDay.of(4, 25) to FestivalType.ANZAC_DAY,
        MonthDay.of(1, 26) to FestivalType.AUSTRALIA_DAY,

        // Valentines day
        MonthDay.of(2, 7) to FestivalType.ROSE_DAY,
        MonthDay.of(2, 8) to FestivalType.PROPOSE_DAY,
        MonthDay.of(2, 9) to FestivalType.CHOCOLATE_DAY,
        MonthDay.of(2, 10) to FestivalType.TEDDY_DAY,
        MonthDay.of(2, 11) to FestivalType.PROMISE_DAY,
        MonthDay.of(2, 12) to FestivalType.HUG_DAY,
        MonthDay.of(2, 13) to FestivalType.KISS_DAY,
        MonthDay.of(2, 14) to FestivalType.VALENTINES_DAY,

        // Special Days
        MonthDay.of(3, 4) to FestivalType.ENGINEERS_DAY,
        MonthDay.of(3, 8) to FestivalType.WOMENS_DAY,
        MonthDay.of(3, 14) to FestivalType.PI_DAY,
        MonthDay.of(5, 12) to FestivalType.NURSES_DAY,
        MonthDay.of(7, 30) to FestivalType.FRIENDSHIP_DAY,
        MonthDay.of(9, 21) to FestivalType.PEACE_DAY,
        MonthDay.of(11, 19) to FestivalType.MENS_DAY,
        MonthDay.of(12, 3) to FestivalType.A11Y_DAY,

        // Can change dates
        MonthDay.of(3, 30) to FestivalType.EID,
        MonthDay.of(3, 31) to FestivalType.EID,

        // Easter 2025
        MonthDay.of(4, 20) to FestivalType.EASTER,

        // Vivid Sydney 2025
        MonthDay.of(5, 23) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 24) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 25) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 26) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 27) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 28) to FestivalType.VIVID_SYDNEY,
        MonthDay.of(5, 29) to FestivalType.VIVID_SYDNEY, // till 14 June
    )

    internal fun getRandomEmoji(overrideEmoji: String? = null): String {
        if (overrideEmoji != null) return overrideEmoji

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        log("Today: $today")

        val festivalEmoji = knownFestivalDates.entries
            .firstOrNull {
                log(
                    "Checking festival date[${it.key.dayOfMonth} / ${it.key.month}] " +
                        "| Comparing to ${today.dayOfMonth} / ${today.month.number}"
                )
                it.key.month == today.month.number && it.key.dayOfMonth == today.dayOfMonth
            }
            ?.let {
                log("Filter result: $it")
                festivalEmojiMap[it.value]?.randomOrNull()
            }

        log("festival Emoji: $festivalEmoji")
        if (festivalEmoji != null) return festivalEmoji

        val randomValue = Random.nextInt(100)
        return when {
            randomValue < 99 -> commonEmojiList.random() // 99% chance for common emojis
            else -> rareEmojiList.random() // 1% chance for the rare emoji
        }
    }
}
