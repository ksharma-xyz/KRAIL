package xyz.ksharma.krail.core.appversion

import androidx.lifecycle.ViewModel
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.AppUpgradePromptEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.platform.ops.PlatformOps

class AppUpgradeViewModel(
    private val appInfoProvider: AppInfoProvider,
    private val platformOps: PlatformOps,
    private val analytics: Analytics,
) : ViewModel() {

    private var shownTracked = false

    /**
     * Sends the `shown` action once for this screen instance. The screen calls this from a `LaunchedEffect`,
     * which runs again after every Activity recreation, so the guard lives here: the ViewModel outlives a
     * rotation and the composable does not.
     */
    fun onShown() {
        if (shownTracked) return
        shownTracked = true
        analytics.track(AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.SHOWN))
    }

    fun onUpdateClick() {
        analytics.track(AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.UPDATE_CLICK))
        val appStoreUrl = appInfoProvider.getAppInfo().appStoreUrl
        platformOps.openUrl(url = appStoreUrl)
    }
}
