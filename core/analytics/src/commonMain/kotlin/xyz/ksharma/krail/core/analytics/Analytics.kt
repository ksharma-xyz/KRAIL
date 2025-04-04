package xyz.ksharma.krail.core.analytics

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

interface Analytics {

    fun track(event: AnalyticsEvent)

    fun setUserId(userId: String)

    fun setUserProperty(name: String, value: String)
}
