package xyz.ksharma.krail.discover.network.real

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import xyz.ksharma.krail.discover.network.api.DiscoverModel
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager

internal class RealDiscoverSydneyManager(
    private val flag: Flag,
    private val defaultDispatcher: CoroutineDispatcher = DispatchersComponent().defaultDispatcher,
) : DiscoverSydneyManager {

    private var cachedFlagValue: FlagValue? = null
    private var cachedDiscoverModels: List<DiscoverModel>? = null

    override suspend fun fetchDiscoverData(): List<DiscoverModel> {
        val currentFlagValue = flag.getFlagValue(FlagKeys.DISCOVER_SYDNEY.key)
        if (cachedFlagValue == currentFlagValue && cachedDiscoverModels != null) {
            return cachedDiscoverModels!!
        }
        val models = currentFlagValue.toDiscoverCards()
        cachedFlagValue = currentFlagValue
        cachedDiscoverModels = models
        return models
    }

    private suspend fun FlagValue.toDiscoverCards(): List<DiscoverModel> {
        val flagValue = this
        log("Fetching Discover Sydney data from flag: ${FlagKeys.DISCOVER_SYDNEY.key}: $flagValue")

        return withContext(defaultDispatcher) {
            when (flagValue) {
                is FlagValue.JsonValue -> {
                    val jsonArray = Json.parseToJsonElement(value).jsonArray
                    jsonArray.map { Json.decodeFromJsonElement<DiscoverModel>(it) }
                }

                else -> {
                    log("FlagValue is not JsonValue, using default value for ${FlagKeys.DISCOVER_SYDNEY.key}")
                    val defaultJson = RemoteConfigDefaults.getDefaults()
                        .firstOrNull { it.first == FlagKeys.DISCOVER_SYDNEY.key }?.second as? String
                    if (defaultJson == null) {
                        emptyList()
                    } else {
                        val jsonArray = Json.parseToJsonElement(defaultJson).jsonArray
                        jsonArray.map { Json.decodeFromJsonElement<DiscoverModel>(it) }
                    }
                }
            }
        }
    }
}
