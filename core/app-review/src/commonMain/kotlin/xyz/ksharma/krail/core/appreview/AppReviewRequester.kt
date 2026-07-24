package xyz.ksharma.krail.core.appreview

/**
 * Asks the platform to show its own review sheet: Play In-App Review on Android,
 * StoreKit on iOS.
 *
 * Both platforms own the sheet completely, and that shapes the whole contract:
 *
 * - **The OS decides whether anything appears.** Both APIs are quota-throttled, so most
 *   calls show nothing at all. A call is a request, not a display.
 * - **The outcome is not reported.** There is no way to learn whether the sheet appeared,
 *   whether the user rated, or what they rated. Do not build logic that needs to know.
 * - **The copy is system-owned.** There is no KRAIL-authored text in the sheet.
 *
 * Callers must not wrap this in a custom pre-prompt, gate it on a sentiment question, or
 * fire it from a button. See [AppReviewManager] for why.
 */
interface AppReviewRequester {

    /**
     * Requests the platform review sheet. Silent no-op when the platform cannot show one.
     * Never throws: a failed review request must not affect anything the user was doing.
     *
     * Which [DelightMoment] prompted the request is recorded on the `review_prompt_requested`
     * analytics event by [AppReviewManager], not passed here: the platform sheet carries no
     * KRAIL-authored context, so the requester has no use for it.
     */
    fun requestReview()
}
