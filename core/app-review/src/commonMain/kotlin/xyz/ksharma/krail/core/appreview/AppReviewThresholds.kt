package xyz.ksharma.krail.core.appreview

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean

/**
 * The policy half of the review trigger. Only the master on/off switch is tunable from Remote
 * Config so the feature can be armed without a release; the engagement thresholds are fixed
 * app-side constants, changed by editing them here when needed. The state half lives in the
 * user-lifecycle store.
 *
 * These sit in their own file rather than on [RealAppReviewManager] so the manager stays under
 * its function limit and the tunables are readable in one place.
 */

/**
 * Saved trips the user must have before ask 1 (`>= this`). One, not two: a single saved trip
 * is already a commitment to a routine, and requiring a second left out most regular riders.
 */
internal const val MIN_SAVED_TRIPS = 1L

/**
 * Distinct active days that qualify for ask 1 without a saved trip (`>= this`). The second way
 * in, for habitual riders who plan from search and never save a trip. Five, because below that
 * the riders it adds have thin engagement, and an ask spent on them is one of only two.
 *
 * The day count starts at zero in the release that added it, since nothing on the device
 * records earlier use. So this path cannot produce an ask until about a week or two after a
 * rider updates. That delay is expected, not a bug.
 */
internal const val MIN_ACTIVE_DAYS = 5L

/** Minimum install age in days before ask 1. */
internal const val MIN_ACCOUNT_AGE_DAYS = 3L

/**
 * Minimum days between ask 1 and ask 2. 150 days is about five months, which keeps both asks
 * comfortably inside the platform's silent per-year quota so neither is wasted against it.
 */
internal const val MIN_DAYS_BETWEEN_ASKS = 150L

/**
 * Off unless Remote Config says otherwise, so the trigger stays dormant until it is
 * deliberately switched on.
 */
internal fun Flag.isInAppReviewEnabled(): Boolean =
    getFlagValue(FlagKeys.IN_APP_REVIEW_ENABLED.key).asBoolean(fallback = false)
