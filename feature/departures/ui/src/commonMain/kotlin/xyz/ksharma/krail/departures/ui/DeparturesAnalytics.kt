package xyz.ksharma.krail.departures.ui

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource

internal fun Analytics.trackDepartureBoardScreenView(
    stopId: String,
    stopName: String,
    source: DepartureBoardSource,
) {
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

fun Analytics.trackDepartureBoardToggle(
    stopId: String,
    stopName: String,
    expand: Boolean,
    source: DepartureBoardSource,
) {
    track(
        AnalyticsEvent.DepartureBoardToggleEvent(
            stopId = stopId,
            stopName = stopName,
            expand = expand,
            source = source,
        ),
    )
}

fun Analytics.trackDepartureBoardStatus(
    stopId: String,
    action: AnalyticsEvent.DepartureBoardStatusEvent.Action,
    source: DepartureBoardSource,
) {
    track(AnalyticsEvent.DepartureBoardStatusEvent(stopId = stopId, action = action, source = source))
}
