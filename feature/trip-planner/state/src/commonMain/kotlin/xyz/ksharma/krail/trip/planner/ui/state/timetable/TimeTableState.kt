package xyz.ksharma.krail.trip.planner.ui.state.timetable

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Stable
data class TimeTableState(
    val isLoading: Boolean = true,
    val silentLoading: Boolean = false, // Loading anim while still displaying TimeTable results.
    val isTripSaved: Boolean = false,
    val journeyList: ImmutableList<JourneyCardInfo> = persistentListOf(),
    val trip: Trip? = null,
    val isError: Boolean = false,
    val unselectedModes: ImmutableSet<Int> = persistentSetOf(),
    // has to be null, otherwise it will switch from default to a festival.
    // It should load only once whether it is a festival or not.
    val loadingEmoji: LoadingEmoji? = null,
) {
    @OptIn(ExperimentalTime::class)
    @Stable
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

        /**
         * Deviation for the first public transport departure vs planned time.
         * Null when real-time estimate is unavailable.
         */
        val departureDeviation: DepartureDeviation? = null,
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

        /**
         * A sealed type describing the departure deviation status.
         */
        sealed interface DepartureDeviation {
            data class Late(val text: String) : DepartureDeviation
            data class Early(val text: String) : DepartureDeviation
            data object OnTime : DepartureDeviation
        }

        @Stable
        sealed class Leg {
            data class WalkingLeg(
                val duration: String, // "10mins"
            ) : Leg()

            data class TransportLeg(
                // modeType - legs.transportation.product.class
                // lineName - legs.transportation.disassembledName
                val transportModeLine: TransportModeLine,

                // transportation.description -> "Burwood to Liverpool",
                val displayText: String?, // "towards X via X"

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

        @Stable
        data class Stop(
            val name: String, // "xx Station, Platform 1" - stopSequence.disassembledName ?: stopSequence.name
            val time: String, // "12:00pm" - stopSequence.departureTimeEstimated ?: stopSequence.departureTimePlanned
            val isWheelchairAccessible: Boolean,
        )
    }

    @Stable
    data class LoadingEmoji(
        val emoji: String,
        val greeting: String,
    )
}
