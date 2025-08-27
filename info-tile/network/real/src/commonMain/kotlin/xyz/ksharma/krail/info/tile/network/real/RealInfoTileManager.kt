package xyz.ksharma.krail.info.tile.network.real

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
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
    private val flag: Flag,
) : InfoTileManager {

    private val configInfoTilesList = flag.getFlagValue(FlagKeys.INFO_TILES.key).toInfoTileList()

    override suspend fun getInfoTiles(): List<InfoTileData> {

        val appUpdateTile = getAppUpdateTileOrNull()

        val allTiles = (configInfoTilesList + listOfNotNull(appUpdateTile))
            .filterDismissedTiles()
            .distinctBy { it.key }
            .sortedBy { it.type.priority }
            .take(MAX_INFO_TILE_COUNT)

        return allTiles
    }

    private suspend fun getAppUpdateTileOrNull(): InfoTileData? {
        val appUpdateCopy = appVersionManager.getUpdateCopy() ?: return null
        return InfoTileData(
            key = appVersionManager.getCurrentVersion(),
            title = appUpdateCopy.title,
            description = appUpdateCopy.description,
            type = InfoTileData.InfoTileType.APP_UPDATE,
            primaryCta = InfoTileCta(
                text = appUpdateCopy.ctaText,
                url = appInfoProvider.getAppInfo().appStoreUrl,
            )
        )
    }

    private fun List<InfoTileData>.filterDismissedTiles(): List<InfoTileData> =
        filter { isKeyNotInDismissedTiles(it.key) }

    private fun isKeyNotInDismissedTiles(key: String): Boolean {
        log("Checking if info tile key '$key' is not in dismissed tiles.")
        return !preferences.isInfoTileDismissed(key)
    }

    /**
     * Convert the FlagValue to a list of InfoTileData.
     * If the FlagValue is not a JsonValue, use the default value from RemoteConfigDefaults.
     * If decoding fails, log the error and return an empty list.
     *
     * Note: This function assumes that the JSON structure matches the InfoTileData data class.
     * If the structure changes, this function may need to be updated accordingly.
     * Also, the same model object is being used for parsing from remote config and for UI state,
     * for less boilerplate and low maintenance.
     *
     * @return List of InfoTileData
     */
    private fun FlagValue.toInfoTileList(): List<InfoTileData> {
        val jsonStr = when (this) {
            is FlagValue.JsonValue -> this.value
            else -> RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.INFO_TILES.key }?.second as? String ?: "[]"
        }

        return try {
            Json.decodeFromString<List<InfoTileData>>(jsonStr)
        } catch (e: Exception) {
            logError("Error decoding info tiles json : $jsonStr", e)
            emptyList()
        }
    }
}
