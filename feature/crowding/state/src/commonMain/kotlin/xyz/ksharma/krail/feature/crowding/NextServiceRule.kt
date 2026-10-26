package xyz.ksharma.krail.feature.crowding

/** One departure as the next-service rule sees it. [routeKey] is line plus direction. */
data class ServiceOption(
    val id: String,
    val routeKey: String,
    val departureEpochSeconds: Long,
    val level: SeatLevel?,
)

/**
 * When KRAIL may point at a later service (CROWDING_DESIGN_REVIEW.md, wording rules). A later
 * service costs the rider time, so it is named only when every condition holds:
 *
 * - it runs the same route in the same direction,
 * - it is at least one full level quieter than the rider's service,
 * - it leaves within [MAX_WAIT_SECONDS] (about ten minutes),
 * - the rider's trip has no connection later that waiting could break,
 * - it is not night: after about 9pm KRAIL never mentions a later train.
 *
 * Returns the first service that qualifies, or null. Null says nothing.
 */
object NextServiceRule {

    const val MAX_WAIT_SECONDS = 10 * 60L

    fun pick(
        current: ServiceOption,
        later: List<ServiceOption>,
        hasLaterConnection: Boolean,
        isNight: Boolean,
    ): ServiceOption? {
        val currentLevel = current.level
        if (isNight || hasLaterConnection || currentLevel == null) return null
        return later
            .sortedBy { it.departureEpochSeconds }
            .firstOrNull { candidate -> candidate.qualifiesAfter(current, currentLevel) }
    }

    private fun ServiceOption.qualifiesAfter(current: ServiceOption, currentLevel: SeatLevel): Boolean {
        val wait = departureEpochSeconds - current.departureEpochSeconds
        val candidateLevel = level ?: return false
        return id != current.id &&
            routeKey == current.routeKey &&
            wait in 1..MAX_WAIT_SECONDS &&
            currentLevel.isBusierThan(candidateLevel)
    }
}
