package xyz.ksharma.krail.deeplink

import android.net.Uri
import xyz.ksharma.krail.core.deeplink.PendingDeepLinkManager
import xyz.ksharma.krail.core.log.log

internal class RealAppDeepLinkHandler(
    private val pendingDeepLinkManager: PendingDeepLinkManager,
) : AppDeepLinkHandler {

    override fun handleColdStart(uri: Uri?) {
        log("[DEEPLINK] handleColdStart — uri=$uri")
        val encodedData = uri?.extractEncodedData()
        if (encodedData != null) {
            log("[DEEPLINK] cold start — storing pending, encodedData=${encodedData.take(20)}…")
            pendingDeepLinkManager.setPending(encodedData)
        } else {
            log("[DEEPLINK] cold start — no valid deep link data (uri=${uri?.host}${uri?.path})")
        }
    }

    override fun handleHotIntent(uri: Uri?) {
        log("[DEEPLINK] handleHotIntent — uri=$uri")
        val encodedData = uri?.extractEncodedData()
        if (encodedData != null) {
            log("[DEEPLINK] hot intent — dispatching, encodedData=${encodedData.take(20)}…")
            pendingDeepLinkManager.dispatchHot(encodedData)
        } else {
            log("[DEEPLINK] hot intent — no valid deep link data (uri=${uri?.host}${uri?.path})")
        }
    }

    private fun Uri.extractEncodedData(): String? =
        takeIf { host == DEEP_LINK_HOST && path?.startsWith(DEEP_LINK_PATH_PREFIX) == true }
            ?.getQueryParameter(QUERY_PARAM_DATA)

    private companion object {
        const val DEEP_LINK_HOST = "ksharma-xyz.github.io"
        const val DEEP_LINK_PATH_PREFIX = "/trip"
        const val QUERY_PARAM_DATA = "d"
    }
}
