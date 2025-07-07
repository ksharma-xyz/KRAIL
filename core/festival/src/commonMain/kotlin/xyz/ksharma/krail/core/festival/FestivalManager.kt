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
     * Checks if there is a festival on a particular date and returns it if available.
     * If no festival is found for the date, it returns null.
     *
     * @param date The date to check for a festival. Defaults to the current date.
     */
    @OptIn(ExperimentalTime::class)
    fun festivalOnDate(
        date: LocalDate = Instant.fromEpochMilliseconds(
            epochMilliseconds = Clock.System.now().toEpochMilliseconds(),
        ).toLocalDateTime(timeZone = currentSystemDefault()).date,
    ): Festival?
}
