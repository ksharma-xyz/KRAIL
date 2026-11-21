package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.CompletableDeferred
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

/**
 * Counts insert calls instead of decoding a protobuf into SQLDelight. Callers that wire both the
 * stops and the bus routes manager use two independent instances, so the call record is per
 * instance.
 *
 * Results come from [enqueue] first, then [result]. Set [gate] to hold every call until the test
 * completes it, which is how a test keeps the insert in progress. Set [throwOnInsert] to make a
 * call throw instead of returning a Result, as a manager that breaks its contract would.
 */
class FakeStopsManager(var result: Result<Unit> = Result.success(Unit)) : StopsManager {

    var insertStopsCallCount = 0
        private set

    var gate: CompletableDeferred<Unit>? = null

    var throwOnInsert: Throwable? = null

    private val queued = ArrayDeque<Result<Unit>>()

    fun enqueue(results: List<Result<Unit>>) {
        queued.addAll(results)
    }

    override suspend fun insertStops(): Result<Unit> {
        insertStopsCallCount++
        gate?.await()
        throwOnInsert?.let { throw it }
        return queued.removeFirstOrNull() ?: result
    }
}
