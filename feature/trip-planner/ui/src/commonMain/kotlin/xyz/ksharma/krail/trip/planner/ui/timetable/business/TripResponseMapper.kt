package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifference
import xyz.ksharma.krail.core.datetime.DateTimeHelper.extractPlatformText
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureRelativeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.departureDeviationMinutes
import xyz.ksharma.krail.trip.planner.network.api.model.isWalkingLeg
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.math.absoluteValue
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@Suppress("ComplexCondition")
internal fun TripResponse.buildJourneyList(): ImmutableList<TimeTableState.JourneyCardInfo>? =
    buildJourneyListWithRawData().first

/**
 * Build journey list along with raw journey data map for map visualization.
 * Returns Pair<JourneyList, RawDataMap>
 */
@Suppress("ComplexCondition")
internal fun TripResponse.buildJourneyListWithRawData():
    Pair<ImmutableList<TimeTableState.JourneyCardInfo>?, Map<String, TripResponse.Journey>> {
    val rawDataMap = mutableMapOf<String, TripResponse.Journey>()

    val journeyList = journeys?.mapNotNull { journey ->
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
                totalUniqueServiceAlerts = legsList
                    .filterIsInstance<TimeTableState.JourneyCardInfo.Leg.TransportLeg>()
                    .flatMap { it.serviceAlertList.orEmpty() }
                    .toSet()
                    .size,
                departureDeviation = firstPublicTransportLeg.getDepartureDeviation(),
                scheduledOriginTime = firstPublicTransportLeg.getScheduledOriginTime(),
            ).also {
                log("\tJourneyId: ${it.journeyId}")
                // Store raw journey data for map visualization
                rawDataMap[it.journeyId] = journey
            }
        } else {
            null
        }
    }?.toImmutableList()

    return Pair(journeyList, rawDataMap)
}
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
    return extractPlatformText(disassembledName)
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
        val mode = NswTransportConfig.modeFromProductClass(productClass)
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
private fun String.getTimeText() = toDepartureRelativeString()

private fun TripResponse.Leg.toUiModel(): TimeTableState.JourneyCardInfo.Leg? =
    // Walking Leg - Always check before public transport leg. Both builders compute their
    // own fields from this leg; see TripResponseLegMapper.kt.
    if (isWalkingLeg()) toWalkingLegUiModel() else toTransportLegUiModel()

/**
 * Resolves the duration of a leg in seconds.
 *
 * The API sometimes omits [TripResponse.Leg.duration] (returns null) for certain legs —
 * commonly the first short bus hop in a multi-leg journey. In those cases we calculate
 * the duration from the departure and arrival timestamps that are always present.
 *
 * Priority: explicit [TripResponse.Leg.duration] > calculated from dep/arr timestamps > null
 */
@OptIn(ExperimentalTime::class)
internal fun TripResponse.Leg.resolveDurationSeconds(): Long? {
    if (duration != null) return duration
    val dep = origin?.departureTimeEstimated ?: origin?.departureTimePlanned
    val arr = destination?.arrivalTimeEstimated ?: destination?.arrivalTimePlanned
    return if (dep != null && arr != null) {
        runCatching {
            val depInstant = Instant.parse(dep)
            val arrInstant = Instant.parse(arr)
            (arrInstant - depInstant).inWholeSeconds.takeIf { it > 0 }
        }.onFailure { error ->
            logError("error - $error")
        }.getOrNull()
    } else {
        null
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

internal fun TripResponse.StopSequence.toUiModel(): TimeTableState.JourneyCardInfo.Stop? {
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

/**
 * Returns the scheduled (planned) origin time formatted for display, but only when the
 * real-time estimated departure differs from the planned time. Returns null otherwise,
 * meaning there is nothing to strike-through.
 */
private fun TripResponse.Leg?.getScheduledOriginTime(): String? {
    val est = this?.origin?.departureTimeEstimated ?: return null
    val planned = this.origin?.departureTimePlanned ?: return null
    return if (est != planned) planned.fromUTCToDisplayTimeString() else null
}

private fun TripResponse.Leg?.getDepartureDeviation(): TimeTableState.JourneyCardInfo.DepartureDeviation? {
    val mins = this.departureDeviationMinutes() ?: return null
    return when {
        mins == 0L -> TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime
        mins > 0L -> {
            val abs = mins.absoluteValue
            TimeTableState.JourneyCardInfo.DepartureDeviation.Late("$abs ${if (abs == 1L) "min" else "mins"} late")
        }
        else -> {
            val abs = mins.absoluteValue
            TimeTableState.JourneyCardInfo.DepartureDeviation.Early("$abs ${if (abs == 1L) "min" else "mins"} early")
        }
    }
}
