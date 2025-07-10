package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

enum class UserType { ADULT, CHILD, CONCESSION, SENIOR }
data class FareResult(
    val totalFare: Double
)

fun calculateFare(
    userType: UserType,
    mode: TransportMode,
    distanceKm: Double,
    isPeak: Boolean,
    isFridayOrWeekendOrHoliday: Boolean,
    includesAirport: Boolean = false,
    faresSoFarToday: Double = 0.0,
    faresSoFarThisWeek: Double = 0.0,
): FareResult {
    val fare = when (userType) {
        UserType.ADULT -> when (mode) {
            is TransportMode.Train, is TransportMode.Metro -> when {
                distanceKm <= 10 -> if (isPeak) 4.20 else 2.94
                distanceKm <= 20 -> if (isPeak) 5.22 else 3.65
                distanceKm <= 35 -> if (isPeak) 6.01 else 4.20
                distanceKm <= 65 -> if (isPeak) 8.03 else 5.62
                else -> if (isPeak) 10.33 else 7.23
            }
            is TransportMode.Bus, is TransportMode.LightRail -> when {
                distanceKm <= 3 -> if (isPeak) 3.20 else 2.24
                distanceKm <= 8 -> if (isPeak) 4.36 else 3.05
                else -> if (isPeak) 5.60 else 3.92
            }
            is TransportMode.Ferry -> if (distanceKm <= 9) 7.13 else 8.92
            is TransportMode.Coach -> 0.0 // Not covered by Opal
        }
        UserType.CHILD, UserType.CONCESSION -> when (mode) {
            is TransportMode.Train, is TransportMode.Metro -> when {
                distanceKm <= 10 -> if (isPeak) 2.10 else 1.47
                distanceKm <= 20 -> if (isPeak) 2.61 else 1.82
                distanceKm <= 35 -> if (isPeak) 3.00 else 2.10
                distanceKm <= 65 -> if (isPeak) 4.01 else 2.80
                else -> if (isPeak) 5.16 else 3.61
            }
            is TransportMode.Bus, is TransportMode.LightRail -> when {
                distanceKm <= 3 -> if (isPeak) 1.60 else 1.12
                distanceKm <= 8 -> if (isPeak) 2.18 else 1.52
                else -> if (isPeak) 2.80 else 1.96
            }
            is TransportMode.Ferry -> if (distanceKm <= 9) 3.56 else 4.46
            is TransportMode.Coach -> 0.0
        }
        UserType.SENIOR -> when (mode) {
            is TransportMode.Train, is TransportMode.Metro -> when {
                distanceKm <= 10 -> if (isPeak) 2.10 else 1.47
                distanceKm <= 20 -> if (isPeak) 2.50 else 1.82
                distanceKm <= 35 -> if (isPeak) 2.50 else 2.10
                distanceKm <= 65 -> 2.50
                else -> 2.50
            }
            is TransportMode.Bus, is TransportMode.LightRail -> when {
                distanceKm <= 3 -> if (isPeak) 1.60 else 1.12
                distanceKm <= 8 -> if (isPeak) 2.18 else 1.52
                else -> if (isPeak) 2.50 else 1.96
            }
            is TransportMode.Ferry -> 2.50 // Daily cap applies
            is TransportMode.Coach -> 0.0
        }
    }

    val airportFee = when (userType) {
        UserType.ADULT -> if (includesAirport) 17.34 else 0.0
        UserType.CHILD, UserType.CONCESSION, UserType.SENIOR -> if (includesAirport) 15.50 else 0.0
    }

    val dailyCap = when (userType) {
        UserType.ADULT -> if (isFridayOrWeekendOrHoliday) 9.35 else 18.70
        UserType.CHILD, UserType.CONCESSION -> if (isFridayOrWeekendOrHoliday) 4.65 else 9.35
        UserType.SENIOR -> 2.50
    }
    val weeklyCap = when (userType) {
        UserType.ADULT -> 50.0
        UserType.CHILD, UserType.CONCESSION -> 25.0
        UserType.SENIOR -> 17.50
    }

    val cappedFare = minOf(
        fare,
        dailyCap - faresSoFarToday,
        weeklyCap - faresSoFarThisWeek
    ).coerceAtLeast(0.0)

    val totalFare = cappedFare + airportFee

    return FareResult(totalFare = totalFare)
}
