package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.NextService
import xyz.ksharma.krail.feature.crowding.NextServiceRule
import xyz.ksharma.krail.feature.crowding.NextServiceWording
import xyz.ksharma.krail.feature.crowding.ServiceOption
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo.Leg
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Names a quieter run of the same train, metro or bus line when [NextServiceRule] allows it.
 *
 * A bus is named only when it boards at the same stand id as the rider's bus, which is what
 * the design's "listed at the same stop" rule protects: the next 333 from across the road is
 * not the next 333. That comes from the trip response itself, so no second request is made.
 * Any journey with a second service counts as having a later connection, because the card
 * does not carry the slack between them and waiting could break the change.
 *
 * The trip planner level is a reading from the vehicle now, so the line says "has".
 */
internal fun List<JourneyCardInfo>.withNextServices(destinationName: String?): List<JourneyCardInfo> {
    if (destinationName == null) return this
    val options = mapNotNull { journey -> journey.serviceOption()?.let { journey to it } }
    val byId = options.associate { (journey, option) -> option.id to journey }
    return map { journey ->
        val option = options.firstOrNull { it.first === journey }?.second
            ?: return@map journey.copy(nextServiceLine = null, nextServiceJourneyId = null)
        val next = NextServiceRule.pick(
            current = option,
            later = options.map { it.second },
            hasLaterConnection = journey.legs.count { it is Leg.TransportLeg } > 1,
            isNight = journey.isNight(),
        )?.let { byId[it.id] }
        journey.copy(
            nextServiceLine = next?.let { it.nextServiceLine(destinationName) },
            nextServiceJourneyId = next?.journeyId,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun JourneyCardInfo.serviceOption(): ServiceOption? {
    val leg = legs.firstNotNullOfOrNull { it as? Leg.TransportLeg }
        ?.takeIf { it.transportModeLine.transportMode in NAMEABLE_MODES }
    val departure = runCatching { Instant.parse(originUtcDateTime).epochSeconds }.getOrNull()
    // A bus is keyed on its stand too; without a stand id it cannot be matched safely.
    val stand = if (leg?.transportModeLine?.transportMode is TransportMode.Bus) leg.boardingStopId else ""
    return if (leg == null || departure == null || stand == null) {
        null
    } else {
        ServiceOption(
            id = journeyId,
            routeKey = "${leg.transportModeLine.lineName}|${leg.displayText}|$stand",
            departureEpochSeconds = departure,
            level = leg.seatLevel,
        )
    }
}

private fun JourneyCardInfo.isNight(): Boolean = runCatching {
    CrowdingWording.isNight(originUtcDateTime.utcToLocalDateTimeAEST().hour)
}.getOrDefault(true)

private fun JourneyCardInfo.nextServiceLine(destinationName: String): String? {
    val leg = legs.firstNotNullOfOrNull { it as? Leg.TransportLeg } ?: return null
    val level = leg.seatLevel ?: return null
    val service = NextService(
        line = leg.transportModeLine.lineName,
        departureTime = originTime,
        level = level,
        certainty = Certainty.LIVE,
        platform = platformText,
    )
    return if (leg.transportModeLine.transportMode is TransportMode.Bus) {
        NextServiceWording.nextBus(service)
    } else {
        NextServiceWording.nextTrain(service = service, destination = destinationName, arrivalTime = destinationTime)
    }
}

private val NAMEABLE_MODES = setOf(TransportMode.Train, TransportMode.Metro, TransportMode.Bus)
