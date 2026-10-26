package xyz.ksharma.krail.trip.planner.ui.components.crowding

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.track.LiveVehiclePosition
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The live cars for a vehicle, or null when its reading is stale. Some train positions in the
 * feed are hours old (TFNSW_OCCUPANCY_DATA.md, "Reliability"), so anything older than about
 * three of the screen's 30-second polls is dropped rather than shown as live.
 */
@OptIn(ExperimentalTime::class)
internal fun freshCarriages(position: LiveVehiclePosition?, now: Instant): CarriageLevels? {
    val cars = position?.carriages ?: return null
    val readAt = Instant.fromEpochSeconds(position.lastUpdatedEpochSec)
    return cars.takeIf { now - readAt <= MAX_READING_AGE }
}

/** A live reading: "has", never "expected". Night swaps in the Nightsafe line. */
@OptIn(ExperimentalTime::class)
internal fun liveCarriageSentence(cars: CarriageLevels?, now: Instant): String? {
    cars ?: return null
    val hour = now.toLocalDateTime(SYDNEY).hour
    return CrowdingWording.forCarriages(cars, Certainty.LIVE, CrowdingWording.isNight(hour))?.sentence
}

private val MAX_READING_AGE = 90.seconds
private val SYDNEY = TimeZone.of("Australia/Sydney")
