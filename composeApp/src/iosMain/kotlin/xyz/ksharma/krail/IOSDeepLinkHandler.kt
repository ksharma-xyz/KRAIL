package xyz.ksharma.krail

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.ksharma.krail.core.deeplink.KRAIL_DEEP_LINK_HOST
import xyz.ksharma.krail.core.deeplink.KRAIL_DEEP_LINK_PATH
import xyz.ksharma.krail.core.deeplink.PendingDeepLinkManager
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean

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
    private val flag: Flag by inject()

    /**
     * Handle a URL string delivered by the OS (Universal Link or custom scheme).
     * Pass `url.absoluteString` from Swift.
     *
     * Silently ignored when [FlagKeys.TRIP_TRACKING_ENABLED] is off so deep links
     * land on the website instead of opening the track-trip screen.
     */
    fun handle(urlString: String?) {
        if (!flag.getFlagValue(FlagKeys.TRIP_TRACKING_ENABLED.key).asBoolean(false)) return
        urlString
            ?.extractEncodedData()
            ?.let { pendingDeepLinkManager.dispatchHot(it) }
    }

    private fun String.extractEncodedData(): String? {
        val queryStart = indexOf(KRAIL_DEEP_LINK_HOST)
            .takeIf { it >= 0 }
            ?.let { hostIdx ->
                val pathSection = substring(hostIdx + KRAIL_DEEP_LINK_HOST.length)
                indexOf('?').takeIf { pathSection.startsWith(KRAIL_DEEP_LINK_PATH) && it >= 0 }?.plus(1)
            }
            ?: return null
        return substring(queryStart).split("&")
            .firstOrNull { it.startsWith("d=") }
            ?.removePrefix("d=")
            ?.takeIf { it.isNotBlank() }
    }
}
