package xyz.ksharma.krail.trip.planner.network.api.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Product classes 99 and 100 are footpath/walking legs in the NSW Transport API. */
fun TripResponse.Leg.isWalkingLeg(): Boolean =
    transportation?.product?.productClass == 99L || transportation?.product?.productClass == 100L

/**
 * Difference in whole minutes between estimated and planned departure (estimated − planned).
 * Positive = late, negative = early, zero = on time, null = no realtime data.
 */
@OptIn(ExperimentalTime::class)
fun TripResponse.Leg?.departureDeviationMinutes(): Long? {
    val est = this?.origin?.departureTimeEstimated ?: return null
    val planned = this?.origin?.departureTimePlanned ?: return null
    return (Instant.parse(est) - Instant.parse(planned)).inWholeMinutes
}
