package xyz.ksharma.krail.feature.pro.state

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ProFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    @SerialName("accentHex") val accentHex: String,
    val enabled: Boolean = true,
    val modeLabel: String = "",
    val detail1Label: String = "",
    val detail1Value: String = "",
    val detail2Label: String = "",
    val detail2Value: String = "",
) {
    companion object {
        fun defaults(): List<ProFeature> = listOf(
            ProFeature(
                id = "live_parking",
                title = "Live Parking",
                subtitle = "Park before the driveway.",
                accentHex = "#009B77",
                modeLabel = "Metro",
                detail1Label = "Covers",
                detail1Value = "20+ Sydney stations",
                detail2Label = "Refresh",
                detail2Value = "Live, every minute",
            ),
            ProFeature(
                id = "maps_route",
                title = "Maps & Route",
                subtitle = "The right exit. Every time.",
                accentHex = "#00B5EF",
                modeLabel = "Bus",
                detail1Label = "Shows",
                detail1Value = "Street-level exits",
                detail2Label = "Works on",
                detail2Value = "Train, Metro, Ferry",
            ),
            ProFeature(
                id = "track_trip",
                title = "Track Trip",
                subtitle = "Watch your bus in the wild.",
                accentHex = "#F6891F",
                modeLabel = "Train",
                detail1Label = "Updates",
                detail1Value = "Live vehicle position",
                detail2Label = "Alerts",
                detail2Value = "Delay, platform change",
            ),
            ProFeature(
                id = "departure_board",
                title = "Departure Board",
                subtitle = "Same minute as the platform.",
                accentHex = "#5AB031",
                modeLabel = "Ferry",
                detail1Label = "Shows",
                detail1Value = "All modes at any stop",
                detail2Label = "Refresh",
                detail2Value = "Real-time",
            ),
            ProFeature(
                id = "zero_ads",
                title = "Zero Ads",
                subtitle = "Gone. Forever. Yours.",
                accentHex = "#E4022D",
                modeLabel = "Light Rail",
                detail1Label = "Ads removed",
                detail1Value = "Everywhere, always",
                detail2Label = "No tracking",
                detail2Value = "Ad-free experience",
            ),
            ProFeature(
                id = "all_themes",
                title = "All Themes",
                subtitle = "Train, Metro, Bus, Ferry, Purple, Barbie.",
                accentHex = "#742282",
                modeLabel = "Coach",
                detail1Label = "Palettes",
                detail1Value = "8 transport themes",
                detail2Label = "Custom icons",
                detail2Value = "Included",
            ),
            ProFeature(
                id = "custom_labels",
                title = "Custom Labels",
                subtitle = "Wynyard becomes Work.",
                accentHex = "#F6891F",
                modeLabel = "Train",
                detail1Label = "Rename",
                detail1Value = "Any stop or station",
                detail2Label = "Syncs",
                detail2Value = "Across all trips",
            ),
            ProFeature(
                id = "early_access",
                title = "Early Access",
                subtitle = "Tomorrow's features. Today.",
                accentHex = "#009B77",
                modeLabel = "Metro",
                detail1Label = "Gets",
                detail1Value = "Beta features first",
                detail2Label = "You built",
                detail2Value = "This community",
            ),
        )
    }
}
