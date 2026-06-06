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
import xyz.ksharma.krail.core.transport.TransportMode
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
        destinationName = NswTransportConfig.resolveServiceDisplayText(
            productClass = transportation?.product?.cls,
            destinationName = transportation?.destination?.name,
            description = transportation?.description,
        ) ?: "",
        departureTimeText = runCatching { departureUtc.utcToLocalDateTimeAEST().toHHMM() }
            .getOrDefault(departureUtc),
        departureUtcDateTime = departureUtc,
        relativeTimeText = runCatching {
            departureUtc.toDepartureRelativeString()
        }.getOrDefault(""),
        platformText = location?.resolvePlatformText(transportation?.product?.cls),
        isRealTime = isRealTime,
        scheduledTimeText = scheduledTimeText,
        delayMinutes = delayMinutes,
        dateLabel = runCatching { departureUtc.toDepartureDateLabel() }.getOrDefault(""),
    )
}

// Returns the platform label ready for display, using transport-mode–aware rules.
//
//   platform     = raw API code  ("THL6", "CE17", "J", "LR1", "F1")
//   platformName = human label   ("Platform 6", "Town Hall, Park St, Stand J",
//                                 "Town Hall Light Rail", "Taronga Zoo Wharf")
//                  — sometimes echoes the raw code (e.g. platformName = "THL6")
//
// Rules by product class:
//   cls 1/2/7 (Train / Metro / Coach):
//     platformName is "Platform N" → return it as-is.
//     platformName == platform code (API bug) → extract number → "Platform N".
//   cls 4 (Light Rail):
//     code present and differs from name → "$pCode · $pName" (e.g. "LR1 · Town Hall Light Rail")
//     no code (or code absent)           → just the name    (e.g. "Town Hall Light Rail")
//   cls 5 / 11 (Bus / School Bus):
//     extract "Stand X" from compound platformName via regex.
//   cls 9 (Ferry):
//     return the raw platform code only  (e.g. "F1").
//   unknown:
//     try regex on platformName, then disassembledName.
@Suppress("MagicNumber")
private fun DepartureMonitorResponse.Location.resolvePlatformText(productClass: Int?): String? {
    // Without a parent, this location IS the stop itself — no platform sub-label.
    parent ?: return null

    val pName = properties?.platformName
    val pCode = properties?.platform

    return when (productClass) {
        // ── Train (1), Metro (2), Coach (7) ──────────────────────────────────────
        // platformName is always "Platform N"; edge-case: API echoes raw code here.
        TransportMode.Train.productClass,
        TransportMode.Metro.productClass,
        TransportMode.Coach.productClass,
        -> resolveNumberedPlatform(pName, pCode, disassembledName)

        // ── Light Rail (4) ───────────────────────────────────────────────────────
        // Show code AND name: "LR1 · Central Chalmers Street Light Rail"
        // If the platform field is absent, just show the name.
        TransportMode.LightRail.productClass -> resolveLightRailPlatform(pName, pCode)

        // ── Bus (5), School Bus (11) ─────────────────────────────────────────────
        // Extract "Stand X" from compound platformName:
        //   "Central Station, Eddy Ave, Stand A" → "Stand A"
        TransportMode.Bus.productClass, TransportMode.SCHOOL_BUS_PRODUCT_CLASS -> (pName ?: disassembledName)?.let {
            extractPlatformText(
                it,
            )
        }

        // ── Ferry (9) ────────────────────────────────────────────────────────────
        // Wharf name is the stop name itself; raw code ("F1") is the useful label.
        TransportMode.Ferry.productClass -> pCode

        // ── Unknown / fallback ───────────────────────────────────────────────────
        else -> resolveFallbackPlatform(pName, disassembledName)
    }
}

// Train / Metro / Coach: prefer "Platform N" name, else derive number from a code that
// echoes the raw API value, else fall back to regex on disassembledName.
@Suppress("MagicNumber")
private fun resolveNumberedPlatform(pName: String?, pCode: String?, disassembledName: String?): String? =
    if (pName != null && pName != pCode) {
        // Normal case: "Platform 17", "Platform 26"
        extractPlatformText(pName)
    } else if (pCode != null) {
        // Bad case: platformName == "THL6" → derive "Platform 6" from code
        val num = Regex("\\d+").find(pCode)?.value
        num?.let { "Platform $it" }
    } else {
        // No properties at all — fall back to regex on disassembledName
        disassembledName?.let { extractPlatformText(it) }
    }

// Light Rail: show code AND name when both present and differ, else whichever exists.
private fun resolveLightRailPlatform(pName: String?, pCode: String?): String? = when {
    pCode != null && pName != null && pCode != pName -> "$pCode · $pName"
    pName != null -> pName
    pCode != null -> pCode
    else -> null
}

// Unknown mode: try regex on platformName (keep raw on miss), then disassembledName.
private fun resolveFallbackPlatform(pName: String?, disassembledName: String?): String? =
    pName?.let { extractPlatformText(it) ?: it }
        ?: disassembledName?.let { extractPlatformText(it) }

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
