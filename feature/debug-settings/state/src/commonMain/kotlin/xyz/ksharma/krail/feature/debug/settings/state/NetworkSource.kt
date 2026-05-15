package xyz.ksharma.krail.feature.debug.settings.state

/**
 * The single knob a debug build uses to decide where BFF-eligible network
 * calls go. Replaces the earlier two-knob design (`FlagOverride` plus
 * `NetworkTarget`) with one flat enum so the Debug Config UI is one screen
 * and one decision.
 *
 * Release builds always behave as [FOLLOW_RC]; the four overrides below
 * only take effect when `AppInfo.isDebug` is `true`. Default for fresh
 * installs is [FOLLOW_RC] so debug builds match the production cohort
 * until a developer flips a row in the Network screen.
 */
enum class NetworkSource {
    /**
     * Follow the live Firebase Remote Config value of `enable_proto_bff`.
     *
     * When the flag is `true` the resolver picks the BFF; in release that
     * is the deployed BFF Prod URL, in debug that is the local BFF URL so
     * developers hit their own BFF when "following" the production rollout.
     * When the flag is `false` (or the resolved BFF URL is blank) the
     * resolver falls through to NSW direct.
     */
    FOLLOW_RC,

    /** Force NSW direct, ignoring the RC value entirely. */
    NSW_DIRECT,

    /**
     * Force BFF and hit the local dev URL (`KRAIL_BFF_BASE_URL`, sourced
     * from `local.properties` `krail.bffBaseUrl`). Empty URL falls back to
     * NSW direct so a misconfigured local opt-in cannot break the app.
     */
    BFF_LOCAL,

    /**
     * Force BFF and hit the deployed prod URL (`KRAIL_BFF_PROD_BASE_URL`,
     * sourced from `local.properties` `krail.bffProdBaseUrl`). Empty URL
     * falls back to NSW direct so a `BFF_PROD` selection cannot break the
     * app before the BFF actually ships.
     */
    BFF_PROD,
}
