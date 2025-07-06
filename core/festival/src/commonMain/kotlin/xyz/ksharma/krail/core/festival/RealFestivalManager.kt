package xyz.ksharma.krail.core.festival

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue

internal class RealFestivalManager(private val flag: Flag) : FestivalManager {

    /**
     * festival dates will be a json object with the following structure, it will be array of objects:
     *
     * ```json
     * [
     * startDate: "2023-10-01",
     * endDate: "2023-10-31",
     * emoji: "🎃",
     * description: "Halloween"
     * ]
     * ```
     *
     * 1. read if there is any festival inside remote config, get it's start date, end date and emoji.
     * 2. if the current date is between start date and end date, then return the emoji.
     * 3. if the current date is not between start date and end date, then return null.
     *
     */

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
