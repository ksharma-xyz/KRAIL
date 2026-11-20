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

    fun trackShown(banner: DisruptionBannerState.Shown, trip: Trip, visibleJourneyCount: Int) {
        if (!shownKeys.add("${banner.disruption.id}|${trip.tripId}")) return
        analytics.track(
            DisruptionStatusEvent(
                action = DisruptionStatusEvent.Action.SHOWN,
                transportMode = banner.disruption.feed.feedName,
                fromStopId = trip.fromStopId,
                toStopId = trip.toStopId,
                reason = when (banner.reason) {
                    DisruptionReason.MODE_MISSING -> DisruptionStatusEvent.REASON_MODE_MISSING
                    DisruptionReason.REPLACEMENT_BUS -> DisruptionStatusEvent.REASON_REPLACEMENT_BUS
                },
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

    fun trackTile(banner: DisruptionBannerState.Shown, expand: Boolean? = null, ctaUrl: String? = null) {
        analytics.track(
            AnalyticsEvent.InfoTileInteraction(
                key = "disruption_${banner.disruption.feed.feedName}",
                expand = expand,
                ctaUrl = ctaUrl,
            ),
        )
    }
}
