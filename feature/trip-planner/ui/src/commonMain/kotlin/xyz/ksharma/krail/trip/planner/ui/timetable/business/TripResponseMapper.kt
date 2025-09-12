package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifference
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@Suppress("ComplexCondition")
internal fun TripResponse.buildJourneyList(): ImmutableList<TimeTableState.JourneyCardInfo>? =
    journeys?.mapNotNull { journey ->
        val firstPublicTransportLeg = journey.getFirstPublicTransportLeg()
        val lastPublicTransportLeg = journey.getLastPublicTransportLeg()
        val originTimeUTC = firstPublicTransportLeg?.getDepartureTime()
        val arrivalTimeUTC = lastPublicTransportLeg?.getArrivalTime()
        val legs = journey.getFilteredValidLegs()
        val totalStops = legs?.getTotalStops() ?: 0

        legs?.logTransportModes()

        val transportModeLines = legs?.getTransportModeLines()
        val legsList = legs?.getLegsList()

        if (originTimeUTC != null && arrivalTimeUTC != null && totalStops > 0 && legsList != null &&
            transportModeLines != null
        ) {
            // A walking leg consists of walking leg
            // + footpath info from public transport leg (if footPathInfoRedundant is true)
            val totalWalkingDuration = legs.sumOf { leg ->
                if (leg.isWalkingLeg() && leg.footPathInfoRedundant != true) {
                    leg.duration ?: 0L
                } else {
                    0L
                }
            }

            val walkingDurationStr =
                totalWalkingDuration.takeIf { it > 0L }?.toDuration(DurationUnit.SECONDS)
                    ?.toFormattedDurationTimeString()

            TimeTableState.JourneyCardInfo(
                timeText = originTimeUTC.getTimeText(),
                platformText = firstPublicTransportLeg.getPlatformText(),
                platformNumber = firstPublicTransportLeg.getPlatformNumber(),
                originTime = originTimeUTC.fromUTCToDisplayTimeString(),
                originUtcDateTime = originTimeUTC,
                destinationUtcDateTime = arrivalTimeUTC,
                destinationTime = arrivalTimeUTC.fromUTCToDisplayTimeString(),
                travelTime = calculateTimeDifference(
                    originTimeUTC,
                    arrivalTimeUTC,
                ).toFormattedDurationTimeString(),
                totalWalkTime = walkingDurationStr,
                transportModeLines = transportModeLines,
                legs = legsList,
                totalUniqueServiceAlerts = legs.flatMap { leg -> leg.infos.orEmpty() }.toSet().size,
                departureDeviation = firstPublicTransportLeg.getDepartureDeviation(),
            ).also {
                log("\tJourneyId: ${it.journeyId}")
            }
        } else {
            null
        }
    }?.toImmutableList()

private fun TripResponse.Journey.getFirstPublicTransportLeg() = legs?.firstOrNull { leg ->
    !leg.isWalkingLeg()
}

private fun TripResponse.Journey.getLastPublicTransportLeg() = legs?.lastOrNull { leg ->
    !leg.isWalkingLeg()
}

private fun TripResponse.Leg?.getDepartureTime() =
    this?.origin?.departureTimeEstimated ?: this?.origin?.departureTimePlanned

private fun TripResponse.Leg?.getArrivalTime() =
    this?.destination?.arrivalTimeEstimated ?: this?.destination?.arrivalTimePlanned

private fun TripResponse.Journey.getFilteredValidLegs() = legs?.filter { it.transportation != null }

private fun List<TripResponse.Leg>.getTotalStops() = sumOf { leg -> leg.stopSequence?.size ?: 0 }

fun TripResponse.Leg?.getPlatformText(): String? {
    val disassembledName = this?.origin?.disassembledName ?: return null
    val regex = Regex("(Platform|Stand|Wharf|Side)\\s*(\\d+|[A-Z])", RegexOption.IGNORE_CASE)
    val matches = regex.findAll(disassembledName).toList()
    return if (matches.isNotEmpty()) matches.joinToString(", ") { it.value } else null
}

/**
 * The platform number such as 1, 2 etc. or Stand A, B etc.
 */
fun TripResponse.Leg?.getPlatformNumber(): String? {
    val disassembledName = this?.origin?.disassembledName ?: return null
    val regex = Regex("(Platform|Stand|Wharf)\\s*(\\d+|[A-Z])", RegexOption.IGNORE_CASE)
    val match = regex.find(disassembledName)
    return match?.groupValues?.get(2)
}

private fun List<TripResponse.Leg>.logTransportModes() = forEachIndexed { index, leg ->

    // log origin's disassembledName
    log("Origin #$index: ${leg.origin?.disassembledName}")
    log(
        "TransportMode #$index: ${leg.transportation?.product?.productClass}, " +
            "name: ${leg.transportation?.product?.name}, " +
            "stops: ${leg.stopSequence?.size}, " +
            "duration: ${leg.duration}",
    )
}

private fun List<TripResponse.Leg>.getTransportModeLines() = mapNotNull { leg ->
    leg.transportation?.product?.productClass?.toInt()?.let { productClass ->
        val mode = TransportMode.toTransportModeType(productClass)
        val lineName = leg.transportation?.disassembledName
        if (mode != null && lineName != null) {
            TransportModeLine(transportMode = mode, lineName = lineName)
        } else {
            null
        }
    }
}.toImmutableList()

private fun List<TripResponse.Leg>.getLegsList() = mapNotNull { it.toUiModel() }.toImmutableList()

