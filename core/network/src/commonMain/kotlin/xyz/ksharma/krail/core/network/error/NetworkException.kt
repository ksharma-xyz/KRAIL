package xyz.ksharma.krail.core.network.error

/**
 * The only throwable a `Real*Service` puts into a failed `Result`.
 *
 * ## Why this rather than a sealed ApiResult
 *
 * A dedicated `ApiResult<T>` would make failure impossible to ignore, which is
 * genuinely better in the abstract. It would also rewrite every service signature,
 * every call site and every fake in a codebase where `suspendSafeResult` already
 * threads `kotlin.Result` everywhere and already handles `CancellationException`
 * correctly.
 *
 * The trade taken here: keep `Result`, and guarantee that its failure is always
 * this type. Same information at the call site, a fraction of the churn.
 *
 * The looseness being accepted is that `Result` allows a failure to be dropped
 * silently. If that produces a real bug, the decision is worth revisiting; that
 * revisit trigger is recorded in `docs/NETWORK_RELIABILITY.md`.
 *
 * @param error what went wrong, in terms the app can act on.
 * @param cause the original platform throwable, kept for logging. Never
 *   inspected by app code: [error] is what classification already concluded, and
 *   re-deriving it at a call site is how two screens end up disagreeing.
 */
class NetworkException(
    val error: NetworkError,
    cause: Throwable? = null,
) : Exception(error.kind, cause)

/**
 * Reads the [NetworkError] out of a throwable that came from a service.
 *
 * Falls back to [NetworkError.Unknown] rather than throwing, because a call site
 * handling a failure is the worst possible place to introduce a second failure.
 * A service that returns something other than a [NetworkException] is a bug in
 * that service, and the guard test named in `docs/NETWORK_RELIABILITY.md` is what
 * catches it.
 */
fun Throwable.asNetworkError(): NetworkError =
    (this as? NetworkException)?.error ?: NetworkError.Unknown(this)

/** Convenience for the common `Result` shape returned by every service. */
fun <T> Result<T>.networkErrorOrNull(): NetworkError? =
    exceptionOrNull()?.asNetworkError()
