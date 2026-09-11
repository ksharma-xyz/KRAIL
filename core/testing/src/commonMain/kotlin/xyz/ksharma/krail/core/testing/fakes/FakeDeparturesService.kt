package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.yield
import xyz.ksharma.krail.core.network.error.NetworkError
import xyz.ksharma.krail.core.network.error.NetworkException
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
 * - [failWith] names *which* failure. A test that only needs "something went wrong" can keep
 *   using [shouldThrow]; a test that asserts on offline versus upstream behaviour sets this.
 * - [callCount] records how many times `departures` was invoked — useful for verifying
 *   that caching / refresh-throttling / `pollStop`-gating logic is wired correctly.
 */
class FakeDeparturesService(
    var response: DepartureMonitorResponse = DepartureMonitorResponse(stopEvents = emptyList()),
    var shouldThrow: Boolean = false,
    var failWith: NetworkError? = null,
) : DeparturesService {

    var callCount = 0
        private set

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): Result<DepartureMonitorResponse> {
        callCount++
        // The real service always crosses a dispatcher inside NetworkCaller, so it always
        // suspends. A fake that returns without suspending lets a caller's "loading" state
        // and its result coalesce into one emission, which hides a real conflation bug
        // rather than reproducing production behaviour.
        yield()
        // failWith wins over shouldThrow so a test can set the specific error without also
        // having to remember to flip the older flag.
        val failure = failWith ?: NetworkError.Unknown(
            cause = RuntimeException("Fake network error"),
        ).takeIf { shouldThrow }

        return failure
            ?.let { Result.failure(NetworkException(error = it)) }
            ?: Result.success(response)
    }
}
