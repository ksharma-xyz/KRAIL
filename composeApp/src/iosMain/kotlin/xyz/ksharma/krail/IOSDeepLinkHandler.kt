package xyz.ksharma.krail

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.ksharma.krail.core.deeplink.PendingDeepLinkManager

/**
 * iOS entry point for deep link handling.
 *
 * Called from Swift in both Universal Link callbacks:
 * - `onContinueUserActivity(NSUserActivityTypeBrowsingWeb)` — Universal Links via NSUserActivity
 * - `onOpenURL` — fallback / custom-scheme URLs
 *
 * Always calls [PendingDeepLinkManager.dispatchHot] regardless of cold/hot start.
 * The Splash guard in `KrailNavHost` correctly handles the cold-start case:
 * - If Splash is still active → hot-event is skipped; SplashEntry calls `consumePending()`.
 * - If Splash has finished → hot-event navigates directly to TrackTripRoute.
 *
 * iOS apps are single-instance by default so there is no `singleTask` / `onNewIntent`
 * equivalent needed — all URL deliveries go through the same callbacks.
 */
class IOSDeepLinkHandler : KoinComponent {

    private val pendingDeepLinkManager: PendingDeepLinkManager by inject()

    /**
     * Handle a URL string delivered by the OS (Universal Link or custom scheme).
     * Pass `url.absoluteString` from Swift.
     */
    fun handle(urlString: String?) {
        urlString
            ?.extractEncodedData()
            ?.let { pendingDeepLinkManager.dispatchHot(it) }
    }

    private fun String.extractEncodedData(): String? {
        if (!contains("ksharma-xyz.github.io")) return null
        // Path must start with /trip
        val pathStart = indexOf("ksharma-xyz.github.io") + "ksharma-xyz.github.io".length
        val pathSection = substring(pathStart)
        if (!pathSection.startsWith("/trip")) return null
        // Extract `d` query parameter
        val queryStart = indexOf('?').takeIf { it >= 0 }?.plus(1) ?: return null
        val query = substring(queryStart)
        return query.split("&")
            .firstOrNull { it.startsWith("d=") }
            ?.removePrefix("d=")
            ?.takeIf { it.isNotBlank() }
    }
}
