package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg

/**
 * True when a result could use per-carriage forecasts: a train leg with a realtime trip id.
 * Bus, ferry and light rail only results never spend the trip updates download.
 */
internal fun List<JourneyCardInfo>.wantsCarriages(): Boolean = any { journey ->
    journey.legs.any { it is Leg.TransportLeg && it.isTrainWithTripId() }
}

/**
 * Attaches the forecast cars, sentence and tag to every train leg the [index] covers. Pure, so
 * the view model can re-apply the same index to every refreshed journey list.
 *
 * Trip updates are per-stop predictions, so the certainty word is always "expected".
 */
internal fun List<JourneyCardInfo>.withCarriages(index: CarriageIndex): List<JourneyCardInfo> {
    if (index.tripCount == 0) return this
    return map { journey ->
        val isNight = runCatching {
            CrowdingWording.isNight(journey.originUtcDateTime.utcToLocalDateTimeAEST().hour)
        }.getOrDefault(false)
        journey.copy(
            legs = journey.legs.map { leg ->
                if (leg is Leg.TransportLeg && leg.isTrainWithTripId()) leg.withCarriages(index, isNight) else leg
            }.toImmutableList(),
        )
    }
}

private fun Leg.TransportLeg.withCarriages(index: CarriageIndex, isNight: Boolean): Leg.TransportLeg {
    val cars = index.at(realtimeTripId, boardingStopId)
        ?: return copy(carriages = null, carriageSentence = null, carriageTag = null)
    val advice = CrowdingWording.forCarriages(cars, Certainty.FORECAST, isNight)
    return copy(carriages = cars, carriageSentence = advice?.sentence, carriageTag = advice?.tag)
}

private fun Leg.TransportLeg.isTrainWithTripId(): Boolean =
    transportModeLine.transportMode is TransportMode.Train && !realtimeTripId.isNullOrBlank()
