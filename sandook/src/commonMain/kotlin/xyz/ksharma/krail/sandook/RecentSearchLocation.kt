package xyz.ksharma.krail.sandook

/**
 * The persisted representation of a location selected in trip planning.
 *
 * Unlike GTFS stops, address and place IDs from `stop_finder` have no local database
 * row. Keeping their display metadata here lets them remain useful after the remote
 * result has expired.
 */
data class RecentSearchLocation(
    val locationId: String,
    val displayName: String,
    val kind: String,
    val addressType: String?,
    val productClasses: String,
)
