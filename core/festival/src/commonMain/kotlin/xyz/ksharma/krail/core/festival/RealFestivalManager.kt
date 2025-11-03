package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.festival.model.FestivalData
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue

internal class RealFestivalManager(private val flag: Flag) : FestivalManager {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun emojiForDate(date: LocalDate): String {
        log("Fetching emoji for date: $date")
        val festival = festivalOnDate(date)
        return festival?.emojiList?.random() ?: FestivalManager.commonEmojiList.random().also {
            log("No festival for date:$date, returning common emoji: $it")
        }
    }

    override fun festivalOnDate(date: LocalDate): Festival? {
        log("Checking festival for date: $date")
        val data = getFestivalData() ?: return null
        log("Today's date: $date")

        // Check fixed date festivals first
        data.confirmedDates.firstOrNull { festival ->
            festival.month == date.month.number && festival.day == date.day
        }?.also { festival ->
            log("Fixed date festival found: ${festival.greeting}")
            return festival
        }

        // Check variable date festivals
        data.variableDates.firstOrNull { festival ->
            val startDate = parseDateOrNull(
                date = festival.startDate,
                greeting = festival.greeting,
                tag = "startDate",
            )
            val endDate = parseDateOrNull(
                date = festival.endDate,
                greeting = festival.greeting,
                tag = "endDate",
            )

            when {
                startDate == null || endDate == null -> {
                    log(
                        "Skipping festival '${festival.greeting}' due to invalid date(s): startDate=$startDate, endDate=$endDate",
                    )
                    false
                }

                startDate > endDate -> {
                    logError(
                        "Invalid date range for festival '${festival.greeting}': startDate=$startDate is after endDate=$endDate",
                    )
                    false
                }

                else -> {
                    val inRange = date >= startDate && date <= endDate
                    log(
                        "Checking festival '${festival.greeting}': date=$date, startDate=$startDate, endDate=$endDate, inRange=$inRange",
                    )
                    inRange
                }
            }
        }?.also { festival ->
            log("Variable date festival found: ${festival.greeting}")
            return festival
        }

        log("No festival found for date: $date")
        return null
    }

    private fun getFestivalData(): FestivalData? {
        log("Fetching festivals from remote config")
        val flagValue = flag.getFlagValue(FlagKeys.FESTIVALS.key)
        log("Flag value for festivals: $flagValue")
        return flagValue.toFestivalData().also {
            if (it == null) {
                log("No festivals found or error decoding festivals.")
            } else {
                log(
                    "Festivals fetched successfully. : ${it.confirmedDates.size} fixed and ${it.variableDates.size} variable dates.",
                )
            }
        }
    }

    private fun FlagValue.toFestivalData(): FestivalData? = when (this) {
        is FlagValue.JsonValue -> {
            try {
                json.decodeFromString<FestivalData>(this.value)
            } catch (e: Exception) {
                logError("Error decoding festivals: ${e.message}", e)
                null
            }
        }

        else -> {
            val defaultJson: String = RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.FESTIVALS.key }?.second as? String ?: "{}"
            try {
                json.decodeFromString<FestivalData>(defaultJson)
            } catch (e: Exception) {
                logError("Error decoding fallback, default festivals: ${e.message}", e)
                null
            }
        }
    }

    private fun parseDateOrNull(date: String, greeting: String, tag: String) =
        runCatching { LocalDate.parse(date) }
            .onFailure { error ->
                logError(
                    "Failed to parse $tag:'$date' for festival '$greeting",
                    error,
                )
            }
            .getOrNull()
}
