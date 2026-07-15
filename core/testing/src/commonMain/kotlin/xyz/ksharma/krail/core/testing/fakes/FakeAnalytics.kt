package xyz.ksharma.krail.core.testing.fakes

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

class FakeAnalytics : Analytics {

    private val trackedEvents = mutableListOf<AnalyticsEvent>()
    private var fakeUserId: String? = null

    fun isEventTracked(eventName: String): Boolean {
        println("trackedEvents: $trackedEvents")
        return trackedEvents.any { it.name == eventName }
    }

    fun getTrackedEvent(eventName: String): AnalyticsEvent? {
        return trackedEvents.find { it.name == eventName }
    }

    fun getTrackedEvents(eventName: String): List<AnalyticsEvent> {
        return trackedEvents.filter { it.name == eventName }
    }

    override fun track(event: AnalyticsEvent) {
        println("Tracking event: $event")
        trackedEvents.add(event)
    }

    override fun setUserId(userId: String) {
        fakeUserId = userId
    }

    override fun setUserProperty(name: String, value: String) {
        println("Setting user property $name to $value")
    }

    fun clear() {
        trackedEvents.clear()
        fakeUserId = null
    }
}
