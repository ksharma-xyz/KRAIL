package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Builds a [TimeTableState.JourneyCardInfo.Leg.WalkingLeg] for a walking leg, or null when the
 * walking leg has no display duration or its footpath info is redundant.
 *
 * [isFirstLeg]/[isLastLeg] identify this leg's position in the journey's raw leg list - the
 * true first-mile (origin) and last-mile (destination) walk legs get a "pin row" name so the
 * rider always knows exactly where they're walking from/to, whether that endpoint is a
 * searched address or a named transit stop (e.g. "Town Hall Station") - a rider unfamiliar
 * with the city shouldn't be left guessing where the final walk leads just because the
 * destination happens to be a stop rather than a street address.
 *
 * EXPERIMENT (see WalkingLeg.kt pin-row work): NSW flags some first/last-mile footpath legs
 * `footPathInfoRedundant = true` (e.g. Town Hall Station -> QVB, Stand C), which previously
 * dropped the leg entirely - even though it's the only text telling the rider they need to
 * walk before boarding. Ignoring the redundant flag for first/last legs surfaces that info;
 * interior/interchange legs still respect it, since those are genuinely covered elsewhere.
 * Revert to `footPathInfoRedundant != true` (drop the `isFirstLeg || isLastLeg` clause) if
 * this turns out noisier than useful.
 */
@OptIn(ExperimentalTime::class)
internal fun TripResponse.Leg.toWalkingLegUiModel(
    isFirstLeg: Boolean = false,
    isLastLeg: Boolean = false,
): TimeTableState.JourneyCardInfo.Leg? {
    // BFF proto path ships a render-ready duration string; the NSW JSON path
    // derives it from duration seconds / dep-arr times.
    val displayDuration = bffDisplayDuration
        ?: resolveDurationSeconds()?.seconds?.toFormattedDurationTimeString()
    val footPathInfoShown = footPathInfoRedundant != true || isFirstLeg || isLastLeg
    return if (displayDuration != null && footPathInfoShown) {
        TimeTableState.JourneyCardInfo.Leg.WalkingLeg(
            duration = displayDuration,
            originPinName = if (isFirstLeg) {
                origin?.disassembledName ?: origin?.name
            } else {
                null
            },
            destinationPinName = if (isLastLeg) {
                destination?.disassembledName ?: destination?.name
            } else {
                null
            },
        )
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
    val rawProductClass = transportation?.product?.productClass?.toInt()
    val isSchoolBus = rawProductClass == TransportMode.SCHOOL_BUS_PRODUCT_CLASS
    val transportMode = rawProductClass
        ?.let { NswTransportConfig.modeFromProductClass(productClass = it) }
    val isOnDemand = transportMode is TransportMode.OnDemand
    val lineName = transportation?.disassembledName
    val displayText = NswTransportConfig.resolveServiceDisplayText(
        productClass = transportation?.product?.productClass?.toInt(),
        destinationName = transportation?.destination?.name,
        description = transportation?.description,
    )
    val numberOfStops = stopSequence?.size
    val displayDuration = bffDisplayDuration
        ?: resolveDurationSeconds()?.seconds?.toFormattedDurationTimeString()
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
            duration?.seconds?.toFormattedDurationTimeString()?.let { formattedDuration ->
                toWalkInterchange(formattedDuration)
            }
        },
        tripId = transportation?.id + transportation?.properties?.realtimeTripId,
        transportationId = transportation?.id,
        isSchoolBus = isSchoolBus,
        isOnDemand = isOnDemand,
    )
}
