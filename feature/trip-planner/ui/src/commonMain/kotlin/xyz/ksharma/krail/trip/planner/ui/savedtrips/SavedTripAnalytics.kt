package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

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

internal fun Analytics.trackLoadTimeTableClick(fromStopId: String, toStopId: String) {
    track(AnalyticsEvent.LoadTimeTableClickEvent(fromStopId = fromStopId, toStopId = toStopId))
}
