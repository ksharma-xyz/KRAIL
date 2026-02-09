package xyz.ksharma.krail.core.appversion

import androidx.lifecycle.ViewModel
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.platform.ops.PlatformOps

class AppUpgradeViewModel(
    private val appInfoProvider: AppInfoProvider,
    private val platformOps: PlatformOps,
) : ViewModel() {

    fun onUpdateClick() {
        val appStoreUrl = appInfoProvider.getAppInfo().appStoreUrl
        platformOps.openUrl(url = appStoreUrl)
    }
}
