package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

/**
 * The one spelling of the Home label, because more than one is how these drift.
 *
 * Home is the only label this app treats as permanent: it cannot be renamed, only reassigned or
 * cleared (see `ManageStopLabelRow`). That is what makes it safe to key behaviour on, and why
 * two places now ask the same question about it: the greeting on the Ask KRAIL surface, and
 * [RiderOriginLocator], which uses Home as the last guess at where a journey starts.
 *
 * Compared case-insensitively at every call site. Riders never type this string, so the
 * constant is about the two readers agreeing rather than about what is stored.
 */
internal const val HOME_STOP_LABEL = "home"
