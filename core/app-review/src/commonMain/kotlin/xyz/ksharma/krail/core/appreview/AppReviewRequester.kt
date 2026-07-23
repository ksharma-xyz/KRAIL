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
     * @param source the [DelightMoment] that prompted this request, used only for logging and
     *   the debug proof surface. The platform sheet itself carries no KRAIL-authored context.
     */
    fun requestReview(source: String)
}
