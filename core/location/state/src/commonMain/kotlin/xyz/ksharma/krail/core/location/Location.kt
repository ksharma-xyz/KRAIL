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
        require(latitude in EarthConstants.MIN_LATITUDE..EarthConstants.MAX_LATITUDE) {
            "Latitude must be between ${EarthConstants.MIN_LATITUDE} and ${EarthConstants.MAX_LATITUDE}"
        }
        require(longitude in EarthConstants.MIN_LONGITUDE..EarthConstants.MAX_LONGITUDE) {
            "Longitude must be between ${EarthConstants.MIN_LONGITUDE} and ${EarthConstants.MAX_LONGITUDE}"
        }
    }
}
