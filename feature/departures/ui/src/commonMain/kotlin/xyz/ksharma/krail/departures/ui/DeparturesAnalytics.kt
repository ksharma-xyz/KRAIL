package xyz.ksharma.krail.departures.ui

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent

internal fun Analytics.trackDepartureBoardScreenView(
    stopId: String,
    stopName: String,
    source: DepartureBoardSource,
) {
    trackScreenViewEvent(AnalyticsScreen.DepartureBoard)
    track(AnalyticsEvent.DepartureBoardScreenViewEvent(stopId = stopId, stopName = stopName, source = source))
}

fun Analytics.trackDepartureBoardLineFilterClick(
    stopId: String,
    selected: Boolean,
    lineNumber: String?,
    transportMode: String?,
    source: DepartureBoardSource,
) {
    track(
        AnalyticsEvent.DepartureBoardLineFilterClickEvent(
            stopId = stopId,
            selected = selected,
            lineNumber = lineNumber,
            transportMode = transportMode,
            source = source,
        ),
    )
}

fun Analytics.trackDepartureBoardShowPrevious(
    stopId: String,
    show: Boolean,
    source: DepartureBoardSource,
) {
    track(AnalyticsEvent.DepartureBoardShowPreviousEvent(stopId = stopId, show = show, source = source))
}

fun Analytics.trackDepartureBoardStopClick(stopId: String, stopName: String, expand: Boolean) {
    track(AnalyticsEvent.DepartureBoardStopClickEvent(stopId = stopId, stopName = stopName, expand = expand))
}

fun Analytics.trackNearbyStopDepartureBoardClick(stopId: String, stopName: String, expand: Boolean) {
    track(AnalyticsEvent.NearbyStopDepartureBoardClickEvent(stopId = stopId, stopName = stopName, expand = expand))
}
