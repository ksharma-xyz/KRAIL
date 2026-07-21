package xyz.ksharma.krail.platform.ops

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks whichever Activity is currently resumed.
 *
 * Some Play APIs (in-app review, in-app updates) need a real Activity and will not accept
 * the application Context that [AndroidPlatformOps] holds. Rather than each of those
 * reaching for its own static, they read the current Activity from here.
 *
 * The reference is weak, so a held instance can never keep a destroyed Activity alive, and
 * it is cleared on pause so a backgrounded app reports no Activity at all.
 */
class CurrentActivityHolder {

    private var activityRef: WeakReference<Activity>? = null

    /** The resumed Activity, or `null` if nothing is in the foreground. */
    val current: Activity?
        get() = activityRef?.get()

    /**
     * Registers lifecycle callbacks so this holder stays current. Call once, from
     * `Application.onCreate`.
     */
    fun startTracking(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityResumed(activity: Activity) {
                    activityRef = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (current === activity) activityRef = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}
