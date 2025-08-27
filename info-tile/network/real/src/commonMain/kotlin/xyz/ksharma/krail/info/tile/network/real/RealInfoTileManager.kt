package xyz.ksharma.krail.info.tile.network.real

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager.Companion.MAX_INFO_TILE_COUNT
import xyz.ksharma.krail.info.tile.network.api.db.isInfoTileDismissed
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.sandook.SandookPreferences

class RealInfoTileManager(
    private val appVersionManager: AppVersionManager,
    private val appInfoProvider: AppInfoProvider,
    private val platformOps: PlatformOps,
    private val preferences: SandookPreferences,
) : InfoTileManager {

    override suspend fun getInfoTiles(): List<InfoTileData> {
        // todo -
        // add info tiles from different sources.
        // Source -1 : Flag, json key.
        // Source 2: check app version and add if required.

        // process list
        // sort by priority .sortedBy { it.type.priority }
        // - have max of MAX_INFO_TILE_COUNT items only
        // toSet - ensure unique elements only.

        // then return list.
        return emptyList()
    }

    private suspend fun checkAppVersion() {
        log("onStart - checkAppVersion called")
        val appUpdateCopy = appVersionManager.getUpdateCopy()
        if (appUpdateCopy == null) {
            log("App update copy is null, no update available.")
            return
        }

        val updateTile = createAppUpdateTile(appUpdateCopy)
    }

    private fun createAppUpdateTile(appUpdateCopy: AppVersionManager.AppVersionUpdateCopy): InfoTileData =
        InfoTileData(
            key = appVersionManager.getCurrentVersion(),
            title = appUpdateCopy.title,
            description = appUpdateCopy.description,
            type = InfoTileData.InfoTileType.APP_UPDATE,
            primaryCta = InfoTileCta(
                text = appUpdateCopy.ctaText,
                url = appInfoProvider.getAppInfo().appStoreUrl,
            )
        )

    private fun addInfoTile(
        currentTiles: List<InfoTileData>?,
        newTile: InfoTileData
    ): ImmutableList<InfoTileData> = (currentTiles ?: persistentListOf())
        .plus(newTile)
        .toSet()
        .sortedBy { it.type.priority }
        .take(MAX_INFO_TILE_COUNT)
        .toImmutableList()

    private fun isKeyNotInDismissedTiles(key: String): Boolean {
        log("Checking if info tile key '$key' is not in dismissed tiles.")
        return !preferences.isInfoTileDismissed(key)
    }
}
