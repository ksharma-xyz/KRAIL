package xyz.ksharma.krail.core.analytics

import dev.gitlive.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.log.log

class RealAnalytics(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val appInfoProvider: AppInfoProvider,
    private val coroutineScope: CoroutineScope,
) : Analytics {

    override fun track(event: AnalyticsEvent) {
        coroutineScope.launch {
            // Only track prod builds analytics events
            if (appInfoProvider.getAppInfo().isDebug.not()) {
                firebaseAnalytics.logEvent(event.name, event.properties)
            } else {
                log("ANALYTICS EVENT: $event")
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
        TODO("Not yet implemented")
    }
}
