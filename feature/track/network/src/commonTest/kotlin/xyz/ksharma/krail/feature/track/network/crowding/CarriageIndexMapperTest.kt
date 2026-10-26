package xyz.ksharma.krail.feature.track.network.crowding

import com.google.transit.realtime.FeedMessage
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CarriageIndexMapperTest {

    private val feed = FeedMessage.ADAPTER.decode(SydneyTrainsTripUpdateFixture.bytes())

    @Test
    fun `Wire decodes the TfNSW occupancy fields from a captured feed`() {
        val stop = feed.entity.single().trip_update!!.stop_time_update.first()
        assertEquals(2, stop.departure_occupancy_status?.value)
        assertEquals(8, stop.carriage_seq_predictive_occupancy.size)
        assertEquals(1, stop.carriage_seq_predictive_occupancy.first().position_in_consist)
    }

    @Test
    fun `index holds the cars per trip and stop in feed order`() {
        val cars = feed.toCarriageIndex().at(SydneyTrainsTripUpdateFixture.TRIP_ID, "2141313")
        assertEquals(listOf(SEATS_AVAILABLE) + List(6) { FEW_SEATS } + SEATS_AVAILABLE, cars?.levels)
    }

    @Test
    fun `captured train reads as seats at both ends`() {
        val cars = feed.toCarriageIndex().at(SydneyTrainsTripUpdateFixture.TRIP_ID, "2141313")!!
        assertEquals(
            "Seats expected at both ends.",
            CrowdingWording.forCarriages(cars, Certainty.FORECAST)?.sentence,
        )
    }

    @Test
    fun `unknown trip or stop is a miss`() {
        val index = feed.toCarriageIndex()
        assertNull(index.at("not-a-trip", "2141313"))
        assertNull(index.at(SydneyTrainsTripUpdateFixture.TRIP_ID, "999"))
        assertNull(index.at(null, "2141313"))
    }
}
