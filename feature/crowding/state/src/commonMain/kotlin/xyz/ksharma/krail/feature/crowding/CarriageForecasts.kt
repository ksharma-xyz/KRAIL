package xyz.ksharma.krail.feature.crowding

/**
 * Per-carriage forecasts from one download of the Sydney Trains trip updates feed, keyed by
 * `RealtimeTripId` and then by the platform stop id the train leaves from.
 *
 * Immutable, so a screen can hold the one it loaded in its state and redraw from it after a
 * rotation without fetching again.
 */
class CarriageIndex(private val byTrip: Map<String, Map<String, CarriageLevels>>) {

    val tripCount: Int get() = byTrip.size

    /** The cars as the train leaves [stopId]. Null when the feed said nothing about it. */
    fun at(realtimeTripId: String?, stopId: String?): CarriageLevels? {
        if (realtimeTripId.isNullOrBlank() || stopId.isNullOrBlank()) return null
        return byTrip[realtimeTripId]?.get(stopId)
    }

    override fun equals(other: Any?): Boolean = other is CarriageIndex && other.byTrip == byTrip

    override fun hashCode(): Int = byTrip.hashCode()

    companion object {
        val EMPTY = CarriageIndex(emptyMap())
    }
}

/**
 * The one extra download crowding costs: the Sydney Trains trip updates feed, about 285 KB for
 * the whole network and impossible to filter per trip.
 *
 * Call [load] once per results screen. Implementations share one cached copy across callers,
 * so the departure board and trip results opened back to back do not download it twice. When
 * the `crowding_carriages_in_results_enabled` kill switch is off, [load] succeeds with
 * [CarriageIndex.EMPTY] and makes no request.
 *
 * A failure is a `NetworkException` (docs/NETWORK_RELIABILITY.md). Nothing tells the rider:
 * the carriage row simply folds away.
 */
interface CarriageForecastRepository {
    suspend fun load(): Result<CarriageIndex>
}
