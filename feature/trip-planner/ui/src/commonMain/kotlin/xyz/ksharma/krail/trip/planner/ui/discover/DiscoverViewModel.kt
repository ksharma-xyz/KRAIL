package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DiscoverCardClick
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DiscoverCardClick.PartnerSocialLink
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DiscoverCardClick.Source
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent.SocialConnectionSource
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.social.ui.toAnalyticsEventPlatform

// TODO - Add UTs
class DiscoverViewModel(
    private val discoverSydneyManager: DiscoverSydneyManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
    private val platformOps: PlatformOps,
    private val appInfoProvider: AppInfoProvider,
    private val appCoroutineScope: CoroutineScope,
) : ViewModel() {

    private val _uiState: MutableStateFlow<DiscoverState> = MutableStateFlow(DiscoverState())
    val uiState: StateFlow<DiscoverState> = _uiState
        .onStart {
            fetchDiscoverCards()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoverState())

    var fetchDiscoverCardsJob: Job? = null

    // track seen cards for analytics purposes
    val analyticsSessionSeenCardIds = mutableSetOf<String>()

    fun onEvent(event: DiscoverEvent) {
        when (event) {
            is DiscoverEvent.AppSocialLinkClicked -> {
                platformOps.openUrl(url = event.krailSocialType.url)
                analytics.track(
                    event = SocialConnectionLinkClickEvent(
                        socialPlatformType = event.krailSocialType.toAnalyticsEventPlatform(),
                        source = SocialConnectionSource.DISCOVER_CARD,
                    ),
                )
            }

            is DiscoverEvent.PartnerSocialLinkClicked -> {
                platformOps.openUrl(url = event.partnerSocialLink.url)
                analytics.track(
                    event = DiscoverCardClick(
                        source = Source.PARTNER_SOCIAL_LINK,
                        cardType = event.cardType.toAnalyticsCardType(),
                        cardId = event.cardId,
                        partnerSocialLink = PartnerSocialLink(
                            type = event.partnerSocialLink.type.toAnalyticsSocialType(),
                            url = event.partnerSocialLink.url,
                        )
                    ),
                )
            }

            is DiscoverEvent.CtaButtonClicked -> {
                platformOps.openUrl(url = event.url)
                analytics.track(
                    event = DiscoverCardClick(
                        source = Source.CTA_CLICK,
                        cardType = event.cardType.toAnalyticsCardType(),
                        cardId = event.cardId,
                    ),
                )
            }

            is DiscoverEvent.ShareButtonClicked -> {
                platformOps.sharePlainText(event.url, title = event.cardTitle)
                analytics.track(
                    event = DiscoverCardClick(
                        source = Source.SHARE_CLICK,
                        cardType = event.cardType.toAnalyticsCardType(),
                        cardId = event.cardId,
                    ),
                )
            }

            is DiscoverEvent.CardSeen -> onCardSeen(event.cardId)

            DiscoverEvent.ResetAllSeenCards -> onResetAllSeenCards()
        }
    }

    private fun onResetAllSeenCards() {
        viewModelScope.launch {
            if (appInfoProvider.getAppInfo().isDebug) {
                log("Resetting all seen cards")
                discoverSydneyManager.resetAllDiscoverCardsDebugOnly()
            }
        }
    }

    private fun onCardSeen(cardId: String) {
        viewModelScope.launchWithExceptionHandler<DiscoverViewModel>(ioDispatcher) {
            discoverSydneyManager.markCardAsSeen(cardId)
            analyticsSessionSeenCardIds.add(cardId)
        }
    }

    private fun fetchDiscoverCards() {
        fetchDiscoverCardsJob?.cancel()
        fetchDiscoverCardsJob =
            viewModelScope.launchWithExceptionHandler<DiscoverViewModel>(ioDispatcher) {
                val data = discoverSydneyManager.fetchDiscoverData().toDiscoverUiModelList()
                log("Fetched Discover Sydney data: ${data.size}")
                updateUiState {
                    copy(
                        discoverCardsList = data.toImmutableList(),
                    )
                }
            }
    }

    private fun List<DiscoverModel>.toDiscoverUiModelList(): List<DiscoverState.DiscoverUiModel> {
        return map { model ->
            DiscoverState.DiscoverUiModel(
                title = model.title,
                description = model.description,
                imageList = model.imageList.toPersistentList(),
                type = model.type,
                disclaimer = model.disclaimer,
                buttons = model.buttons?.toPersistentList(),
                cardId = model.cardId,
            )
        }
    }

    private fun updateUiState(block: DiscoverState.() -> DiscoverState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        log("DiscoverViewModel cleared")
        fetchDiscoverCardsJob?.cancel()

        // Track the completion of the discover card session
        appCoroutineScope.launchWithExceptionHandler<DiscoverViewModel>(ioDispatcher) {
            analytics.track(
                event = AnalyticsEvent.DiscoverCardSessionComplete(
                    cardSeenCount = analyticsSessionSeenCardIds.size,
                )
            )
        }
    }
}
