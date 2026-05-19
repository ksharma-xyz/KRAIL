package xyz.ksharma.krail.core.testing.fakes

import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.network.api.service.DeparturesService

/**
 * Canonical fake for [DeparturesService]. Promoted from the private inline class that
 * was being copy-pasted across departure-board tests.
 *
 * - [response] is the next value returned to `departures(...)`. Mutate before / between
 *   calls to drive different states (loading, refresh, error recovery).
 * - [shouldThrow] flips the next call into an error path (and every subsequent call until
 *   reset) so tests can exercise repository error handling without rebuilding the fake.
 * - [callCount] records how many times `departures` was invoked — useful for verifying
 *   that caching / refresh-throttling / `pollStop`-gating logic is wired correctly.
 */
class FakeDeparturesService(
    var response: DepartureMonitorResponse = DepartureMonitorResponse(stopEvents = emptyList()),
    var shouldThrow: Boolean = false,
) : DeparturesService {

    var callCount = 0
        private set

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): DepartureMonitorResponse {
        callCount++
        if (shouldThrow) error("Fake network error")
        return response
    }
}
