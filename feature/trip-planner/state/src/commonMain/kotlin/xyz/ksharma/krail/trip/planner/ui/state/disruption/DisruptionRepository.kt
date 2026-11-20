package xyz.ksharma.krail.trip.planner.ui.state.disruption

/**
 * Source of service disruptions. Optional on a screen: a failure is returned, never thrown, and
 * callers treat it as "no banner" rather than an error the rider sees.
 */
interface DisruptionRepository {
    suspend fun load(feed: DisruptionFeed): Result<List<Disruption>>
}
