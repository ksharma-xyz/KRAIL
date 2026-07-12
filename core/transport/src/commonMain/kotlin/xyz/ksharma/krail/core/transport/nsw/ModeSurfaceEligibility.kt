package xyz.ksharma.krail.core.transport.nsw

import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.TransportMode

/**
 * Per-surface eligibility policy for NSW transport modes.
 *
 * Exhaustive `when` (no `else`) by design: adding a new [TransportMode] subobject breaks
 * compilation here until its surface policy is declared. This keeps "which modes show where"
 * a single, deliberate decision instead of scattered `.filter` hacks at each call site.
 */
internal fun TransportMode.isAvailableOn(surface: ModeSelectionSurface): Boolean =
    when (this) {
        // School Bus is a real mode for trip planning, but never a stop type you pick on the map.
        TransportMode.SchoolBus -> surface == ModeSelectionSurface.TRIP_PLANNER
        // On Demand shuttles are booked per-trip, not a fixed stop you'd filter nearby stops by.
        TransportMode.OnDemand -> surface == ModeSelectionSurface.TRIP_PLANNER
        TransportMode.Train,
        TransportMode.Metro,
        TransportMode.Bus,
        TransportMode.LightRail,
        TransportMode.Ferry,
        TransportMode.Coach,
        -> true
    }
