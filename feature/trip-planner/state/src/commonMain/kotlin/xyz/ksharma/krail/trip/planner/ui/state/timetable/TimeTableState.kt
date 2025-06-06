package xyz.ksharma.krail.trip.planner.ui.state.timetable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

data class TimeTableState(
    val isLoading: Boolean = true,
    val silentLoading: Boolean = false, // Loading anim while still displaying TimeTable results.
    val isTripSaved: Boolean = false,
    val journeyList: ImmutableList<JourneyCardInfo> = persistentListOf(),
    val trip: Trip? = null,
    val isError: Boolean = false,
    val unselectedModes: ImmutableSet<Int> = persistentSetOf(),
) {
    data class JourneyCardInfo(
        val timeText: String, // "in x mins"

        val platformText: String? = null, // "on Platform X" or Stand A etc.
        val platformNumber: String? = null, // "1, 2, A etc

        // If first leg is not walking then,
        //      leg.first.origin.departureTimeEstimated ?: leg.first.origin.departureTimePlanned
        // else leg.first.destination.arrivalTimeEstimated ?: leg.first.destination.arrivalTimePlanned
        val originTime: String, // "11:30pm" stopSequence.arrivalTimeEstimated ?: stopSequence.arrivalTimePlanned
        val originUtcDateTime: String, // "2024-09-24T19:00:00Z"

        // legs.last.destination.arrivalTimeEstimated ?: legs.last.destination.arrivalTimePlanned
        val destinationTime: String, // "11:40pm"
        val destinationUtcDateTime: String, // "2024-09-24T19:00:00Z" Use for calculations.

        // legs.sumBy { it.duration } - seconds
        val travelTime: String, // "(10 min)"

        /**
         * Total walking time in the journey.
         */
        val totalWalkTime: String? = null, // "10 mins"

        /**
         * transportation.disassembledName -> lineName
         * transportation.product.class -> TransportModeType
         */
        val transportModeLines: ImmutableList<TransportModeLine>,

        val legs: ImmutableList<Leg>,

        val totalUniqueServiceAlerts: Int,
    ) {
        val journeyId: String
            get() = buildString {
                append(
                    legs.joinToString { leg ->
                        when (leg) {
                            is Leg.WalkingLeg -> ""
                            is Leg.TransportLeg -> leg.tripId ?: "T"
                        }
                    }.filter { it.isLetterOrDigit() },
                )
            }

        /**
         * If the origin time is in the past.
         */
        val hasJourneyStarted: Boolean
            get() = Instant.parse(originUtcDateTime) < Clock.System.now()

        /**
         * If the destination time is in the past.
         */
        val hasJourneyEnded: Boolean
            get() = Instant.parse(destinationUtcDateTime) < Clock.System.now()

        sealed class Leg {
            data class WalkingLeg(
                val duration: String, // "10mins"
            ) : Leg()

            data class TransportLeg(
                // modeType - legs.transportation.product.class
                // lineName - legs.transportation.disassembledName
                val transportModeLine: TransportModeLine,

                // transportation.description -> "Burwood to Liverpool",
                val displayText: String, // "towards X via X"

                // leg.stopSequence.size  (leg.duration seconds)
                val totalDuration: String, // 12 min"

                val stops: ImmutableList<Stop>,

                val walkInterchange: WalkInterchange? = null,

                // Service Alerts for the leg.
                val serviceAlertList: ImmutableList<ServiceAlert>? = null,

                val tripId: String? = null,
            ) : Leg()
        }

        data class WalkInterchange(
            // leg.footPathInfo.duration seconds
            val duration: String,

            // leg.footPathInfo.position
            val position: WalkPosition,
        )

        enum class WalkPosition(val position: String) {
            /**
             * Need to walk before the Leg starts.
             */
            BEFORE("BEFORE"),

            /**
             * After displaying the Leg info, we need to walk.
             */
            AFTER("AFTER"),

            /**
             * This indicates that the walking portion of the leg is the entire leg itself.
             * In other words, the leg involves walking only, with no vehicle transportation involved.
             * For example, if you're planning a trip from one location to another that involves walking
             * the entire distance, the "position" would be "IDEST".
             */
            IDEST("IDEST"),
        }

        data class Stop(
            val name: String, // "xx Station, Platform 1" - stopSequence.disassembledName ?: stopSequence.name
            val time: String, // "12:00pm" - stopSequence.departureTimeEstimated ?: stopSequence.departureTimePlanned
            val isWheelchairAccessible: Boolean,
        )
    }
}
