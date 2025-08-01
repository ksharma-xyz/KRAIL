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

    // Cache the parsed JSON card list to avoid repeated parsing.
    private var cachedFlagValue: FlagValue? = null
    private var cachedParsedCards: List<DiscoverModel>? = null

    /**
     * Fetches Discover cards for the UI.
     * - Caches the parsed JSON card list to avoid repeated parsing.
     * - Always sorts the cached list to reflect up-to-date "seen" status.
     * - This ensures sorting is always correct, but parsing is only done when the flag changes.
     */
    override suspend fun fetchDiscoverData(): List<DiscoverModel> {
        val currentFlagValue = flag.getFlagValue(FlagKeys.DISCOVER_SYDNEY.key)
        val parsedCards = getOrParseCards(currentFlagValue)
        // Always sort the cached cards to reflect latest "seen" status.
        val sortedModels = discoverCardOrderingEngine.getSortedCards(parsedCards)
        log("Fetched and sorted Discover Sydney cards data: ${sortedModels.size} cards")
        return sortedModels
    }

    /**
     * Returns the cached parsed cards if the flag value hasn't changed,
     * otherwise parses and caches the new card list.
     * This avoids repeated JSON parsing for the same flag value.
     */
    private suspend fun getOrParseCards(currentFlagValue: FlagValue): List<DiscoverModel> {
        if (cachedFlagValue == currentFlagValue && cachedParsedCards != null) {
            // Return cached parsed cards if flag hasn't changed.
            return cachedParsedCards!!
        }
        // Parse and cache new cards if flag value changed.
        val discoverCards: List<DiscoverModel> = parseCardsFromFlag(currentFlagValue)
        cachedFlagValue = currentFlagValue
        cachedParsedCards = discoverCards
        return discoverCards
    }

    /**
     * Parses the Discover cards from the given flag value.
     * Uses default value if flag is not a JSON value.
     */
    private suspend fun parseCardsFromFlag(flagValue: FlagValue): List<DiscoverModel> {
        log("Parsing Discover Sydney data from flag: ${FlagKeys.DISCOVER_SYDNEY.key}")
        return withContext(defaultDispatcher) {
            when (flagValue) {
                is FlagValue.JsonValue -> {
                    val jsonArray = Json.parseToJsonElement(flagValue.value).jsonArray
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

    override suspend fun markCardAsSeen(cardId: String) {
        log("Marking card as seen: $cardId")
        discoverCardOrderingEngine.markCardAsSeen(cardId)
    }

    override suspend fun resetAllSeenCards() {
        log("Resetting all seen cards")
        discoverCardOrderingEngine.resetAllSeenCards()
    }

    override fun feedbackThumbButtonClicked(feedbackId: String, isPositive: Boolean) {
        // Save in local db, so that we don't show the same feedback to user again.
        log("Feedback thumb button clicked: feedbackId=$feedbackId, isPositive=$isPositive")
        // TODO: implement feedback saving logic
    }
}
