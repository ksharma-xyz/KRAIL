package xyz.ksharma.krail.core.appreview

/**
 * Decides when to ask the platform for a review sheet.
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
 * ## How the two-ask model works
 *
 * A user is asked **at most twice, ever**. Both asks fire on the same shared set of
 * [DelightMoment]s and differ only by their gates: ask 1 needs a tenured, invested user; ask
 * 2 needs a long gap after ask 1. After the second ask, never again. See
 * `docs/investigations/IN_APP_REVIEW_TIMING.md` for the reasoning.
 *
 * The platform never reports whether a sheet was shown or what was rated, so there is no
 * outcome to react to. Every ask is treated identically and we simply stop after two
 * well-timed ones.
 *
 * ## Arm-then-fire
 *
 * A delight moment usually happens on a screen the review sheet should **not** cover (a
 * timetable, the add-park-ride picker). So [onDelightMoment] only *arms* a pending request;
 * [onSavedTripsScreenShown] is what actually evaluates the gates and fires, once the user has
 * landed back on the calm Saved Trips screen. State comes from the user-lifecycle store,
 * policy comes from Remote Config, so thresholds move without a release.
 */
interface AppReviewManager {

    /**
     * Records that a positive, completed action just happened, arming a review request to be
     * evaluated the next time the user lands on the Saved Trips screen. Safe to call on every
     * such moment; it overwrites any moment still armed, since only the source label differs.
     */
    fun onDelightMoment(moment: DelightMoment)

    /**
     * The Saved Trips screen became visible. If a [DelightMoment] is armed and every gate
     * passes, requests the platform review sheet now and consumes the armed moment. A no-op
     * when nothing is armed, so it is safe to call on every appearance of the screen.
     */
    fun onSavedTripsScreenShown()
}

/**
 * The completed, positive actions that can arm a review request. Each carries the analytics
 * `source` label recorded on `review_prompt_requested`, so a new moment extends that event
 * rather than minting a new Firebase event name.
 *
 * All four land the user on a calm screen and none is a "rate" button or a mid-task
 * interruption. That is the whole compliance argument; do not add a moment that fires during
 * an incomplete task.
 */
enum class DelightMoment(val source: String) {

    /** Opened a saved trip, saw its timetable load, and navigated back to Saved Trips. */
    TIMETABLE_VIEWED("timetable_viewed"),

    /** Saved a trip. */
    TRIP_SAVED("trip_saved"),

    /** Added a Park and Ride facility. */
    PARK_RIDE_ADDED("park_ride_added"),

    /** Tapped a Park and Ride card on the Saved Trips screen (armed after a short delay). */
    PARK_RIDE_CARD_TAPPED("park_ride_card_tapped"),
}
