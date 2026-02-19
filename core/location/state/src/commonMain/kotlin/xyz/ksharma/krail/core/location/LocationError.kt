package xyz.ksharma.krail.core.location

sealed class LocationError(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** Location permission was not granted by the user. */
    class PermissionDenied : LocationError("Location permission denied")

    /** Unexpected error — inspect [cause] for details. */
    data class Unknown(override val cause: Throwable?) : LocationError("Unknown location error", cause)
}
