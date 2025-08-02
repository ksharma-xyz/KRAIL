package xyz.ksharma.krail.discover.network.api

import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.state.DiscoverState

interface DiscoverSydneyManager {

    /**
     * Returns a list of cards to be displayed in the Discover screen.
     */
    suspend fun fetchDiscoverData(): List<DiscoverModel>

    fun cardFeedbackSelected(cardId: String, isPositive: Boolean)

    suspend fun markCardAsSeen(cardId: String)

    suspend fun resetAllDiscoverCardsDebugOnly()

    // region Card Feedback
    fun getCardFeedback(cardId: String): DiscoverState.DiscoverUiModel.FeedbackState?

    fun markFeedbackAsCompleted(cardId: String)

    // endregion
}
