package xyz.ksharma.krail.core.analytics

import dev.gitlive.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.log.log

internal class RealAnalytics(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val appInfoProvider: AppInfoProvider,
    private val coroutineScope: CoroutineScope,
) : Analytics {

    override fun track(event: AnalyticsEvent) {
        coroutineScope.launch {
            // Only track prod builds analytics events
            // Pane is attached here rather than at each call site: it is context about
            // where the rider was, not data the event knows about itself, and injecting
            // it centrally means events added later are attributed without anyone
            // remembering to do it.
            val properties = AnalyticsPaneTracker.decorate(event.properties)
            if (appInfoProvider.getAppInfo().isDebug.not()) {
                firebaseAnalytics.logEvent(event.name, properties)
            } else {
                log("ANALYTICS EVENT: $event pane=${properties[AnalyticsPaneTracker.PROP_PANE]}")
            }
        }
    }

    override fun setUserId(userId: String) {
        coroutineScope.launch {
            if (appInfoProvider.getAppInfo().isDebug) {
                firebaseAnalytics.setUserId("KRAIL-TEST-123")
            } else {
                firebaseAnalytics.setUserId(userId)
            }
        }
    }

    override fun setUserProperty(name: String, value: String) {
        coroutineScope.launch {
            // Same prod-only rule as track(): a debug session must not colour real
            // segments, and the log line is how these are verified on device.
            if (appInfoProvider.getAppInfo().isDebug.not()) {
                firebaseAnalytics.setUserProperty(name = name, value = value)
            } else {
                log("ANALYTICS USER PROPERTY: $name = $value")
            }
        }
    }
}
