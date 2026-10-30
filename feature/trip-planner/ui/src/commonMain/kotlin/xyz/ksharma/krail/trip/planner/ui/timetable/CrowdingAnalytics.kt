package xyz.ksharma.krail.trip.planner.ui.timetable

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.JourneyCardToggleEvent.Companion.CROWDING_CARS
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.JourneyCardToggleEvent.Companion.CROWDING_LEVEL
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.JourneyCardToggleEvent.Companion.CROWDING_NONE
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.trip.planner.ui.components.crowding.NEXT_SERVICE_TILE_KEY
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo

/** What seat data the card's first service had, for `journey_card_toggle.crowding`. */
internal fun JourneyCardInfo?.crowdingParam(): String = when {
    this?.seatCars != null -> CROWDING_CARS
    this?.seatLevel != null -> CROWDING_LEVEL
    else -> CROWDING_NONE
}

/**
 * The level the card's tag is built from: the whole-service level, or the level most cars share
 * when only carriages are known. Null when there is neither.
 */
internal fun JourneyCardInfo.analyticsSeatLevel(): String? =
    (seatLevel ?: seatCars?.typicalLevel())?.let { level ->
        when (level) {
            SeatLevel.SEATS_AVAILABLE -> "seats"
            SeatLevel.FEW_SEATS -> "few"
            SeatLevel.STANDING_ONLY -> "standing"
        }
    }

/** The "Want a seat?" tip opened. Reuses `info_tile_interaction` rather than minting an event. */
internal fun Analytics.trackSeatTipOpened() {
    track(AnalyticsEvent.InfoTileInteraction(key = NEXT_SERVICE_TILE_KEY, expand = true))
}
