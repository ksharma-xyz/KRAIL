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
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel

internal class RealDiscoverSydneyManager(
    private val flag: Flag,
    private val defaultDispatcher: CoroutineDispatcher = DispatchersComponent().defaultDispatcher,
    private val discoverCardOrderingEngine: DiscoverCardOrderingEngine
) : DiscoverSydneyManager {

    private var cachedFlagValue: FlagValue? = null
    private var cachedDiscoverModels: List<DiscoverModel>? = null

    override suspend fun fetchDiscoverData(): List<DiscoverModel> {
        val currentFlagValue = flag.getFlagValue(FlagKeys.DISCOVER_SYDNEY.key)
        if (cachedFlagValue == currentFlagValue && cachedDiscoverModels != null) {
            return cachedDiscoverModels!!
        }
        val discoverModelList = currentFlagValue.toDiscoverCards()

        // Sort cards using the ordering engine (which checks seen status)
        val sortedModels = discoverCardOrderingEngine.getSortedCards(discoverModelList)
        cachedFlagValue = currentFlagValue
        cachedDiscoverModels = sortedModels

        return sortedModels
    }

    override suspend fun markCardAsSeen(cardId: String) {
        log("Marking card as seen: $cardId")
        discoverCardOrderingEngine.markCardAsSeen(cardId)
    }

    override fun feedbackThumbButtonClicked(feedbackId: String, isPositive: Boolean) {
        // save in local db, so that we don't show the same feedback to user again.
        log("Feedback thumb button clicked: feedbackId=$feedbackId, isPositive=$isPositive")
        // todo implement feedback saving logic
    }

    // todo - Move to another mapper file.
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
