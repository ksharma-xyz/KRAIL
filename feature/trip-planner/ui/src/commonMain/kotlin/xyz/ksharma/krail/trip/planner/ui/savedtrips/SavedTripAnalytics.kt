package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.searchstop.labelKindOf
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind

internal fun Analytics.trackDeleteSavedTrip(fromStopId: String, toStopId: String) {
    track(
        AnalyticsEvent.DeleteSavedTripClickEvent(
            fromStopId = fromStopId,
            toStopId = toStopId,
        ),
    )
}

internal fun Analytics.trackSavedTripCardClick(fromStopId: String, toStopId: String) {
    track(AnalyticsEvent.SavedTripCardClickEvent(fromStopId = fromStopId, toStopId = toStopId))
}

internal fun Analytics.trackLoadTimeTableClick(
    fromStopId: String,
    toStopId: String,
    source: AnalyticsEvent.LoadTimeTableClickEvent.Source,
    searchSessionId: String?,
) {
    track(
        AnalyticsEvent.LoadTimeTableClickEvent(
            fromStopId = fromStopId,
            toStopId = toStopId,
            source = source,
            searchSessionId = searchSessionId,
        ),
    )
}

internal fun Analytics.trackHomeCardParkRideDirections(stopId: String, facilityId: String) {
    track(
        AnalyticsEvent.ParkRideActionEvent(
            action = AnalyticsEvent.ParkRideActionEvent.Action.DIRECTIONS,
            source = AnalyticsEvent.ParkRideActionEvent.Source.HOME_CARD,
            stopId = stopId,
            facilityId = facilityId,
        ),
    )
}

/** A label set from its own chip on the home screen, which is a different path from setting one on Search Stop. */
internal fun Analytics.trackLabelSetFromHomeChip(
    labelKey: String,
    locationKind: LocationKind,
    isReassignment: Boolean,
) {
    track(
        AnalyticsEvent.StopLabelStopAssignedEvent(
            assignmentSurface = AnalyticsEvent.StopLabelSurface.HOME_CHIP,
            assignmentMode = AnalyticsEvent.StopLabelStopAssignedEvent.AssignmentMode.EXISTING_LABEL,
            locationKind = when (locationKind) {
                LocationKind.TRANSIT_STOP -> AnalyticsEvent.StopSelectedEvent.LocationKind.TRANSIT_STOP
                LocationKind.ADDRESS -> AnalyticsEvent.StopSelectedEvent.LocationKind.ADDRESS
            },
            labelKind = labelKindOf(labelKey),
            isReassignment = isReassignment,
        ),
    )
}
