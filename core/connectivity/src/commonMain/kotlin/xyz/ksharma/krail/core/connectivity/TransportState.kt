package xyz.ksharma.krail.core.connectivity

/**
 * What the operating system reports about the device's network transport.
 *
 * This is deliberately NOT called `isOnline`, and deliberately not a `Boolean`.
 *
 * The OS reports the state of the local transport, not whether anything can
 * actually be reached. A phone joined to a cafe Wi-Fi behind a captive portal
 * reports a usable network. So does one on a VPN whose tunnel has dropped, and
 * one on a carrier that is quietly blackholing packets. Treating this as "are we
 * online" produces an app that tells riders confident lies.
 *
 * The rule this type exists to enforce: **transport state is an input to
 * classifying a failure. The request outcome is the authority.** Nothing in the
 * app renders a claim about connectivity sourced from this value alone.
 *
 * See `docs/NETWORK_RELIABILITY.md`.
 */
enum class TransportState {

    /**
     * The OS reports a network that it has validated as having real end-to-end
     * connectivity.
     *
     * On Android this means `NET_CAPABILITY_VALIDATED`, not merely
     * `NET_CAPABILITY_INTERNET` — the latter is true on a captive portal.
     */
    Up,

    /** The OS reports no usable network. Airplane mode, no radio, nothing joined. */
    Down,

    /**
     * No callback has arrived yet, so nothing is known.
     *
     * This is the initial value and it matters. Reporting [Down] before the first
     * callback would make a perfectly good first request fail fast for no reason,
     * which is the exact bug a naive `isOnline = false` default produces.
     *
     * Read it as "do not act on this yet": [shouldAttemptRequest] treats it as
     * permission to try, and the failure classifier treats it as grounds NOT to
     * claim the rider is offline.
     */
    Unknown,
}

/**
 * Whether a request is worth attempting.
 *
 * [TransportState.Unknown] counts as yes. The cost of a pointless attempt is one
 * failed request; the cost of wrongly refusing is a screen that shows an offline
 * message while the device is perfectly connected.
 */
val TransportState.shouldAttemptRequest: Boolean
    get() = this != TransportState.Down

/**
 * Whether this state is strong enough evidence to tell a rider they have no
 * network.
 *
 * Only [TransportState.Up] is ruled out. [TransportState.Unknown] is not evidence
 * of anything, so a failure observed while Unknown is classified as unreachable
 * rather than offline.
 */
val TransportState.mayClaimOffline: Boolean
    get() = this == TransportState.Down
