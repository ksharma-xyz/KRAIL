package xyz.ksharma.krail.feature.track

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripDeepLink(
    @SerialName("f") val fromStopId: String,
    @SerialName("t") val toStopId: String,
    @SerialName("fn") val fromStopName: String,
    @SerialName("tn") val toStopName: String,
    /** ISO 8601 UTC departure datetime, e.g. "2025-04-19T14:30:00Z" */
    @SerialName("dep") val departureUtcDateTime: String,
    /** One entry per TransportLeg in the shared journey */
    @SerialName("legs") val legs: List<DeepLinkLeg>,
) {
    @Serializable
    data class DeepLinkLeg(
        /**
         * `transportation.id` from the TfNSW trip API — a stable timetable route identifier
         * (e.g. `"nsw:020T1:W:R:sj2"` for the T1 Western Line). NOT the volatile `RealtimeTripId`.
         *
         * Important: this ID identifies the **route/service**, not a specific departure time.
         * Multiple runs of the same service on the same day share this ID (e.g. the T1 at 8:20am
         * and at 8:26am both carry `"nsw:020T1:W:R:sj2"`). The [TripDeepLink.departureUtcDateTime]
         * is therefore also required to uniquely pinpoint the correct run when matching against a
         * live API response.
         */
        @SerialName("tid") val transportationId: String,
        /** TfNSW product class — 1=Train, 2=Metro, 4=LightRail, 5=Bus, 7=Coach, 9=Ferry */
        @SerialName("cls") val productClass: Int,
    )
}
