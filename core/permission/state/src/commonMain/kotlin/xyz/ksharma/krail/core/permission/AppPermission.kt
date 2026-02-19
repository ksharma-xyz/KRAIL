package xyz.ksharma.krail.core.permission

/**
 * Represents a permission that the app can request from the user.
 *
 * Add new sealed subclasses here as the app requires more permission types
 * (e.g. Camera, Microphone, Contacts).
 */
sealed class AppPermission {

    /** Location permissions — used for map and nearby-stops features. */
    sealed class Location : AppPermission() {

        /** Fine location while the app is in the foreground (most common case). */
        data object WhenInUse : Location()

        /** Coarse location only (approximate position). */
        data object Coarse : Location()
    }
}
