package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature

/**
 * UI state for stop time information.
 */
data class StopTimeInfo(
    val label: String,
    val value: String,
)

/**
 * Mapper to convert JourneyStopFeature to UI-ready time information.
 */
object JourneyStopUiMapper {

    /**
     * Extract and format time information from a stop.
     */
    fun JourneyStopFeature.getTimeInfo(): List<StopTimeInfo> {
        val formattedArrival = arrivalTime?.formatUtcTime()
        val formattedDeparture = departureTime?.formatUtcTime()

        return listOfNotNull(
            formattedArrival?.let { StopTimeInfo(label = "Arrival", value = it) },
            formattedDeparture?.let { StopTimeInfo(label = "Departure", value = it) },
        )
    }

    /**
     * Format UTC time string to local HH:MM format.
     * Returns the original string if formatting fails.
     */
    private fun String.formatUtcTime(): String? = runCatching {
        utcToLocalDateTimeAEST().toHHMM()
    }.getOrElse { error ->
        logError("Failed to format time: $this - ${error.message}")
        null
    }
}
