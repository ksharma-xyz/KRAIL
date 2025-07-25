package xyz.ksharma.krail.discover.network.real

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import xyz.ksharma.krail.discover.network.api.DiscoverCardModel
import xyz.ksharma.krail.discover.network.api.DiscoverCardsProvider

internal class RealDiscoverCardsProvider(
    private val flag: Flag,
) : DiscoverCardsProvider {

    private val discoverCardModelList: List<DiscoverCardModel> by lazy {
        flag.getFlagValue(FlagKeys.DISCOVER_SYDNEY.key).toDiscoverCards()
    }

    override fun getCards(): List<DiscoverCardModel> {
        return discoverCardModelList
    }

    private fun FlagValue.toDiscoverCards(): List<DiscoverCardModel> {
        return when (this) {
            is FlagValue.JsonValue -> {
                log("flagValue: ${this.value}")
                val jsonArray = Json.parseToJsonElement(value).jsonArray
                jsonArray.map { Json.decodeFromJsonElement<DiscoverCardModel>(it) }
            }
            else -> {
                log("FlagValue is not JsonValue, using default value for ${FlagKeys.DISCOVER_SYDNEY.key}")
                val defaultJson = RemoteConfigDefaults.getDefaults()
                    .firstOrNull { it.first == FlagKeys.DISCOVER_SYDNEY.key }?.second as? String
                    ?: return emptyList()
                val jsonArray = Json.parseToJsonElement(defaultJson).jsonArray
                jsonArray.map { Json.decodeFromJsonElement<DiscoverCardModel>(it) }
            }
        }
    }}

