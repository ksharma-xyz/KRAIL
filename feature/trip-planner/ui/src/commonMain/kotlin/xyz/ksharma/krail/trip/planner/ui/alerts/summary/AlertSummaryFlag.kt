package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean

/**
 * Off unless Remote Config says otherwise, so the whole AI alert summary surface stays
 * dormant until deliberately switched on — and can be pulled instantly without a release.
 */
internal fun Flag.isAlertSummaryEnabled(): Boolean =
    getFlagValue(FlagKeys.ALERT_SUMMARY_ENABLED.key).asBoolean(fallback = false)
