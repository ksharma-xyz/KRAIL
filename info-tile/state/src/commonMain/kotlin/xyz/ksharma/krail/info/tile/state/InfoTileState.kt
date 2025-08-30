package xyz.ksharma.krail.info.tile.state

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class InfoTileData(
    /**
     * Unique key to identify the info tile, will be used to track when user dismisses the tile.
     */
    val key: String,

    val title: String,

    val description: String,

    /**
     * Type of Tile which determines its priority in the list of Tiles.
     */
    val type: InfoTileType,

    val dismissCtaText: String = "Dismiss",

    /**
     * ISO-8601 formatted date string representing the end date of the info tile.
     * E.g., "2023-12-31" for December 31, 2023.
     */
    val endDate: String? = null,

    val primaryCta: InfoTileCta? = null,
) {
    enum class InfoTileType(val priority: Int) {
        INFO(priority = 1),
        APP_UPDATE(priority = 2), //  higher priority than info, but lower than alert
        CRITICAL_ALERT(9999), // highest priority, should be shown at top of list
    }
}

@Stable
@Serializable
data class InfoTileCta(
    val text: String,
    val url: String,
)

enum class InfoTileState {
    COLLAPSED,
    EXPANDED
}
