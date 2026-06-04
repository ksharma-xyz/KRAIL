package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Builds a [TimeTableState.JourneyCardInfo.Leg.WalkingLeg] for a walking leg, or null when the
 * walking leg has no display duration or its footpath info is redundant.
 */
@OptIn(ExperimentalTime::class)
internal fun TripResponse.Leg.toWalkingLegUiModel(): TimeTableState.JourneyCardInfo.Leg? {
    // duration can be null for some legs; resolveDurationSeconds() falls back to dep/arr times.
    val displayDuration = resolveDurationSeconds()?.seconds?.toFormattedDurationTimeString()
    return if (displayDuration != null && footPathInfoRedundant != true) {
        TimeTableState.JourneyCardInfo.Leg.WalkingLeg(duration = displayDuration)
    } else {
        null
    }
}

/**
 * Builds a [TimeTableState.JourneyCardInfo.Leg.TransportLeg] for a public transport leg, or null
 * (and logs which field was null) when any required field is missing.
 */
@OptIn(ExperimentalTime::class)
@Suppress("ComplexCondition")
internal fun TripResponse.Leg.toTransportLegUiModel(): TimeTableState.JourneyCardInfo.Leg? {
    val transportMode = transportation?.product?.productClass?.toInt()
        ?.let { NswTransportConfig.modeFromProductClass(productClass = it) }
    val lineName = transportation?.disassembledName
    val displayText = NswTransportConfig.resolveServiceDisplayText(
        productClass = transportation?.product?.productClass?.toInt(),
        destinationName = transportation?.destination?.name,
        description = transportation?.description,
    )
    val numberOfStops = stopSequence?.size
    val displayDuration = resolveDurationSeconds()?.seconds?.toFormattedDurationTimeString()
    val stops = stopSequence?.mapNotNull { it.toUiModel() }?.toImmutableList()
    val alerts = infos?.mapNotNull { it.toAlert() }?.toImmutableList()

    if (transportMode == null || lineName == null || displayText == null ||
        numberOfStops == null || stops == null || displayDuration == null
    ) {
        logError(
            "Something is null - NOT adding Transport LEG: " +
                "TransportMode: $transportMode, lineName: $lineName, displayText: $displayText, " +
                "numberOfStops: $numberOfStops, stops: $stops, displayDuration: $displayDuration",
        )
        return null
    }

    return TimeTableState.JourneyCardInfo.Leg.TransportLeg(
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
        transportationId = transportation?.id,
    )
}
