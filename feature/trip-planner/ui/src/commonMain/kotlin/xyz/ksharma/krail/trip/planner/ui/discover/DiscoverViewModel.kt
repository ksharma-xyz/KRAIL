package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.api.model.DiscoverModel
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.social.ui.toAnalyticsEventPlatform

class DiscoverViewModel(
    private val discoverSydneyManager: DiscoverSydneyManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
    private val platformOps: PlatformOps,
) : ViewModel() {

    private val _uiState: MutableStateFlow<DiscoverState> = MutableStateFlow(DiscoverState())
    val uiState: StateFlow<DiscoverState> = _uiState
        .onStart {
            fetchDiscoverCards()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoverState())

    fun onEvent(event: DiscoverEvent) {
        when (event) {
            is DiscoverEvent.AppSocialLinkClicked -> {
                platformOps.openUrl(url = event.krailSocialType.url)
                analytics.track(
                    event = AnalyticsEvent.SocialConnectionLinkClickEvent(
                        socialPlatform = event.krailSocialType.toAnalyticsEventPlatform(),
                    ),
                )
            }

            is DiscoverEvent.CtaButtonClicked -> {}
            is DiscoverEvent.FeedbackThumbButtonClicked -> {}
            is DiscoverEvent.PartnerSocialLinkClicked -> {}
            is DiscoverEvent.ShareButtonClicked -> {}
        }
    }

    private fun fetchDiscoverCards() {
        viewModelScope.launchWithExceptionHandler<DiscoverViewModel>(ioDispatcher) {
            val data = discoverSydneyManager.fetchDiscoverData().toDiscoverUiModelList()
            log("Fetched Discover Sydney data: ${data.size}")
            data.forEach {
                log("\tDiscover Card: ${it.type}")
            }
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
            )
        }
    }

    private fun updateUiState(block: DiscoverState.() -> DiscoverState) {
        _uiState.update(block)
    }
}
