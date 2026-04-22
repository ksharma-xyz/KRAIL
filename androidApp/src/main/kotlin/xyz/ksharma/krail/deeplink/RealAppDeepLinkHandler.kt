package xyz.ksharma.krail.deeplink

import android.net.Uri
import xyz.ksharma.krail.core.deeplink.PendingDeepLinkManager

internal class RealAppDeepLinkHandler(
    private val pendingDeepLinkManager: PendingDeepLinkManager,
) : AppDeepLinkHandler {

    override fun handleColdStart(uri: Uri?) {
        uri?.extractEncodedData()?.let { encodedData ->
            pendingDeepLinkManager.setPending(encodedData)
        }
    }

    override fun handleHotIntent(uri: Uri?) {
        uri?.extractEncodedData()?.let { encodedData ->
            pendingDeepLinkManager.dispatchHot(encodedData)
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
