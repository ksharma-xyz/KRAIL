package xyz.ksharma.krail.feature.track.network.crowding

import com.google.transit.realtime.CarriageDescriptor
import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedHeader
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.Position
import com.google.transit.realtime.TripDescriptor
import com.google.transit.realtime.VehiclePosition
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeMatcher.findLiveVehiclePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveConsistTest {

    @Test
    fun `a captured Sydney Trains vehicle carries its live cars`() {
        val feed = FeedMessage.ADAPTER.decode(VEHICLE_HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
        val position = feed.findLiveVehiclePosition(TRIP_ID, "nsw:020T1:W:H:sj2", "23:00", "20261026")
        assertEquals(List(8) { SeatLevel.SEATS_AVAILABLE }, position?.carriages?.levels)
    }

    @Test
    fun `zero based Metro positions read the same as one based`() {
        val position = feedWith(
            listOf(car(0, 3), car(1, 1)),
        ).findLiveVehiclePosition(METRO_TRIP, "nsw:M1", "08:00", "20261026")
        assertEquals(listOf(SeatLevel.STANDING_ONLY, SeatLevel.SEATS_AVAILABLE), position?.carriages?.levels)
    }

    @Test
    fun `a consist without levels is no carriages`() {
        val position = feedWith(
            listOf(CarriageDescriptor(position_in_consist = 1), CarriageDescriptor(position_in_consist = 2)),
        ).findLiveVehiclePosition(METRO_TRIP, "nsw:M1", "08:00", "20261026")
        assertNull(position?.carriages)
    }

    private fun car(position: Int, level: Int) = CarriageDescriptor(
        position_in_consist = position,
        occupancy_status = VehiclePosition.OccupancyStatus.fromValue(level),
    )

    private fun feedWith(consist: List<CarriageDescriptor>) = FeedMessage(
        header_ = FeedHeader(gtfs_realtime_version = "2.0"),
        entity = listOf(
            FeedEntity(
                id = "metro",
                vehicle = VehiclePosition(
                    trip = TripDescriptor(trip_id = METRO_TRIP),
                    position = Position(latitude = -33.8f, longitude = 151.0f),
                    consist = consist,
                ),
            ),
        ),
    )

    private companion object {
        const val METRO_TRIP = "0273-001-049-003:1000"
        const val TRIP_ID = "195U.1324.165.12.A.8.92000160"

        // One vehicle from /v2/gtfs/vehiclepos/sydneytrains captured 2026-10-26 at 10:59pm,
        // raw bytes, eight cars all MANY_SEATS_AVAILABLE.
        const val VEHICLE_HEX =
            "0a0d0a03322e301000188081fdd60612f1010a02383822ea010a290a1d313935552e313332342e3136352e31" +
                "322e412e382e393230303031363020002a065753545f3161120a0dcb8807c215bf34174328fdfcfcd6063000" +
                "3a135379646e65792e5379646e65792038204c6f63425a0a27343139332e323035352e373930372e34313837" +
                "2e353735312e323039362e363938392e39303731122932333a30332043656e7472616c2053746174696f6e20" +
                "746f2050656e726974682053746174696f6e20fa3e031a01414801fa3e0410011801fa3e0410021801fa3e04" +
                "10031801fa3e0410041801fa3e0410051801fa3e0410061801fa3e0410071801fa3e0410081801"
    }
}
