package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.festival.model.FestivalData
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue

internal class RealFestivalManager(private val flag: Flag) : FestivalManager {

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
            val startDate = runCatching { LocalDate.parse(festival.startDate) }.getOrNull()
            val endDate = runCatching { LocalDate.parse(festival.endDate) }.getOrNull()
            startDate != null && endDate != null && (date >= startDate && date <= endDate)
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
        return flagValue.toFestivalData().also {
            if (it == null) logError("No festivals found or error decoding festivals.")
            else log("Festivals fetched successfully.")
        }
    }

    private fun FlagValue.toFestivalData(): FestivalData? = when (this) {
        is FlagValue.JsonValue -> {
            try {
                Json.decodeFromString<FestivalData>(this.value)
            } catch (e: Exception) {
                logError("Error decoding festivals: ${e.message}", e)
                null
            }
        }

        else -> {
            val defaultJson: String = RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.FESTIVALS.key }?.second as? String ?: "{}"
            try {
                Json.decodeFromString<FestivalData>(defaultJson)
            } catch (e: Exception) {
                logError("Error decoding fallback, default festivals: ${e.message}", e)
                null
            }
        }
    }
}
