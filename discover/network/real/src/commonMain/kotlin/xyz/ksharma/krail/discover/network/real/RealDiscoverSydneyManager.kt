package xyz.ksharma.krail.discover.network.real

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import xyz.ksharma.krail.discover.network.api.DiscoverModel
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager

internal class RealDiscoverSydneyManager(
    private val flag: Flag,
) : DiscoverSydneyManager {

    private val discoverModelList: List<DiscoverModel> by lazy {
        flag.getFlagValue(FlagKeys.DISCOVER_SYDNEY.key).toDiscoverCards()
    }

    override fun fetchDiscoverData(): List<DiscoverModel> {
        return discoverModelList
    }

    private fun FlagValue.toDiscoverCards(): List<DiscoverModel> {
        return when (this) {
            is FlagValue.JsonValue -> {
                log("flagValue: ${this.value}")
                val jsonArray = Json.parseToJsonElement(value).jsonArray
                jsonArray.map { Json.decodeFromJsonElement<DiscoverModel>(it) }
            }
            else -> {
                log("FlagValue is not JsonValue, using default value for ${FlagKeys.DISCOVER_SYDNEY.key}")
                val defaultJson = RemoteConfigDefaults.getDefaults()
                    .firstOrNull { it.first == FlagKeys.DISCOVER_SYDNEY.key }?.second as? String
                    ?: return emptyList()
                val jsonArray = Json.parseToJsonElement(defaultJson).jsonArray
                jsonArray.map { Json.decodeFromJsonElement<DiscoverModel>(it) }
            }
        }
    }}

