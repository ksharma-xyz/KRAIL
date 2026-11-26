package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent.EndReason
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapViewedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideActionEvent
import kotlin.random.Random
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Where a map is shown, which `map_viewed` reports as `entry`. The Intro has no map. */
enum class MapEntry(internal val event: MapViewedEvent.Entry) {
    /** The home screen's map: the right pane of Saved Trips on a wide window. */
    HOME(MapViewedEvent.Entry.HOME),

    /** Search Stop's map state, and its wide window map pane. */
    SEARCH_STOP(MapViewedEvent.Entry.SEARCH_STOP),
}

/**
 * Counts map opens for analytics: one `map_viewed` and, later, exactly one `map_dismissed` for each, joined by a
 * `mapSessionId` that is also stamped on the map's two existing events.
 *
 * An open is one stretch of the map being on screen. [MapOpenEffect] decides when it starts and ends, from the
 * lifecycle and from what covers the map. This class holds only what the open needs and sends nothing twice:
 *
 * - [viewed] starts an open and sends `map_viewed`. It does nothing while one is already open, so the screen being
 *   built again after a rotation continues the open it was in and does not start another.
 * - [dismissed] ends the open and sends `map_dismissed`. It does nothing with no open, so whichever of back, a stop,
 *   the lifecycle or the cover comes first is the one that sends, and a later one cannot send a second.
 * - [stopPicked] marks the open as having a pick, then ends it.
 *
 * The duration is a monotonic clock difference taken as the open ends: no timer runs while the map is open. The id
 * is minted here and held here, so it lives as long as the ViewModel that owns this (a rotation keeps it). It is
 * not saved: an open never outlives its process, because the app leaving the foreground ends it before the system
 * can kill the process, and a restored process shows the map as a new open.
 *
 * Everything runs on the main thread.
 */
class MapOpenTracker(
    private val analytics: Analytics,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val newSessionId: () -> String = ::newMapSessionId,
) {
    private var open: Open? = null

    /** The id of the open in progress, for the events that belong to it, or null with none. */
    val sessionId: String? get() = open?.id

    fun viewed(
        entry: MapEntry,
        hadUserLocation: Boolean,
        locationPermission: IntroLetsKrailClickEvent.LocationResult,
        layout: MapViewedEvent.Layout,
        stopCount: Int,
    ) {
        if (open != null) return
        val id = newSessionId()
        open = Open(id = id, startedAt = timeSource.markNow(), hadStops = stopCount > 0)
        analytics.track(
            MapViewedEvent(
                entry = entry.event,
                hadUserLocation = hadUserLocation,
                locationPermission = locationPermission,
                layout = layout,
                mapSessionId = id,
            ),
        )
    }

    /** The rider opened a station's parking card from the map. */
    fun parkRideCardOpened(layout: MapViewedEvent.Layout, stopId: String) {
        analytics.track(
            ParkRideActionEvent(
                action = ParkRideActionEvent.Action.MAP_CARD_OPENED,
                source = layout.parkRideSource(),
                stopId = stopId,
                mapSessionId = sessionId,
            ),
        )
    }

    /** The rider tapped directions to one car park on the map's parking card. */
    fun parkRideDirections(layout: MapViewedEvent.Layout, stopId: String, facilityId: String) {
        analytics.track(
            ParkRideActionEvent(
                action = ParkRideActionEvent.Action.DIRECTIONS,
                source = layout.parkRideSource(),
                stopId = stopId,
                facilityId = facilityId,
                mapSessionId = sessionId,
            ),
        )
    }

    /** The number of stop markers now on the map. One or more makes the open `hadStops`, and it stays so. */
    fun stopsOnMap(count: Int) {
        if (count > 0) open?.hadStops = true
    }

    fun stopPicked() = dismissed(EndReason.PICKED)

    fun dismissed(reason: EndReason = EndReason.LEFT) {
        val ending = open ?: return
        open = null
        val openMs = ending.startedAt.elapsedNow().inWholeMilliseconds
        analytics.track(
            MapDismissedEvent(
                mapSessionId = ending.id,
                endReason = reason,
                durationBucket = MapDismissedEvent.DurationBucket.from(openMs),
                hadStops = ending.hadStops,
            ),
        )
    }

    private class Open(
        val id: String,
        val startedAt: TimeMark,
        var hadStops: Boolean,
    )
}

private fun MapViewedEvent.Layout.parkRideSource(): ParkRideActionEvent.Source = when (this) {
    MapViewedEvent.Layout.FULL -> ParkRideActionEvent.Source.STOP_MAP
    MapViewedEvent.Layout.SIDE -> ParkRideActionEvent.Source.SIDE_MAP
}

private const val SESSION_ID_LENGTH = 16
private const val HEX_RADIX = 16

/**
 * A map session id: 16 lowercase hex characters from a random 64 bit value, never derived from a user id or a time.
 * It is minted as `searchSessionId` is (`Random.nextLong().toULong().toString(radix = 16)`), and padded to 16
 * because a value with a leading zero nibble is otherwise shorter, and the registry reads the id as a fixed width.
 */
fun newMapSessionId(random: Random = Random): String =
    random.nextLong().toULong().toString(radix = HEX_RADIX).padStart(SESSION_ID_LENGTH, '0')
