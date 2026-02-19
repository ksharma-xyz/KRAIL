package xyz.ksharma.krail.core.location

data class LocationConfig(
    val updateIntervalMs: Long = 30_000L,
    val minDistanceMeters: Float = 0f,
    val priority: LocationPriority = LocationPriority.HIGH_ACCURACY,
) {
    init {
        require(updateIntervalMs > 0) { "Update interval must be positive" }
        require(minDistanceMeters >= 0) { "Minimum distance must be non-negative" }
    }
}
