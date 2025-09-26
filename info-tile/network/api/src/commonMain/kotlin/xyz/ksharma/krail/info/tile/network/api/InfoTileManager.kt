package xyz.ksharma.krail.info.tile.network.api

import xyz.ksharma.krail.info.tile.state.InfoTileData

interface InfoTileManager {

    /**
     * Retrieves a list of active info tiles to be displayed to the user.
     * This list is filtered to exclude any tiles that have been dismissed by the user.
     *
     * @return A list of [InfoTileData] representing the active info tiles.
     */
    suspend fun getInfoTiles(): List<InfoTileData>

    /**
     * Checks if the info tile with the given key is currently active (not dismissed).
     *
     * @param key The unique key of the info tile.
     * @return True if the info tile is active, false if it has been dismissed.
     */
    fun isInfoTileActive(key: String): Boolean

    /**
     * Marks the given info tile as dismissed, preventing it from being shown again.
     *
     * @param infoTileData The info tile data to be marked as dismissed.
     */
    fun markInfoTileDismissed(infoTileData: InfoTileData)

    companion object {
        const val MAX_INFO_TILE_COUNT = 2
    }
}
