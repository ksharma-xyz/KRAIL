package xyz.ksharma.krail.core.location

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val altitudeAccuracy: Double? = null,
    val speed: Double? = null,
    val speedAccuracy: Double? = null,
    val bearing: Double? = null,
    val bearingAccuracy: Double? = null,
    val timestamp: Long,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
    }
}
