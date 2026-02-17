package xyz.ksharma.krail.core.location

sealed class LocationError : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    data object PermissionDenied : LocationError("Location permission denied")
    data object LocationDisabled : LocationError("Location services are disabled")
    data object Timeout : LocationError("Location request timed out")
    data class Unknown(override val cause: Throwable?) : LocationError("Unknown location error", cause)
}
