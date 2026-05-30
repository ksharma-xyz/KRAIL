package xyz.ksharma.krail.trip.planner.ui.state.mapstopselection

import xyz.ksharma.krail.core.maps.state.LatLng

/**
 * Events handled by `MapStopSelectionViewModel`. Kept deliberately small — only the
 * actions that mutate VM state. UI-internal events (sheet open/close, marker tap
 * highlighting) live in the composable.
 */
sealed interface MapStopSelectionEvent {
    /** User's GPS location updated. null = lost / not granted. Triggers nearby-stops reload. */
    data class UserLocationUpdated(val location: LatLng?) : MapStopSelectionEvent

    /** Map camera moved to a new center. Triggers nearby-stops reload for the new position. */
    data class MapCenterChanged(val center: LatLng) : MapStopSelectionEvent
}
