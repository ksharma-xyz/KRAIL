package xyz.ksharma.krail.feature.crowding

/** A later service named the way the station board names it: line, clock time, platform. */
data class NextService(
    val line: String,
    val departureTime: String,
    val level: SeatLevel,
    val certainty: Certainty,
    val platform: String? = null,
)

/**
 * The next-service line from the design review wording table. The service gets the verb and the
 * rider chooses: KRAIL never says "skip" or "wait". Kept apart from [CrowdingWording] because it
 * names a different service, not where to stand on this one.
 */
object NextServiceWording {

    /**
     * "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due at
     * Central at 9:26am." Named like the station board: line, clock time, platform. The service
     * gets the verb and the rider chooses. Arrival is "due at", never "by".
     */
    fun nextTrain(service: NextService, destination: String, arrivalTime: String): String {
        val from = service.platform?.let { " from $it" }.orEmpty()
        return "Want a seat? The next ${service.line}, at ${service.departureTime}$from, " +
            "${serviceHas(service)} and is due at $destination at $arrivalTime."
    }

    /** "Want a seat? The next 333, at 9:04am, has seats available." */
    fun nextBus(service: NextService): String =
        "Want a seat? The next ${service.line}, at ${service.departureTime}, ${serviceHas(service)}."

    private fun serviceHas(service: NextService): String {
        val seats = if (service.level == SeatLevel.SEATS_AVAILABLE) "seats available" else "a few seats left"
        return when (service.certainty) {
            Certainty.LIVE -> "has $seats"
            Certainty.FORECAST -> "should have $seats"
        }
    }
}
