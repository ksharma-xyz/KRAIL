package xyz.ksharma.krail.trip.planner.ui.departureboard

import xyz.ksharma.krail.core.transport.TransportMode

/**
 * Maps a [String] transport mode name (e.g. [StopDeparture.transportModeName])
 * to the corresponding [TransportMode] sealed class instance.
 *
 * Drives the lookup from [TransportMode.all] so no hardcoded strings are needed —
 * adding a new mode to the sealed class automatically makes it resolvable here.
 *
 * Returns `null` for unknown or unrecognised mode names.
 */
fun String.toTransportMode(): TransportMode? =
    TransportMode.all.firstOrNull { it.name.equals(trim(), ignoreCase = true) }

/**
 * Returns the single uppercase initial of the transport mode name for use inside
 * the [TransportModeLineBadge] circle.
 *
 * Examples: `"Train"` → `"T"`, `"Bus"` → `"B"`, `"Light Rail"` → `"L"`
 */
fun String.toTransportModeInitial(): String = trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
