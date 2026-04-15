package xyz.ksharma.krail.departures.ui.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.extractPlatformText
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureDateLabel
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureRelativeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
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
        log("[DepartureMonitorMapper] skipping StopEvent — no departure time (both planned and estimated are null)")
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
            departureUtc.toDepartureRelativeString()
        }.getOrDefault(""),
        platformText = location?.resolvePlatformText(),
        isRealTime = isRealTime,
        scheduledTimeText = scheduledTimeText,
        delayMinutes = delayMinutes,
        dateLabel = runCatching { departureUtc.toDepartureDateLabel() }.getOrDefault(""),
    )
}

// Returns the platform label (e.g. "Stand H", "Platform 6", "Town Hall Light Rail")
// ready for display.
//
// All three fields in `location.properties` are considered:
//   platform     = raw code   ("THL6", "J", "LR1")
//   platformName = human label ("Platform 6", "Town Hall, Park St, Stand J", "Town Hall Light Rail")
//                  — but sometimes the API echoes the raw code here too (e.g. "THL6")
//
// Decision tree (primary source is always properties.platformName, NOT disassembledName):
//   A. platformName is a real label (differs from the raw platform code):
//      1. Run regex → extracts "Platform N", "Stand X", etc. from compound strings
//      2. No match   → return platformName as-is (e.g. "Town Hall Light Rail")
//   B. platformName equals the raw platform code — API returned raw code as label:
//      → Extract trailing digits from the code → "Platform N"
//      → No digits → return the code itself
//   C. No properties at all (legacy / edge case):
//      → Fall back to regex on disassembledName
private fun DepartureMonitorResponse.Location.resolvePlatformText(): String? {
    // Without a parent, this location IS the stop itself — no platform sub-label to extract.
    parent ?: return null

    val pName = properties?.platformName
    val pCode = properties?.platform

    if (pName != null) {
        return if (pName != pCode) {
            // A – real human-readable label: try regex first, then use as-is.
            extractPlatformText(pName) ?: pName
        } else {
            // B – API echoed the raw code (e.g. platformName = "THL6"):
            //     extract the trailing number and format as "Platform N".
            val num = Regex("\\d+").find(pName)?.value
            if (num != null) "Platform $num" else pName
        }
    }

    // C – No platformName; try extracting a number from the raw platform code.
    if (pCode != null) {
        val num = Regex("\\d+").find(pCode)?.value
        if (num != null) return "Platform $num"
    }

    // Last resort: regex on the full disassembledName (handles data without properties).
    val locationLabel = disassembledName ?: return null
    return extractPlatformText(locationLabel)
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
