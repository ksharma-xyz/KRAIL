package xyz.ksharma.krail.core.network.error

/**
 * Why a network call failed, in terms the app can act on.
 *
 * Before this existed every failure reached the UI as `isError: Boolean`, so
 * airplane mode, an NSW outage and a changed response shape were indistinguishable
 * and produced the same screen. These are the cases the app can tell apart and,
 * more importantly, the cases it should behave differently about.
 *
 * The taxonomy, including which platform exception maps to which case, is in
 * `docs/NETWORK_RELIABILITY.md`. That table is the specification for
 * `Throwable.toNetworkError` and the fixture its tests are written from; change
 * one and change the other.
 *
 * Two cases are easy to confuse and are deliberately separate:
 *
 *  - [Offline] means the device has no transport. It is the rider's connection.
 *  - [Unreachable] means the device has a working transport but the host could not
 *    be reached. It is not the rider's fault and it is worth retrying.
 *
 * Neither can be identified from the thrown exception alone: on Android both
 * airplane mode and a genuine DNS failure surface as `UnknownHostException`. That
 * is why the classifier takes the observed transport state as a second input.
 */
sealed interface NetworkError {

    /**
     * No network transport. The OS reported it was down at the moment the request
     * failed.
     *
     * The only case that resolves itself: when transport returns, the request is
     * worth repeating without the rider doing anything. See [recoversOnReconnect].
     */
    data object Offline : NetworkError

    /**
     * Transport was up but the host could not be reached. DNS failure, connection
     * refused, connection reset.
     *
     * Distinct from [Offline] because the message differs and the handling differs:
     * there is nothing for the rider to fix, and retrying may well work.
     */
    data object Unreachable : NetworkError

    /** The request did not complete in time. Connect, socket or whole-request. */
    data object Timeout : NetworkError

    /**
     * A network intercepted the request and answered with its own page, almost
     * always a Wi-Fi login screen.
     *
     * Detected by the response declaring a content type the endpoint never serves,
     * not by the deserialisation failure that follows, because by the time that
     * exception exists the headers are gone. Separate from [Malformed] because only
     * this one is fixable, and only by the rider.
     */
    data object CaptivePortal : NetworkError

    /**
     * The response arrived and could not be read: a shape change, a field that
     * stopped being nullable, a content type with no configured converter.
     *
     * Never retryable. The same request produces the same unreadable response, and
     * the fix is a code change.
     */
    data object Malformed : NetworkError

    /**
     * The upstream answered 5xx. NSW or the BFF is having a problem, the request
     * itself was fine.
     */
    data class Upstream(val code: Int) : NetworkError

    /**
     * The upstream answered 4xx. The request was wrong: a rejected API key, an
     * unknown stop id, a malformed parameter.
     *
     * Never retryable, because nothing about repeating it changes the answer.
     */
    data class Request(val code: Int) : NetworkError

    /**
     * Nothing in the taxonomy matched.
     *
     * A rising number of these means the taxonomy has drifted from what the
     * platforms actually throw, which is why the analytics event records the case
     * name.
     */
    data class Unknown(val cause: Throwable) : NetworkError
}

/**
 * Whether repeating the identical request could plausibly succeed.
 *
 * Deliberately an exhaustive `when` with no `else`: adding a case to
 * [NetworkError] must not compile until someone has decided what it means here.
 * That is the whole reason this lives beside the type instead of at each call
 * site.
 *
 * [NetworkError.Offline] is absent from the retryable set on purpose even though
 * it obviously can succeed later. Retrying immediately just burns the connect
 * timeout again; it recovers through [recoversOnReconnect] instead.
 */
val NetworkError.isRetryable: Boolean
    get() = when (this) {
        NetworkError.Unreachable,
        NetworkError.Timeout,
        is NetworkError.Upstream,
        -> true

        NetworkError.Offline,
        NetworkError.CaptivePortal,
        NetworkError.Malformed,
        is NetworkError.Request,
        is NetworkError.Unknown,
        -> false
    }

/**
 * Whether this failure should be retried automatically once transport returns.
 *
 * Only [NetworkError.Offline]. Everything else either had a working connection
 * already, so reconnecting changes nothing, or is not a connection problem at all.
 */
val NetworkError.recoversOnReconnect: Boolean
    get() = this == NetworkError.Offline

/**
 * Whether this failure is worth telling the rider about as a connection problem,
 * as opposed to a problem on our side or the upstream's.
 *
 * Kept here rather than in the UI layer so that the two screens that will render
 * this cannot disagree about which cases are the rider's connection.
 */
val NetworkError.isConnectionProblem: Boolean
    get() = when (this) {
        NetworkError.Offline,
        NetworkError.CaptivePortal,
        -> true

        NetworkError.Unreachable,
        NetworkError.Timeout,
        NetworkError.Malformed,
        is NetworkError.Upstream,
        is NetworkError.Request,
        is NetworkError.Unknown,
        -> false
    }

/**
 * Stable identifier for analytics and logs.
 *
 * Explicit rather than derived from the class name so that renaming a case cannot
 * silently split a metric in two.
 */
val NetworkError.kind: String
    get() = when (this) {
        NetworkError.Offline -> "offline"
        NetworkError.Unreachable -> "unreachable"
        NetworkError.Timeout -> "timeout"
        NetworkError.CaptivePortal -> "captive_portal"
        NetworkError.Malformed -> "malformed"
        is NetworkError.Upstream -> "upstream"
        is NetworkError.Request -> "request"
        is NetworkError.Unknown -> "unknown"
    }
