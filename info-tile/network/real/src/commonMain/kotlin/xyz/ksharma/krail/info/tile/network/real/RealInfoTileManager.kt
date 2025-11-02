package xyz.ksharma.krail.info.tile.network.real

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateInFuture
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.JsonConfig
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager.Companion.MAX_INFO_TILE_COUNT
import xyz.ksharma.krail.info.tile.network.api.db.isInfoTileDismissed
import xyz.ksharma.krail.info.tile.network.api.db.markInfoTileAsDismissed
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.sandook.SandookPreferences

class RealInfoTileManager(
    private val appVersionManager: AppVersionManager,
    private val appInfoProvider: AppInfoProvider,
    private val preferences: SandookPreferences,
    private val flag: Flag,
    private val ioDispatcher: CoroutineDispatcher = DispatchersComponent().ioDispatcher,
) : InfoTileManager {

    /**
     * Cache the last fetched [FlagValue] and the parsed list of [InfoTileData].
     */
    private var cachedInfoTilesFlagValue: FlagValue? = null

    /** This cache is only valid for the lifetime of this instance.
     * If the [FlagValue] changes, the cache will be updated on the next call to [getInfoTiles].
     */
    private var cachedParsedTiles: List<InfoTileData>? = null

    // TODO - add UT's for this method
    override suspend fun getInfoTiles(): List<InfoTileData> = withContext(ioDispatcher) {
        val appUpdateTile = getAppUpdateTileOrNull()
        val configInfoTilesList = flag.getFlagValue(FlagKeys.INFO_TILES.key).toInfoTileList()

        val allTiles = (configInfoTilesList + listOfNotNull(appUpdateTile))
            .filterExpiredTiles()
            .filterDismissedTiles()
            .distinctBy { it.key }
            .sortedBy { it.type.priority }
            .take(MAX_INFO_TILE_COUNT)

        return@withContext allTiles
    }

    override fun isInfoTileActive(key: String): Boolean {
        log("Checking if info tile key '$key' is active (not dismissed).")
        return !preferences.isInfoTileDismissed(key)
    }

    override fun markInfoTileDismissed(infoTileData: InfoTileData) {
        log("Marking info tile key '${infoTileData.key}' as dismissed.")
        preferences.markInfoTileAsDismissed(infoTileData.key)
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
            ),
        )
    }

    private fun List<InfoTileData>.filterDismissedTiles(): List<InfoTileData> =
        filter { isKeyNotInDismissedTiles(it.key) }

    private fun List<InfoTileData>.filterExpiredTiles(): List<InfoTileData> =
        filter { it.endDate?.isDateInFuture() != false }

    private fun isKeyNotInDismissedTiles(key: String): Boolean {
        log(
            "Checking if info tile key '$key' is not in dismissed tiles. : ${
                preferences.isInfoTileDismissed(
                    key,
                )
            }",
        )
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
        cachedParsedTiles?.let { cachedTiles ->
            if (cachedInfoTilesFlagValue == this) return cachedTiles
        }

        val jsonStr = when (this) {
            is FlagValue.JsonValue -> this.value
            else -> RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.INFO_TILES.key }?.second as? String ?: "[]"
        }

        return try {
            JsonConfig.lenient.decodeFromString<List<InfoTileData>>(jsonStr)
        } catch (e: Exception) {
            logError("Error decoding info tiles json : $jsonStr", e)
            emptyList()
        }
    }
}
