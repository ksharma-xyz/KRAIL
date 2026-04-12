package xyz.ksharma.krail.departures.ui.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.datetime.DateTimeHelper.extractPlatformText
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureDateLabel
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Maps a [DepartureMonitorResponse] to a list of [StopDeparture] items ready for display.
 *
 * All colour and mode lookups are delegated to `NswTransportConfig` — the NSW implementation
 * of `TransportConfig` in :core:transport — so colours live in a single place.
 * Swapping in a Melbourne config in the future requires no changes here.
 */
internal fun DepartureMonitorResponse.toStopDepartures(): ImmutableList<StopDeparture> =
    stopEvents
        ?.mapNotNull { it.toStopDeparture() }
        ?.toImmutableList()
        ?: persistentListOf()

@OptIn(ExperimentalTime::class)
internal fun DepartureMonitorResponse.StopEvent.toStopDeparture(): StopDeparture? {
    val isRealTime = departureTimeEstimated != null
    val departureUtc = departureTimeEstimated ?: departureTimePlanned ?: run {
        logError("DepartureMonitorMapper", Exception("StopEvent has no departure time, skipping"))
        return null
    }

    val lineNumber = transportation?.disassembledName
        ?: transportation?.number
        ?: transportation?.name
        ?: ""

    // Compute delay only when we have both planned and estimated times.
    val delayMinutes: Int
    val scheduledTimeText: String?
    val planned: String? = departureTimePlanned
    val estimated: String? = departureTimeEstimated

    if (isRealTime && planned != null && estimated != null) {
        val diffMinutes = runCatching {
            val plannedInstant = Instant.parse(planned)
            val estimatedInstant = Instant.parse(estimated)
            val diffMs = estimatedInstant.toEpochMilliseconds() - plannedInstant.toEpochMilliseconds()
            (diffMs / MILLIS_PER_MINUTE).toInt()
        }.getOrDefault(0)
        delayMinutes = diffMinutes
        scheduledTimeText = if (diffMinutes != 0) {
            runCatching { planned.utcToLocalDateTimeAEST().toHHMM() }.getOrNull()
        } else {
            null
        }
    } else {
        delayMinutes = 0
        scheduledTimeText = null
    }

    return StopDeparture(
        lineNumber = lineNumber,
        lineColorCode = resolveLineColor(
            lineName = lineNumber,
            productClass = transportation?.product?.cls,
        ),
        transportModeName = resolveTransportModeName(transportation?.product?.cls),
        destinationName = transportation?.destination?.name ?: "",
        departureTimeText = runCatching { departureUtc.utcToLocalDateTimeAEST().toHHMM() }
            .getOrDefault(departureUtc),
        departureUtcDateTime = departureUtc,
        relativeTimeText = runCatching {
            calculateTimeDifferenceFromNow(departureUtc).toGenericFormattedTimeString()
        }.getOrDefault(""),
        platformText = location?.resolvePlatformText(),
        isRealTime = isRealTime,
        scheduledTimeText = scheduledTimeText,
        delayMinutes = delayMinutes,
        dateLabel = runCatching { departureUtc.toDepartureDateLabel() }.getOrDefault(""),
    )
}

// Returns the platform label (e.g. "Stand A", "Platform 7") extracted from the full
// disassembledName (e.g. "Seven Hills Station, Stand A, Seven Hills"), or null if no
// platform/stand/wharf keyword is found.
private fun DepartureMonitorResponse.Location.resolvePlatformText(): String? {
    val locationLabel = disassembledName ?: return null
    // Without a parent, this location IS the stop itself — no platform sub-label to extract.
    val parentNode = parent ?: return null
    log(
        "[DEPARTURES] resolvePlatformText — disassembledName=\"$locationLabel\" " +
            "parent=\"${parentNode.disassembledName}\"",
    )
    return extractPlatformText(locationLabel)
        .also { log("[DEPARTURES] resolvePlatformText → result=\"$it\"") }
}

// Resolves the hex colour for a line badge via NswTransportConfig:
// line-specific lookup first, then product-class fallback, then Bus colour.
private fun resolveLineColor(lineName: String, productClass: Int?): String =
    NswTransportConfig.lineColor(lineName)
        ?: productClass?.let { NswTransportMode.fromProductClass(it)?.colorCode }
        ?: NswTransportMode.Bus.colorCode

private fun resolveTransportModeName(productClass: Int?): String =
    productClass?.let { NswTransportMode.fromProductClass(it)?.name }
        ?: NswTransportMode.Bus.name

// -- constants -------------------------------------------------------------------

private const val MILLIS_PER_MINUTE = 60_000L
