package xyz.ksharma.krail.core.appreview

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.core.remoteconfig.flag.asNumber

/**
 * The policy half of the review trigger: every tunable it reads comes from Remote Config, so
 * thresholds move without a release. The state half lives in the user-lifecycle store.
 *
 * These sit in their own file rather than on [RealAppReviewManager] so the manager stays under
 * its function limit and the tunables are readable in one place.
 */

/** Saved-trip opens required before a review sheet is requested. */
internal const val DEFAULT_MIN_SAVED_TRIP_OPENS = 3L

/** Minimum install age in days before a review sheet is requested. */
internal const val DEFAULT_MIN_ACCOUNT_AGE_DAYS = 3L

/** Days between review requests. */
internal const val DEFAULT_COOLDOWN_DAYS = 60L

/**
 * Off unless Remote Config says otherwise, so the trigger stays dormant until it is
 * deliberately switched on.
 */
internal fun Flag.isInAppReviewEnabled(): Boolean =
    getFlagValue(FlagKeys.IN_APP_REVIEW_ENABLED.key).asBoolean(fallback = false)

internal fun Flag.minSavedTripOpens(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_MIN_SAVED_TRIP_OPENS.key)
        .asNumber(fallback = DEFAULT_MIN_SAVED_TRIP_OPENS)

internal fun Flag.minAccountAgeDays(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_MIN_ACCOUNT_AGE_DAYS.key)
        .asNumber(fallback = DEFAULT_MIN_ACCOUNT_AGE_DAYS)

internal fun Flag.reviewCooldownDays(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_COOLDOWN_DAYS.key)
        .asNumber(fallback = DEFAULT_COOLDOWN_DAYS)
