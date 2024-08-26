package xyz.ksharma.krail.coroutines.ext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.Result

/**
 * Executes the given block on the specified [CoroutineDispatcher] and returns the result in a [Result] object.
 *
 * If the block execution is successful, the result is wrapped in a [Result.success].
 * If an exception is thrown, it is wrapped in a [Result.Failure].
 *
 * **Note:** This function will not catch [CancellationException].
 *
 * @param dispatcher The CoroutineDispatcher on which to execute the block.
 * @param block The block of code to execute.
 * @return A [Result] object containing the result of the block execution or the exception that was thrown.
 */
suspend fun <T, R> T.safeResult(
    dispatcher: CoroutineDispatcher,
    block: T.() -> R
): Result<R> = withContext(dispatcher) {
    try {
        Result.success(block())
    } catch (e: Throwable) {
        // Should not catch CancellationException
        coroutineContext.ensureActive()
        Result.failure(e)
    }
}
