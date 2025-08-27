package xyz.ksharma.krail.info.tile.network.api

import xyz.ksharma.krail.info.tile.state.InfoTileData

interface InfoTileManager {

    suspend fun getInfoTiles(): List<InfoTileData>

    companion object {
        const val MAX_INFO_TILE_COUNT = 2
    }
}
