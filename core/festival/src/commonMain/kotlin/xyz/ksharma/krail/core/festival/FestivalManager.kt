package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.festival.model.Festival
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface FestivalManager {

    /**
     * Returns a list of festivals from the remote config.
     *
     * If the remote config is not available or the value is not a valid JSON, it will return null.
     *
     * @return List of [Festival] objects or null if no festivals are available.
     */
    fun getFestivals(): List<Festival>?

    /**
     * Checks if there is a festival today and returns it if available.
     * If no festival is found for today, it returns null.
     *
     * @param date The date to check for a festival. Defaults to the current date.
     */
    @OptIn(ExperimentalTime::class)
    fun festivalOnDate(
        date: LocalDate = Instant.fromEpochMilliseconds(
            epochMilliseconds = Clock.System.now().toEpochMilliseconds(),
        ).toLocalDateTime(timeZone = currentSystemDefault()).date
    ): Festival?
}
