package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/** What the Add Park & Ride screen counts: a station taken off, and what the rider does from its map. */
internal fun Analytics.trackParkRideRemoved(station: ParkRideStationPickerItem) {
    track(
        AnalyticsEvent.ParkRideUserFacilityEvent(
            facilityId = station.mappings.joinToString { it.facilityId },
            stopId = station.stationId,
            action = AnalyticsEvent.ParkRideUserFacilityEvent.Action.REMOVE,
            source = AnalyticsEvent.ParkRideUserFacilityEvent.Source.ADD_PARK_RIDE_SCREEN,
        ),
    )
}

internal fun Analytics.trackParkRideMapCardOpened(stopId: String) {
    track(
        AnalyticsEvent.ParkRideActionEvent(
            action = AnalyticsEvent.ParkRideActionEvent.Action.MAP_CARD_OPENED,
            source = AnalyticsEvent.ParkRideActionEvent.Source.ADD_PARK_RIDE,
            stopId = stopId,
        ),
    )
}

internal fun Analytics.trackParkRideMapDirections(stopId: String, facilityId: String) {
    track(
        AnalyticsEvent.ParkRideActionEvent(
            action = AnalyticsEvent.ParkRideActionEvent.Action.DIRECTIONS,
            source = AnalyticsEvent.ParkRideActionEvent.Source.ADD_PARK_RIDE,
            stopId = stopId,
            facilityId = facilityId,
        ),
    )
}
