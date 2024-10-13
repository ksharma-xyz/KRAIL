package xyz.ksharma.krail.trip_planner.ui.timetable.business

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import xyz.ksharma.krail.core.date_time.DateTimeHelper.aestToHHMM
import xyz.ksharma.krail.core.date_time.DateTimeHelper.calculateTimeDifference
import xyz.ksharma.krail.core.date_time.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.date_time.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.core.date_time.DateTimeHelper.toFormattedString
import xyz.ksharma.krail.core.date_time.DateTimeHelper.utcToAEST
import xyz.ksharma.krail.trip_planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip_planner.ui.state.TransportMode
import xyz.ksharma.krail.trip_planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip_planner.ui.state.timetable.TimeTableState

internal fun TripResponse.buildJourneyList() = journeys?.map { journey ->

    // TODO -
    //  1. Sanitise data in domain layer.
    //  2. Pass non null items only to display in ViewModel.

    val firstLeg = journey.legs?.firstOrNull()
    val lastLeg = journey.legs?.lastOrNull()

    val firstPublicTransportLeg = journey.legs?.firstOrNull { leg ->
        leg.transportation?.product?.productClass != 99L &&
                leg.transportation?.product?.productClass != 100L
    }
    val lastPublicTransportLeg = journey.legs?.lastOrNull { leg ->
        leg.transportation?.product?.productClass != 99L &&
                leg.transportation?.product?.productClass != 100L
    }

    val originTime = firstPublicTransportLeg?.origin?.departureTimeEstimated
        ?: firstPublicTransportLeg?.origin?.departureTimePlanned
    val arrivalTime = lastPublicTransportLeg?.destination?.arrivalTimeEstimated
        ?: lastPublicTransportLeg?.destination?.arrivalTimePlanned

    TimeTableState.JourneyCardInfo(
        timeText = originTime?.let {
            Timber.d("originTime: $it :- ${calculateTimeDifferenceFromNow(utcDateString = it)}")
            calculateTimeDifferenceFromNow(utcDateString = it).toFormattedString()
        } ?: "NULL,",

        platformText = when (firstPublicTransportLeg?.transportation?.product?.productClass) {
            // Train or Metro
            1L, 2L -> {
                firstPublicTransportLeg.stopSequence?.firstOrNull()?.disassembledName?.split(",")
                    ?.lastOrNull()
            }

            // Other Modes
            9L, 5L, 4L, 7L -> {
                firstPublicTransportLeg.stopSequence?.firstOrNull()?.disassembledName
            }

            else -> null
        }?.trim(),

        originTime = originTime?.utcToAEST()?.aestToHHMM() ?: "NULL",
        originUtcDateTime = originTime ?: "NULL",

        destinationTime = arrivalTime?.utcToAEST()?.aestToHHMM() ?: "NULL",

        travelTime = calculateTimeDifference(
            originTime!!,
            arrivalTime!!
        ).toMinutes().toString() + " mins",
        transportModeLines = journey.legs?.mapNotNull { leg ->
            leg.transportation?.product?.productClass?.toInt()?.let {
                TransportMode.toTransportModeType(productClass = it)
                    ?.let { it1 ->
                        TransportModeLine(
                            transportMode = it1,
                            lineName = leg.transportation?.disassembledName
                                ?: "NULL"
                        )
                    }
            }
        }?.toImmutableList() ?: persistentListOf(),
    )
}?.toImmutableList()

internal fun TripResponse.logForUnderstandingData() {
    Timber.d("Journeys: ${journeys?.size}")
    journeys?.mapIndexed { jindex, j ->
        Timber.d("JOURNEY #${jindex + 1}")
        j.legs?.forEachIndexed { index, leg ->

            val transportationProductClass =
                leg.transportation?.product?.productClass

            Timber.d(
                " LEG#${index + 1} -- Duration: ${leg.duration} -- productClass:${transportationProductClass?.toInt()}"
            )
            Timber.d(
                "\t\t ORG: ${
                    leg.origin?.departureTimeEstimated?.utcToAEST()
                        ?.formatTo12HourTime()
                }," +
                        " DEST: ${
                            leg.destination?.arrivalTimeEstimated?.utcToAEST()
                                ?.formatTo12HourTime()
                        }, " +
                        //     "Duration: ${leg.duration}, " +
                        // "transportation: ${leg.transportation?.name}",
                        "interchange: ${leg.interchange?.run { "[desc:$desc, type:$type] " }}" +
                        // "leg properties: ${leg.properties}" +
                        // "leg origin properties: ${leg.origin?.properties}"
                        "\n\t\t\t leg stopSequence: ${leg.stopSequence?.interchangeStopsList()}"
            )
        }
    }
}

/**
 * Prints the stops for legs when interchange required.
 */
private fun List<TripResponse.StopSequence>.interchangeStopsList() = this.mapNotNull {
    // TODO - figure role of ARR vs DEP time
    val timeArr = it.arrivalTimeEstimated?.utcToAEST()
        ?.formatTo12HourTime() ?: it.arrivalTimePlanned?.utcToAEST()?.formatTo12HourTime()

    val depTime = it.departureTimeEstimated?.utcToAEST()
        ?.formatTo12HourTime() ?: it.departureTimePlanned?.utcToAEST()?.formatTo12HourTime()

    if (timeArr == null && depTime == null) null else "\n\t\t\t\t Stop: ${it.name}, depTime: ${timeArr ?: depTime}"
}
