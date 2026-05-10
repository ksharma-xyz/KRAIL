package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Debug-only override for the single Firebase Remote Config flag
 * `enable_proto_bff` that controls whether the app routes traffic through
 * the BFF or hits NSW direct.
 *
 * Release builds always read [FOLLOW_RC]; the override path only kicks in
 * when `AppInfo.isDebug` is true. Default for new installs is [FOLLOW_RC]
 * so debug builds match what the production cohort sees until the developer
 * flips a row in the Debug Config UI.
 */
enum class FlagOverride {
    /** Read the live Firebase RC value for `enable_proto_bff`. */
    FOLLOW_RC,

    /** Force the BFF path on regardless of the RC value. */
    FORCE_ON,

    /** Force the NSW direct path regardless of the RC value. */
    FORCE_OFF,
}
