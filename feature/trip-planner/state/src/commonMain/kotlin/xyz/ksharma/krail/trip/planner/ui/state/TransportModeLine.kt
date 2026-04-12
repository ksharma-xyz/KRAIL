package xyz.ksharma.krail.trip.planner.ui.state

import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine

/**
 * Reference - https://en.wikipedia.org/wiki/Module:Adjacent_stations/Sydney_Trains
 * - https://transportnsw.info/routes/details/newcastle-light-rail/nlr/
 *
 * Line colours are sourced from `NswTransportLine` in :core:transport — single source of truth.
 */
data class TransportModeLine(
    /**
     * Train / Bus / Ferry etc along with their color codes.
     */
    val transportMode: TransportMode,

    /**
     * Line number e.g. T1, T4, F1, F2 etc.
     */
    val lineName: String,

    val lineColorCode: String = NswTransportLine.entries
        .firstOrNull { it.key == lineName }
        ?.hexColor
        ?: NswTransportConfig.colorFor(transportMode),
)
