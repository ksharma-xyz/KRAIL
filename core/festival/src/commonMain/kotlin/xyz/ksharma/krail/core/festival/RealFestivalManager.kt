package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import kotlin.time.ExperimentalTime

internal class RealFestivalManager(private val flag: Flag) : FestivalManager {

    @OptIn(ExperimentalTime::class)
    override fun festivalOnDate(date: LocalDate): Festival? {
        log("Checking festival for date: $date")
        val festivals = getFestivals() ?: return null
        log("Today's date: $date")

        return festivals.firstOrNull { festival ->
            val startDate = runCatching { LocalDate.parse(festival.startDate) }.getOrNull()
            val endDate = runCatching { LocalDate.parse(festival.endDate) }.getOrNull()
            if (startDate == null || endDate == null) {
                logError("Invalid date for festival: ${festival.description}")
                false
            } else {
                log("Checking festival: ${festival.description} from $startDate to $endDate")
                (date >= startDate && date <= endDate)
            }
        }?.also { festival ->
            log("Festival found for today: ${festival.description} with emoji: ${festival.emojiList}")
        } ?: run {
            log("No festival found for today.")
            null
        }
    }

    override fun getFestivals(): List<Festival>? {
        log("Fetching festivals from remote config")
        val flagValue = flag.getFlagValue(FlagKeys.FESTIVALS.key)

        return flagValue.toFestivalList().also { festivals ->
            if (festivals == null) {
                logError("No festivals found or error decoding festivals.")
            } else {
                log("Festivals fetched successfully: ${festivals.size} festivals found.")
            }
        }
    }

    private fun FlagValue.toFestivalList(): List<Festival>? = when (this) {
        is FlagValue.JsonValue -> {
            log("flagValue: ${this.value}")
            try {
                Json.decodeFromString<List<Festival>>(this.value)
            } catch (e: Exception) {
                logError("Error decoding festivals: ${e.message}", e)
                null
            }
        }
        else -> {
            val defaultJson: String = RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.FESTIVALS.key }?.second as? String ?: "[]"
            try {
                Json.decodeFromString<List<Festival>>(defaultJson)
            } catch (e: Exception) {
                logError(
                    message = "Error decoding fallback, default festivals: ${e.message}",
                    throwable = e,
                )
                null
            }
        }
    }
}