package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class RealFestivalManager(private val flag: Flag) : FestivalManager {

    @OptIn(ExperimentalTime::class)

    override fun festivalToday(): Festival? {
        log("Checking for today's festival")
        val festivals = getFestivals() ?: return null

        val now = Clock.System.now()
        val today: LocalDate = Instant.fromEpochMilliseconds(now.toEpochMilliseconds())
            .toLocalDateTime(currentSystemDefault()).date
        log("Today's date: $today")

        return festivals.firstOrNull { festival ->
            val startDate = runCatching { LocalDate.parse(festival.startDate) }.getOrNull()
            val endDate = runCatching { LocalDate.parse(festival.endDate) }.getOrNull()
            if (startDate == null || endDate == null) {
                logError("Invalid date for festival: ${festival.description}")
                false
            } else {
                log("Checking festival: ${festival.description} from $startDate to $endDate")
                (today >= startDate && today <= endDate)
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

    /**
     * Returns a list of festivals from the remote config.
     *
     * If the remote config is not available or the value is not a valid JSON, it will return null.
     *
     * * @return List of [Festival] objects or null if no festivals are available.
     *
     * * @see [RemoteConfigDefaults.getDefaults]
     */
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
