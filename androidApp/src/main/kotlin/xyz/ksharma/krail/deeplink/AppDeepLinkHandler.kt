package xyz.ksharma.krail.deeplink

import android.net.Uri

/**
 * Handles incoming deep links on Android.
 *
 * Separates platform-level URI parsing from navigation logic so [MainActivity] stays
 * thin and the matching/dispatch rules are in one testable place.
 *
 * ## Two dispatch paths
 *
 * | Caller | Method | Effect |
 * |--------|--------|--------|
 * | `MainActivity.onCreate` | [handleColdStart] | Stored in `PendingDeepLinkManager`; consumed by Splash after navigation settles |
 * | `MainActivity.onNewIntent` | [handleHotIntent] | Stored AND emitted on `hotEvents`; nav host reacts immediately |
 */
interface AppDeepLinkHandler {

    /** Called from `onCreate`. Stores the deep link for the Splash entry to consume. */
    fun handleColdStart(uri: Uri?)

    /**
     * Called from `onNewIntent`. Stores the deep link and dispatches a hot event so the
     * nav host can navigate immediately without waiting for Splash.
     */
    fun handleHotIntent(uri: Uri?)
}
