package xyz.ksharma.krail.trip.planner.ui.components.crowding

import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.feature.track.LiveVehiclePosition
import xyz.ksharma.krail.feature.track.VehicleStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LiveCarriagesTest {

    private val cars = CarriageLevels(List(6) { SeatLevel.STANDING_ONLY })
    private val now = Instant.parse("2026-10-26T07:30:00Z") // 6:30pm in Sydney

    private fun readingAt(secondsAgo: Long) = LiveVehiclePosition(
        latitude = 0.0,
        longitude = 0.0,
        bearing = 0f,
        status = VehicleStatus.IN_TRANSIT_TO,
        lastUpdatedEpochSec = now.epochSeconds - secondsAgo,
        carriages = cars,
    )

    @Test
    fun `a fresh reading is shown`() {
        assertEquals(cars, freshCarriages(readingAt(secondsAgo = 30), now))
    }

    @Test
    fun `a reading older than three polls is dropped`() {
        assertNull(freshCarriages(readingAt(secondsAgo = 600), now))
    }

    @Test
    fun `live sentence says standing without a certainty hedge`() {
        assertEquals("Standing only, all along the train.", liveCarriageSentence(cars, now))
    }
}
