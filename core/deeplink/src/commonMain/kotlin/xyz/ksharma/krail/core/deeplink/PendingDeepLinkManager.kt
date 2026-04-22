package xyz.ksharma.krail.core.deeplink

import kotlinx.coroutines.flow.SharedFlow

/**
 * Holds a deep-link encoded payload until the navigation stack is ready to consume it.
 *
 * There are two dispatch paths:
 *
 * **Cold start** (app not running): call [setPending] in `onCreate`. The Splash entry then
 * calls [consumePending] once navigation has settled to hand the encoded payload to the navigator.
 *
 * **Hot intent** (app already running): call [dispatchHot] from `onNewIntent`. This stores
 * the payload AND emits on [hotEvents] so that the nav host can react immediately without
 * waiting for Splash to complete.
 */
interface PendingDeepLinkManager {

    /**
     * Emits when a deep link arrives while the app is already running (via `onNewIntent`).
     * The nav host collects this to navigate immediately, bypassing the Splash flow.
     *
     * Backed by a `SharedFlow` with a buffer of 1 — if no collector is active yet during
     * the startup race window, the event is held and delivered when collection starts.
     */
    val hotEvents: SharedFlow<String>

    /** Called from `onCreate` — stores the deep link for Splash to consume after navigation settles. */
    fun setPending(encodedData: String)

    /**
     * Called from `onNewIntent` — stores the deep link AND emits on [hotEvents] so the
     * nav host can navigate immediately without waiting for Splash to complete.
     */
    fun dispatchHot(encodedData: String)

    /** Returns and clears the stored payload. Returns null if nothing is pending. */
    fun consumePending(): String?

    fun hasPending(): Boolean
}