@OptIn(ExperimentalTime::class)
private fun String.getTimeText() = let {
    calculateTimeDifferenceFromNow(utcDateString = it).toGenericFormattedTimeString()
}

@Suppress("ComplexCondition")
private fun TripResponse.Leg.toUiModel(): TimeTableState.JourneyCardInfo.Leg? {
    val transportMode =
        transportation?.product?.productClass?.toInt()
            ?.let { TransportMode.toTransportModeType(productClass = it) }
    val lineName = transportation?.disassembledName

    val displayText =
        if (transportation?.product?.productClass?.toInt() == TransportMode.Train().productClass) {
            transportation?.destination?.name
        } else {
            transportation?.description
        }
    val numberOfStops = stopSequence?.size
    val displayDuration = duration?.seconds?.toFormattedDurationTimeString()
    val stops = stopSequence?.mapNotNull { it.toUiModel() }?.toImmutableList()
    val alerts = infos?.mapNotNull { it.toAlert() }?.toImmutableList()

    return when {
        // Walking Leg - Always check before public transport leg
        isWalkingLeg() -> if (displayDuration != null && this.footPathInfoRedundant != true) {
            TimeTableState.JourneyCardInfo.Leg.WalkingLeg(duration = displayDuration)
        } else {
            null
        }

        else -> { // Public Transport Leg
            if (transportMode != null && lineName != null && displayText != null &&
                numberOfStops != null && stops != null && displayDuration != null
            ) {
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    transportModeLine = TransportModeLine(
                        transportMode = transportMode,
                        lineName = lineName,
                    ),
                    displayText = displayText,
                    totalDuration = displayDuration,
                    stops = stops,
                    serviceAlertList = alerts,
                    walkInterchange = footPathInfo?.firstOrNull()?.run {
                        duration?.seconds?.toFormattedDurationTimeString()
                            ?.let { formattedDuration -> toWalkInterchange(formattedDuration) }
                    },
                    tripId = transportation?.id + transportation?.properties?.realtimeTripId,
                )
            } else {
                logError(
                    "Something is null - NOT adding Transport LEG: " +
                        "TransportMode: $transportMode, lineName: $lineName, displayText: $displayText, " +
                        "numberOfStops: $numberOfStops, stops: $stops, displayDuration: $displayDuration",
                )
                null
            }
        }
    }
}

internal fun TripResponse.FootPathInfo.toWalkInterchange(
    displayDuration: String,
): TimeTableState.JourneyCardInfo.WalkInterchange? {
    return position?.let { position ->
        when (position) {
            TimeTableState.JourneyCardInfo.WalkPosition.BEFORE.position -> {
                TimeTableState.JourneyCardInfo.WalkInterchange(
                    duration = displayDuration,
                    position = TimeTableState.JourneyCardInfo.WalkPosition.BEFORE,
                )
            }

            TimeTableState.JourneyCardInfo.WalkPosition.AFTER.position -> {
                TimeTableState.JourneyCardInfo.WalkInterchange(
                    duration = displayDuration,
                    position = TimeTableState.JourneyCardInfo.WalkPosition.AFTER,
                )
            }

            TimeTableState.JourneyCardInfo.WalkPosition.IDEST.position -> {
                TimeTableState.JourneyCardInfo.WalkInterchange(
                    duration = displayDuration,
                    position = TimeTableState.JourneyCardInfo.WalkPosition.IDEST,
                )
            }

            else -> null
        }
    }
}

private fun TripResponse.StopSequence.toUiModel(): TimeTableState.JourneyCardInfo.Stop? {
    val stopName = disassembledName ?: name
    // For last leg there is no departure time, so using arrival time
    // For first leg there is no arrival time, so using departure time.
    val time =
        departureTimeEstimated ?: departureTimePlanned ?: arrivalTimeEstimated
            ?: arrivalTimePlanned
    return if (stopName != null && time != null) {
        TimeTableState.JourneyCardInfo.Stop(
            name = stopName,
            time = time.fromUTCToDisplayTimeString(),
            isWheelchairAccessible = properties?.wheelchairAccess.toBoolean(),
        )
    } else {
        null
    }
}

private fun String.fromUTCToDisplayTimeString() = this.utcToLocalDateTimeAEST().toHHMM()

private fun TripResponse.Leg.isWalkingLeg(): Boolean =
    transportation?.product?.productClass == 99L || transportation?.product?.productClass == 100L

@OptIn(ExperimentalTime::class)
private fun TripResponse.Leg?.getDepartureDeviation(): TimeTableState.JourneyCardInfo.DepartureDeviation? {
    val est = this?.origin?.departureTimeEstimated
    val planned = this?.origin?.departureTimePlanned
    if (est == null || planned == null) return null
    val estInstant = Instant.parse(est)
    val plannedInstant = Instant.parse(planned)
    val diff = estInstant - plannedInstant
    val mins = diff.inWholeMinutes
    return when {
        mins == 0L -> TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime
        mins > 0L -> {
            val abs = mins.absoluteValue
            val unit = if (abs == 1L) "min" else "mins"
            TimeTableState.JourneyCardInfo.DepartureDeviation.Late("$abs $unit late")
        }
        else -> {
            val abs = mins.absoluteValue
            val unit = if (abs == 1L) "min" else "mins"
            TimeTableState.JourneyCardInfo.DepartureDeviation.Early("$abs $unit early")
        }
    }
}
