package xyz.ksharma.krail.core.maps.data.location

import androidx.compose.runtime.staticCompositionLocalOf
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * What the app remembers and reports about the location permission. [UserLocationManager] is built for one
 * screen at a time, so what has to outlive a screen is kept here.
 */
interface LocationPermissionRecorder {
    /**
     * True when the rider's last known answer to the location permission was no. It changes what analytics
     * reports and nothing the rider sees: Android can forget it ever asked, and then reads as never asked.
     */
    var wasRefused: Boolean

    /** One real permission request came back. */
    fun onRequestAnswered(
        source: PermissionResultEvent.Source,
        outcome: PermissionResultEvent.Outcome,
        canAskAgain: Boolean?,
    )
}

/** Remembers nothing and reports nothing. For previews and for hosts that wire no recorder. */
object NoLocationPermissionRecorder : LocationPermissionRecorder {
    override var wasRefused: Boolean
        get() = false
        set(_) = Unit

    override fun onRequestAnswered(
        source: PermissionResultEvent.Source,
        outcome: PermissionResultEvent.Outcome,
        canAskAgain: Boolean?,
    ) = Unit
}

/** The recorder [rememberUserLocationManager] uses. The app's root provides the real one. */
val LocalLocationPermissionRecorder = staticCompositionLocalOf<LocationPermissionRecorder> {
    NoLocationPermissionRecorder
}

private const val PREF_LOCATION_REFUSED = "location_permission_refused"

internal class RealLocationPermissionRecorder(
    private val analytics: Analytics,
    private val preferences: SandookPreferences,
) : LocationPermissionRecorder {
    override var wasRefused: Boolean
        get() = preferences.getBoolean(PREF_LOCATION_REFUSED) == true
        set(value) {
            // Written only on a change: the status is read every time a map comes to the front.
            if (value != wasRefused) preferences.setBoolean(PREF_LOCATION_REFUSED, value)
        }

    override fun onRequestAnswered(
        source: PermissionResultEvent.Source,
        outcome: PermissionResultEvent.Outcome,
        canAskAgain: Boolean?,
    ) {
        analytics.track(
            PermissionResultEvent(
                permission = PermissionResultEvent.Permission.LOCATION,
                outcome = outcome,
                source = source,
                canAskAgain = canAskAgain,
            ),
        )
    }
}
