package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

class FareCalculatorTest {
    // region ADULT tests
    @Test
    fun testAdultTrainPeak_0_10km() =
        assertEquals(
            4.20,
            calculateFare(UserType.ADULT, TransportMode.Train(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainOffPeak_0_10km() =
        assertEquals(
            2.94,
            calculateFare(UserType.ADULT, TransportMode.Train(), 8.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainPeak_10_20km() =
        assertEquals(
            5.22,
            calculateFare(UserType.ADULT, TransportMode.Train(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainOffPeak_10_20km() =
        assertEquals(
            3.65,
            calculateFare(UserType.ADULT, TransportMode.Train(), 15.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainPeak_20_35km() =
        assertEquals(
            6.01,
            calculateFare(UserType.ADULT, TransportMode.Train(), 25.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainOffPeak_20_35km() =
        assertEquals(
            4.20,
            calculateFare(UserType.ADULT, TransportMode.Train(), 25.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainPeak_35_65km() =
        assertEquals(
            8.03,
            calculateFare(UserType.ADULT, TransportMode.Train(), 50.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainOffPeak_35_65km() =
        assertEquals(
            5.62,
            calculateFare(UserType.ADULT, TransportMode.Train(), 50.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainPeak_65plus() =
        assertEquals(
            10.33,
            calculateFare(UserType.ADULT, TransportMode.Train(), 70.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultTrainOffPeak_65plus() =
        assertEquals(
            7.23,
            calculateFare(UserType.ADULT, TransportMode.Train(), 70.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusPeak_0_3km() =
        assertEquals(
            3.20,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 2.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusOffPeak_0_3km() =
        assertEquals(
            2.24,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 2.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusPeak_3_8km() =
        assertEquals(
            4.36,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusOffPeak_3_8km() =
        assertEquals(
            3.05,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 5.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusPeak_8plus() =
        assertEquals(
            5.60,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 10.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultBusOffPeak_8plus() =
        assertEquals(
            3.92,
            calculateFare(UserType.ADULT, TransportMode.Bus(), 10.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testAdultFerryShort() =
        assertEquals(
            7.13,
            calculateFare(UserType.ADULT, TransportMode.Ferry(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultFerryLong() =
        assertEquals(
            8.92,
            calculateFare(UserType.ADULT, TransportMode.Ferry(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultMetroPeak() =
        assertEquals(
            4.20,
            calculateFare(UserType.ADULT, TransportMode.Metro(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testAdultWithAirportFee() =
        assertEquals(
            4.20 + 17.34,
            calculateFare(
                UserType.ADULT,
                TransportMode.Train(),
                8.0,
                true,
                false,
                includesAirport = true
            ).totalFare,
            0.01
        )

    // endregion

    // region CHILD tests
    @Test
    fun testChildTrainPeak_0_10km() =
        assertEquals(
            2.10,
            calculateFare(UserType.CHILD, TransportMode.Train(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainOffPeak_0_10km() =
        assertEquals(
            1.47,
            calculateFare(UserType.CHILD, TransportMode.Train(), 8.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainPeak_10_20km() =
        assertEquals(
            2.61,
            calculateFare(UserType.CHILD, TransportMode.Train(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainOffPeak_10_20km() =
        assertEquals(
            1.82,
            calculateFare(UserType.CHILD, TransportMode.Train(), 15.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainPeak_20_35km() =
        assertEquals(
            3.00,
            calculateFare(UserType.CHILD, TransportMode.Train(), 25.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainOffPeak_20_35km() =
        assertEquals(
            2.10,
            calculateFare(UserType.CHILD, TransportMode.Train(), 25.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainPeak_35_65km() =
        assertEquals(
            4.01,
            calculateFare(UserType.CHILD, TransportMode.Train(), 50.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainOffPeak_35_65km() =
        assertEquals(
            2.80,
            calculateFare(UserType.CHILD, TransportMode.Train(), 50.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainPeak_65plus() =
        assertEquals(
            5.16,
            calculateFare(UserType.CHILD, TransportMode.Train(), 70.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildTrainOffPeak_65plus() =
        assertEquals(
            3.61,
            calculateFare(UserType.CHILD, TransportMode.Train(), 70.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusPeak_0_3km() =
        assertEquals(
            1.60,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 2.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusOffPeak_0_3km() =
        assertEquals(
            1.12,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 2.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusPeak_3_8km() =
        assertEquals(
            2.18,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusOffPeak_3_8km() =
        assertEquals(
            1.52,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 5.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusPeak_8plus() =
        assertEquals(
            2.80,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 10.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildBusOffPeak_8plus() =
        assertEquals(
            1.96,
            calculateFare(UserType.CHILD, TransportMode.Bus(), 10.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testChildFerryShort() =
        assertEquals(
            3.56,
            calculateFare(UserType.CHILD, TransportMode.Ferry(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildFerryLong() =
        assertEquals(
            4.46,
            calculateFare(UserType.CHILD, TransportMode.Ferry(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildMetroPeak() =
        assertEquals(
            2.10,
            calculateFare(UserType.CHILD, TransportMode.Metro(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testChildWithAirportFee() =
        assertEquals(
            2.10 + 15.50,
            calculateFare(
                UserType.CHILD,
                TransportMode.Train(),
                8.0,
                true,
                false,
                includesAirport = true
            ).totalFare,
            0.01
        )
    // endregion

    // region SENIOR tests
    @Test
    fun testSeniorTrainPeak_0_10km() =
        assertEquals(
            2.10,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainOffPeak_0_10km() =
        assertEquals(
            1.47,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 8.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainPeak_10_20km() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainOffPeak_10_20km() =
        assertEquals(
            1.82,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 15.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainPeak_20_35km() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 25.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainOffPeak_20_35km() =
        assertEquals(
            2.10,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 25.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainPeak_35_65km() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 50.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainOffPeak_35_65km() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 50.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainPeak_65plus() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 70.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorTrainOffPeak_65plus() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Train(), 70.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusPeak_0_3km() =
        assertEquals(
            1.60,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 2.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusOffPeak_0_3km() =
        assertEquals(
            1.12,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 2.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusPeak_3_8km() =
        assertEquals(
            2.18,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusOffPeak_3_8km() =
        assertEquals(
            1.52,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 5.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusPeak_8plus() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 10.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorBusOffPeak_8plus() =
        assertEquals(
            1.96,
            calculateFare(UserType.SENIOR, TransportMode.Bus(), 10.0, false, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorFerryShort() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Ferry(), 5.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorFerryLong() =
        assertEquals(
            2.50,
            calculateFare(UserType.SENIOR, TransportMode.Ferry(), 15.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorMetroPeak() =
        assertEquals(
            2.10,
            calculateFare(UserType.SENIOR, TransportMode.Metro(), 8.0, true, false).totalFare,
            0.01
        )

    @Test
    fun testSeniorWithAirportFee() =
        assertEquals(
            2.10 + 15.50,
            calculateFare(
                UserType.SENIOR,
                TransportMode.Train(),
                8.0,
                true,
                false,
                includesAirport = true
            ).totalFare,
            0.01
        )

    // endregion
}