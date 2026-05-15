package xyz.ksharma.krail.feature.pro.state

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single Pro feature entry, serializable from Remote Config JSON.
 *
 * RC key: [xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys.PRO_FEATURES]
 *
 * Example JSON entry:
 * ```json
 * {
 *   "id": "smart_alerts",
 *   "title": "Smart Alerts",
 *   "subtitle": "Know before you leave the house.",
 *   "accentHex": "#F6891F",
 *   "enabled": true
 * }
 * ```
 *
 * Unknown fields are ignored ([JsonConfig.lenient]) so new fields can be added server-side
 * without breaking older app versions.
 */
@Stable
@Serializable
data class ProFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    @SerialName("accentHex") val accentHex: String,
    val enabled: Boolean = true,
) {
    companion object {
        /** Hardcoded fallback used when RC is unavailable or returns an empty list. */
        fun defaults(): List<ProFeature> = listOf(
            ProFeature(
                id = "smart_alerts",
                title = "Smart Alerts",
                subtitle = "Your 8:15 from Parramatta is running late. Know before you leave the house.",
                accentHex = "#F6891F",
            ),
            ProFeature(
                id = "full_park_ride",
                title = "Full Park & Ride",
                subtitle = "Live counts at all 20+ stations. Push alert when a spot opens at yours.",
                accentHex = "#009B77",
            ),
            ProFeature(
                id = "journey_maps",
                title = "Journey Maps",
                subtitle = "Street-level exit guidance and walking paths. No standing around guessing.",
                accentHex = "#5AB031",
            ),
            ProFeature(
                id = "departure_board",
                title = "Departure Board",
                subtitle = "Live multi-mode board for any stop. Every bus, train and ferry at a glance.",
                accentHex = "#00B5EF",
            ),
            ProFeature(
                id = "safe_ride",
                title = "SafeRide",
                subtitle = "Share your live location with someone. Auto-ends when you arrive safely.",
                accentHex = "#E4022D",
            ),
            ProFeature(
                id = "widgets",
                title = "Home Screen Widgets",
                subtitle = "Your next service on your lock screen. Open nothing. Miss nothing.",
                accentHex = "#742282",
            ),
            ProFeature(
                id = "full_themes",
                title = "Full Themes",
                subtitle = "All 8 colour palettes and custom app icons. Make KRAIL yours.",
                accentHex = "#AC00C9",
            ),
            ProFeature(
                id = "early_access",
                title = "Early Access",
                subtitle = "New features before anyone else. You built this community — first dibs.",
                accentHex = "#E0218A",
            ),
        )
    }
}
