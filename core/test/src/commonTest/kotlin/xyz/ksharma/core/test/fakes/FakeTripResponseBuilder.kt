package xyz.ksharma.core.test.fakes

import kotlin.time.Clock
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse.StopSequence
import kotlin.time.Duration.Companion.minutes

object FakeTripResponseBuilder {
    private var originStopSequence: StopSequence = buildStopSequence()
    private var destinationStopSequence: StopSequence = buildStopSequence()
    private var stopSequence: List<StopSequence> = listOf(buildStopSequence())
    private var transportation: TripResponse.Transportation =
        buildTransportation()
    private var duration: Long = 200

    fun setOriginStopSequence(originStopSequence: StopSequence) =
        apply { this.originStopSequence = originStopSequence }

    fun setDestinationStopSequence(destinationStopSequence: StopSequence) =
        apply { this.destinationStopSequence = destinationStopSequence }

    fun setStopSequence(stopSequence: List<StopSequence>) =
        apply { this.stopSequence = stopSequence }

    fun setTransportation(transportation: TripResponse.Transportation) =
        apply { this.transportation = transportation }

    fun setDuration(duration: Long) = apply { this.duration = duration }

    fun build(): TripResponse.Leg {
        return TripResponse.Leg(
            origin = originStopSequence,
            destination = destinationStopSequence,
            stopSequence = stopSequence,
            transportation = transportation,
            duration = duration
        )
    }

    private fun buildStopSequence(
        arrivalTimePlanned: String = "2024-09-24T19:00:00Z",
        arrivalTimeEstimated: String = "2024-09-24T19:00:00Z",
        departureTimePlanned: String = "2024-09-24T19:10:00Z",
        departureTimeEstimated: String = "2024-09-24T19:10:00Z",
        name: String = "Stop",
        id: String = "stop_id",
    ) = StopSequence(
        arrivalTimePlanned = arrivalTimePlanned,
        arrivalTimeEstimated = arrivalTimeEstimated,
        departureTimePlanned = departureTimePlanned,
        departureTimeEstimated = departureTimeEstimated,
        name = name,
        disassembledName = name,
        id = id,
        type = StopType.STOP.type,
    )

    private fun buildTransportation(transportationId: String = "Transportation Id") = TripResponse.Transportation(
        disassembledName = "Transportation Name",
        product = TripResponse.Product(
            productClass = 1,
            name = "Train",
        ),
        destination = TripResponse.OperatorClass(
            name = "Destination Operator",
            id = "Destination Operator Id",
        ),
        name = "Transportation Name",
        id = transportationId,
        description = "Transportation Description",
    )

    private fun buildJourneyLeg(legIndex: Int, stops: Int, journeyIndex: Int = 0) =
        TripResponse.Leg(
        origin = buildStopSequence(
            name = "Origin Stop $legIndex",
            arrivalTimeEstimated = Clock.System.now().plus(5.minutes * journeyIndex).toString(),
            arrivalTimePlanned = Clock.System.now().plus(5.minutes * journeyIndex).toString(),
            departureTimeEstimated = Clock.System.now().plus(5.minutes * journeyIndex).toString(),
            departureTimePlanned = Clock.System.now().plus(5.minutes * journeyIndex).toString(),
        ),
        destination = buildStopSequence(
            name = "Destination Stop $legIndex",
            arrivalTimeEstimated = Clock.System.now().plus(10.minutes * journeyIndex).toString(),
            arrivalTimePlanned = Clock.System.now().plus(10.minutes * journeyIndex).toString(),
            departureTimeEstimated = Clock.System.now().plus(10.minutes * journeyIndex).toString(),
            departureTimePlanned = Clock.System.now().plus(10.minutes * journeyIndex).toString(),
        ),
        stopSequence = List(stops) { index ->
            buildStopSequence(
                id = "stop_id_${index + 1}",
                name = "Stop ${index + 1}",
                arrivalTimeEstimated = Clock.System.now().plus(5.minutes + journeyIndex.minutes)
                    .toString(),
                arrivalTimePlanned = Clock.System.now().plus(5.minutes + journeyIndex.minutes)
                    .toString(),
                departureTimeEstimated = Clock.System.now().plus(5.minutes + journeyIndex.minutes)
                    .toString(),
                departureTimePlanned = Clock.System.now().plus(5.minutes + journeyIndex.minutes)
                    .toString(),
            )
        },
        transportation = buildTransportation(
            transportationId = "Transportation Id $journeyIndex",
        ),
        duration = 120,
    )

    private fun buildJourneyList(
        numberOfJourney: Int = 1,
        numberOfLegs: Int = 1,
        reverseTimeOrder: Boolean = false,
    ): List<TripResponse.Journey> {
        val journeyList = List(numberOfJourney) { buildJourney(numberOfLegs = numberOfLegs, journeyIndex = it) }
        return if (reverseTimeOrder) journeyList.reversed() else journeyList
    }

    private fun buildJourney(
        numberOfLegs: Int = 1,
        stops: Int = 1,
        journeyIndex: Int = 0,
    ): TripResponse.Journey {
        return TripResponse.Journey(
            legs = List(numberOfLegs) { index ->
                buildJourneyLeg(legIndex = index, stops = stops, journeyIndex = journeyIndex)
            },
        )
    }

    fun buildTripResponse(
        numberOfJourney: Int = 1, numberOfLegs: Int = 1,
        reverseTimeOrder: Boolean = false,
    ): TripResponse {
        return TripResponse(
            journeys = buildJourneyList(
                numberOfJourney = numberOfJourney,
                numberOfLegs = numberOfLegs,
                reverseTimeOrder = reverseTimeOrder,
            ),
        )
    }
}
