package xyz.ksharma.krail.feature.track

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

const val TRACK_DEEP_LINK_BASE_URL = "https://ksharma-xyz.github.io/trip"

object TripDeepLinkEncoder {

    @Suppress("LongParameterList")
    fun encode(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
        departureUtcDateTime: String,
        legs: List<TimeTableState.JourneyCardInfo.Leg>,
    ): String? {
        val deepLinkLegs = legs
            .filterIsInstance<TimeTableState.JourneyCardInfo.Leg.TransportLeg>()
            .mapNotNull { leg ->
                val stableId = leg.transportationId ?: return@mapNotNull null
                TripDeepLink.DeepLinkLeg(
                    transportationId = stableId,
                    productClass = leg.transportModeLine.transportMode.productClass,
                )
            }

        if (deepLinkLegs.isEmpty()) return null

        val deepLink = TripDeepLink(
            fromStopId = fromStopId,
            toStopId = toStopId,
            fromStopName = fromStopName,
            toStopName = toStopName,
            departureUtcDateTime = departureUtcDateTime,
            legs = deepLinkLegs,
        )

        val json = Json.encodeToString(deepLink)
        val encoded = json.encodeBase64Url()
        return "$TRACK_DEEP_LINK_BASE_URL?d=$encoded"
    }
}

@OptIn(ExperimentalEncodingApi::class)
internal fun String.encodeBase64Url(): String =
    Base64.UrlSafe.encode(this.encodeToByteArray()).trimEnd('=')
