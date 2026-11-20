package xyz.ksharma.krail.trip.planner.network.disruption

import com.google.transit.realtime.Alert
import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedHeader
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.TranslatedString

/**
 * Alerts copied from the live NSW metro alerts feed on 2026-11-20: four planned trackwork closures
 * and two "Station Update" facility notices with no effect. The times are the real epoch seconds.
 */
internal object AlertFixtures {

    /** Buses replace metro between Bankstown and Sydenham, Mon 23 Nov 11:20pm to Tue 2:00am (AEDT). */
    val busesReplaceMetro = alert(
        header = "Buses replace metro services between Bankstown and Sydenham",
        description = "Monday 23 November\nFrom 11:10PM, buses replace metro services between Bankstown and Sydenham.",
        url = "https://transportnsw.info/alerts/details#/ems-8150",
    )

    val metroDoNotRun = alert(
        header = "Metro services do not run between Tallawong and Bankstown",
        start = 1_796_403_600L,
        end = 1_796_421_644L,
    )

    /** UNKNOWN_EFFECT facility notice that is live in the feed with no end date. */
    val stationUpdate = alert(
        header = "Station Update - Central",
        effect = Alert.Effect.UNKNOWN_EFFECT,
        start = 1_783_128_840L,
        end = null,
    )

    private fun text(value: String) = TranslatedString(
        translation = listOf(TranslatedString.Translation(text = value, language = "en")),
    )

    fun alert(
        header: String,
        effect: Alert.Effect? = Alert.Effect.MODIFIED_SERVICE,
        start: Long? = 1_795_436_400L,
        end: Long? = 1_795_446_005L,
        description: String = "",
        url: String? = null,
    ) = Alert(
        header_text = text(header),
        description_text = if (description.isEmpty()) null else text(description),
        effect = effect,
        cause = Alert.Cause.MAINTENANCE,
        url = url?.let { text(it) },
        active_period = if (start == null) emptyList() else listOf(Alert.TimeRange(start = start, end = end)),
    )

    fun feed(vararg alerts: Alert) = FeedMessage(
        header_ = FeedHeader(gtfs_realtime_version = "2.0"),
        entity = alerts.mapIndexed { index, alert -> FeedEntity(id = "$index", alert = alert) },
    )
}
