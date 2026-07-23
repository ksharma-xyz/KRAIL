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

/** Saved trips the user must have before ask 1 (`>= this`). */
internal const val DEFAULT_MIN_SAVED_TRIPS = 2L

/** Minimum install age in days before ask 1. */
internal const val DEFAULT_MIN_ACCOUNT_AGE_DAYS = 3L

/**
 * Minimum days between ask 1 and ask 2. 150 days is about five months, which keeps both asks
 * comfortably inside the platform's silent per-year quota so neither is wasted against it.
 */
internal const val DEFAULT_MIN_DAYS_BETWEEN_ASKS = 150L

/**
 * Off unless Remote Config says otherwise, so the trigger stays dormant until it is
 * deliberately switched on.
 */
internal fun Flag.isInAppReviewEnabled(): Boolean =
    getFlagValue(FlagKeys.IN_APP_REVIEW_ENABLED.key).asBoolean(fallback = false)

internal fun Flag.minSavedTrips(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_MIN_SAVED_TRIPS.key)
        .asNumber(fallback = DEFAULT_MIN_SAVED_TRIPS)

internal fun Flag.minAccountAgeDays(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_MIN_ACCOUNT_AGE_DAYS.key)
        .asNumber(fallback = DEFAULT_MIN_ACCOUNT_AGE_DAYS)

internal fun Flag.minDaysBetweenAsks(): Long =
    getFlagValue(FlagKeys.IN_APP_REVIEW_MIN_DAYS_BETWEEN_ASKS.key)
        .asNumber(fallback = DEFAULT_MIN_DAYS_BETWEEN_ASKS)
