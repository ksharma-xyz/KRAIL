package xyz.ksharma.krail.core.analytics

/**
 * User properties attach to **every subsequent event** a rider fires, which is what makes
 * them the cheap way to answer "do large-screen riders behave differently".
 *
 * The alternative was adding a width parameter to every interaction event; that costs a
 * param on dozens of events, and still misses the events nobody thought to update. One
 * property set once per change covers all of them, existing and future.
 *
 * Rules Firebase imposes: name at most 24 characters, value at most 36, and the property
 * must be registered as a custom dimension on the analytics side before it appears in
 * reports. Values here are bare `SCREAMING_CASE` enum names, same contract as event params.
 *
 * These are read by the KRAIL-Analytics registry from this file, alongside
 * [xyz.ksharma.krail.core.analytics.event.AnalyticsEvent].
 */
enum class AnalyticsUserProperty(val propertyName: String) {

    /**
     * Hardware shape as the app sees it: `PHONE`, `TABLET`, `FOLDABLE_OPEN`.
     *
     * A folded foldable is indistinguishable from a phone at the window level - no hinge
     * is reported inside the window - so it lands in `PHONE`. That is not a defect to work
     * around: crossed with the `deviceModel` Firebase already collects, "model says Fold,
     * form factor says PHONE" is precisely the "used closed" segment, which is otherwise
     * unanswerable.
     */
    DEVICE_FORM_FACTOR("device_form_factor"),

    /**
     * Width size class of the window right now, on the app's own breakpoint axis
     * (`COMPACT`, `MEDIUM`, `EXPANDED`, `LARGE`, `EXTRA_LARGE`).
     *
     * Updated on settled changes, so events fired after an unfold or a rotation carry the
     * new value. In BigQuery this makes "what did riders do while the window was EXPANDED"
     * a filter on existing events rather than a new event.
     */
    WINDOW_WIDTH_CLASS("window_width_class"),

    /**
     * Whether the app is actually rendering two panes (`SINGLE` / `DUAL`).
     *
     * Distinct from width class on purpose: the width class is the input, this is what the
     * layout did with it. Without this, a wide window is assumed to mean two panes, which
     * is exactly the assumption the adaptive work needs evidence for rather than faith in.
     */
    PANE_MODE("pane_mode"),
}

/** Sets [property] to [value], both as bare strings, for all subsequent events. */
fun Analytics.setUserProperty(property: AnalyticsUserProperty, value: String) {
    setUserProperty(name = property.propertyName, value = value)
}
