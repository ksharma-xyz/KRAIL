package xyz.ksharma.krail.core.appreview

/**
 * Decides when to ask the platform for a review sheet, and counts the engagement that
 * feeds that decision.
 *
 * ## Why there is no dialog here
 *
 * Both stores forbid the "custom pre-prompt, then gate on the answer" pattern:
 *
 * - **Google Play In-App Review policy** — must not precede the API with any custom prompt
 *   or question, must not gate by sentiment, must not trigger from a button.
 * - **Apple HIG (Ratings and reviews) / Guideline 1.1.7** — do not call it in response to a
 *   button tap, do not show a custom prompt that mimics or precedes the system alert, and
 *   request only after demonstrated engagement.
 *
 * So this class shows no UI of its own and asks the user nothing. It watches for a moment
 * that already demonstrates engagement, calls the platform API, and lets the OS decide
 * whether anything appears. Any change here that adds a KRAIL-authored prompt, or filters
 * on how the user feels, breaks both policies.
 *
 * A user-initiated "Rate KRAIL" row in Settings is a different thing and is allowed, because
 * it deep-links to the store listing rather than driving this API.
 *
 * ## What gates a request
 *
 * State comes from the user-lifecycle store, policy comes from Remote Config, so thresholds
 * move without a release. See `docs/USER_LIFECYCLE_STORE.md`.
 */
interface AppReviewManager {

    /**
     * Records that the user opened a saved trip, and requests a review sheet if that open
     * made them eligible. Safe to call on every open; it does its own gating.
     */
    fun onSavedTripOpened()

    /**
     * Notes that a search just came back empty, which suppresses review requests for a
     * short window. A rating ask that lands seconds after the app failed to find anything
     * is asking at the worst possible moment.
     */
    fun onZeroResultSearch()
}
