package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.trip.planner.ui.navigation.DiscoverRoute

internal fun NavGraphBuilder.discoverDestination(navController: NavHostController) {
    composable<DiscoverRoute> {
        val viewModel: DiscoverViewModel = koinViewModel<DiscoverViewModel>()
        val discoverState by viewModel.uiState.collectAsStateWithLifecycle()

        DiscoverScreen(
            state = discoverState,
            onBackClick = {
                navController.navigateUp()
            },
            onAppSocialLinkClicked = { krailSocialType ->
                viewModel.onEvent(
                    event = DiscoverEvent.AppSocialLinkClicked(krailSocialType = krailSocialType)
                )
            },
            onPartnerSocialLinkClicked = { partnerSocialLink, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.PartnerSocialLinkClicked(
                        partnerSocialLink = partnerSocialLink,
                        cardId = cardId,
                        cardType = cardType,
                    )
                )
            },
            onCtaClicked = { url, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.CtaButtonClicked(
                        url = url,
                        cardId = cardId,
                        cardType = cardType,
                    )
                )
            },
            onFeedbackCta = { isPositive, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.FeedbackCtaButtonClicked(
                        isPositive = isPositive,
                        cardId = cardId,
                        cardType = cardType,
                    )
                )
            },
            onFeedbackThumb = { isPositive, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.FeedbackThumbButtonClicked(
                        isPositive = isPositive,
                        cardId = cardId,
                        cardType = cardType,
                    )
                )
            },
            onShareClick = { shareUrl, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.ShareButtonClicked(
                        cardTitle = shareUrl,
                        cardId = cardId,
                        cardType = cardType,
                        url = shareUrl,
                    )
                )
            },
            onCardSeen = { cardId ->
                viewModel.onEvent(
                    event = DiscoverEvent.CardSeen(cardId = cardId)
                )
            },
            resetAllSeenCards = {
                viewModel.onEvent(event = DiscoverEvent.ResetAllSeenCards)
            }
        )
    }
}
