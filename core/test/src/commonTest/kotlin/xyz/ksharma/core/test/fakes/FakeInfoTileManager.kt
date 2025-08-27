package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.info.tile.network.api.db.markInfoTileAsDismissed
import xyz.ksharma.krail.info.tile.state.InfoTileData

class FakeInfoTileManager : InfoTileManager {

    private val tiles: MutableList<InfoTileData> = mutableListOf()

    private val dismissedKeys = mutableSetOf<String>()

    override suspend fun getInfoTiles(): List<InfoTileData> {
        return tiles.filter { it.key !in dismissedKeys }
            .take(InfoTileManager.MAX_INFO_TILE_COUNT)
    }

    override fun isInfoTileActive(key: String): Boolean {
        return key !in dismissedKeys
    }

    override fun markInfoTileDismissed(infoTileData: InfoTileData) {
        dismissedKeys.add(infoTileData.key)
    }

    fun setTiles(newTiles: List<InfoTileData>) {
        tiles.clear()
        tiles.addAll(newTiles)
    }
}
