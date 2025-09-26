package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.state.DiscoverState.DiscoverUiModel
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

    private val _allCards = MutableStateFlow<List<DiscoverUiModel>>(emptyList())
    private val _selectedType = MutableStateFlow<DiscoverCardType?>(null)
    val uiState: StateFlow<DiscoverState> = combine(
        _allCards,
        _selectedType
    ) { allCards, selectedType ->
        val filteredCards = filterDiscoverCards(selectedType, allCards)
        DiscoverState(
            discoverCardsList = filteredCards.toImmutableList(),
            sortedDiscoverCardTypes = allCards.extractSortedDiscoverCardTypes(),
            selectedType = selectedType ?: DiscoverCardType.Unknown,
        )
    }.onStart {
        fetchDiscoverCards() // Fetch cards when first subscriber comes
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
                val card = _allCards.value.firstOrNull { it.cardId == event.cardId }
                val ctaLink = uiState.value.getCtaOrPartnerSocialLinkForCard(event.cardId)

                platformOps.sharePlainText(
                    text = createShareText(
                        cardTitle = card?.title,
                        cardDescription = card?.description,
                        cardType = event.cardType,
                        ctaLink = ctaLink,
                    ),
                )
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

            is DiscoverEvent.FilterChipClicked -> onFilterChipClicked(event.cardType)
        }
    }

    private fun onFilterChipClicked(cardType: DiscoverCardType) {
        _selectedType.value = if (_selectedType.value == cardType) null else cardType
        analytics.track(AnalyticsEvent.DiscoverFilterChipSelected(cardType = cardType.toAnalyticsCardType()))
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
                val data = discoverSydneyManager.fetchDiscoverData()
                    .toDiscoverUiModelList()
                log("Fetched Discover Sydney data: ${data.size}")
                _allCards.value = data // Update _allCards instead of _uiState
            }
    }

    private fun List<DiscoverUiModel>.extractSortedDiscoverCardTypes(): ImmutableList<DiscoverCardType> {
        val sortedTypes = this
            .map { it.type }
            .toSet()
            .sortedBy { discoverCardType -> discoverCardType.sortOrder }
            .toImmutableList()
        return sortedTypes
    }

    private fun List<DiscoverModel>.toDiscoverUiModelList(): List<DiscoverUiModel> {
        return map { model ->
            DiscoverUiModel(
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

    private fun filterDiscoverCards(
        selectedType: DiscoverCardType?,
        allCards: List<DiscoverUiModel>
    ): List<DiscoverUiModel> = if (selectedType != null) {
        allCards.filter { it.type == selectedType }
    } else {
        allCards
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

/**
 * Returns the first CTA or PartnerSocial link URL for the card, or null if none found.
 */
fun DiscoverState.getCtaOrPartnerSocialLinkForCard(cardId: String): String? {
    val card = discoverCardsList.firstOrNull { it.cardId == cardId }
    card?.buttons?.forEach { button ->
        when (button) {
            is Button.Cta -> return button.url

            is Button.Social.PartnerSocial -> {
                val firstLink = button.links.firstOrNull()
                if (firstLink != null) return firstLink.url
            }

            else -> Unit
        }
    }
    return null
}

private fun createShareText(
    cardTitle: String?,
    cardDescription: String?,
    cardType: DiscoverCardType,
    ctaLink: String? = null
): String {
    log("ctaLink: $ctaLink")
    val letsKrailText = "\n#LetsKRAIL https://krail.app"

    val postfix = when (cardType) {
        DiscoverCardType.Travel -> "\n$ctaLink\n\nFound in KRAIL App$letsKrailText"
        DiscoverCardType.Events -> "\n$ctaLink\n\nSee more events in KRAIL App$letsKrailText"
        DiscoverCardType.Food -> "\n$ctaLink\n\nDiscovered in KRAIL App$letsKrailText"
        DiscoverCardType.Sports -> "\n$ctaLink\n\nGame on with KRAIL App$letsKrailText"
        DiscoverCardType.Unknown -> "\n$ctaLink\n\nShared via KRAIL App$letsKrailText"
    }
    val text = (cardTitle ?: "") + "\n" + (cardDescription ?: "") + postfix
    log("Share text created: $text")
    return text
}
