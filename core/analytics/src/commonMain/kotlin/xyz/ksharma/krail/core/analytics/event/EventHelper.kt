package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen

fun Analytics.trackScreenViewEvent(screen: AnalyticsScreen) {
    track(event = AnalyticsEvent.ScreenViewEvent(screen))
}
