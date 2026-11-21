package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DisruptionStatusEvent
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Reports the disruption banner to analytics, and keeps the de-duplication that makes the numbers mean
 * what they say. Held by the timetable ViewModel, so the memory is per screen.
 *
 * - `shown` once per alert and trip: the banner is a list item that composes again every time it
 *   scrolls back into view, and a refresh every 30 seconds rebuilds state.
 * - `error` once per screen: a flaky feed would otherwise report on every 30 second refresh.
 * - Expanding and "More info" ride the existing `info_tile_interaction` event, with a fixed key per
 *   mode, because the banner is an info tile. No alert text is ever sent.
 */
internal class DisruptionAnalytics(private val analytics: Analytics) {
    private val shownKeys = mutableSetOf<String>()
    private var errorTracked = false

    fun trackShown(banner: DisruptionBannerState, trip: Trip, visibleJourneyCount: Int) {
        val reported = banner.reported() ?: return
        if (!shownKeys.add("${reported.key}|${trip.tripId}")) return
        analytics.track(
            DisruptionStatusEvent(
                action = DisruptionStatusEvent.Action.SHOWN,
                transportMode = reported.source,
                fromStopId = trip.fromStopId,
                toStopId = trip.toStopId,
                reason = reported.reason,
                visibleJourneyCount = visibleJourneyCount,
            ),
        )
    }

    fun trackUnavailable(feed: DisruptionFeed, trip: Trip) {
        if (errorTracked) return
        errorTracked = true
        analytics.track(
            DisruptionStatusEvent(
                action = DisruptionStatusEvent.Action.ERROR,
                transportMode = feed.feedName,
                fromStopId = trip.fromStopId,
                toStopId = trip.toStopId,
            ),
        )
    }

    fun trackTile(banner: DisruptionBannerState, expand: Boolean? = null, ctaUrl: String? = null) {
        val reported = banner.reported() ?: return
        analytics.track(
            AnalyticsEvent.InfoTileInteraction(
                key = "disruption_${reported.source}",
                expand = expand,
                ctaUrl = ctaUrl,
            ),
        )
    }
}

/**
 * What a banner reports. A replacement-bus banner with no alert behind it has no feed, so its source
 * is the trip planner, which is where the evidence came from.
 */
private class Reported(val key: String, val source: String, val reason: String)

private fun DisruptionBannerState.reported(): Reported? = when (this) {
    DisruptionBannerState.None -> null
    is DisruptionBannerState.Shown -> Reported(
        key = disruption.id,
        source = disruption.feed.feedName,
        reason = when (reason) {
            DisruptionReason.MODE_MISSING -> DisruptionStatusEvent.REASON_MODE_MISSING
            DisruptionReason.REPLACEMENT_BUS -> DisruptionStatusEvent.REASON_REPLACEMENT_BUS
        },
    )
    is DisruptionBannerState.ReplacementBuses -> Reported(
        key = disruption?.id ?: REPLACEMENT_LEG_KEY,
        source = disruption?.feed?.feedName ?: PLANNER_SOURCE,
        reason = DisruptionStatusEvent.REASON_REPLACEMENT_BUS,
    )
}

private const val REPLACEMENT_LEG_KEY = "replacement_leg"
private const val PLANNER_SOURCE = "planner"
