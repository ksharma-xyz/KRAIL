package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.appinfo.LocalAppInfo
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.ui.DiscoverCard
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.taj.components.DiscoverCardVerticalPager
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onFeedbackCta: (isPositive: Boolean, cardId: String, cardType: DiscoverCardType) -> Unit,
    onFeedbackThumb: (isPositive: Boolean, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (shareUrl: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column {

            if (state.discoverCardsList.isNotEmpty()) {
                // todo - for tablets use a normal scrolling list
                DiscoverCardVerticalPager(
                    pages = state.discoverCardsList,
                    modifier = Modifier.fillMaxSize(),
                    content = { cardModel, isCardSelected ->

                        if (isCardSelected) {
                            LaunchedEffect(cardModel.cardId) {
                                onCardSeen(cardModel.cardId)
                            }
                        }

                        DiscoverCard(
                            discoverModel = cardModel,
                            onAppSocialLinkClicked = onAppSocialLinkClicked,
                            onPartnerSocialLinkClicked = onPartnerSocialLinkClicked,
                            onCtaClicked = onCtaClicked,
                            onFeedbackThumb = { isPositive ->
                                onFeedbackThumb(
                                    isPositive,
                                    cardModel.cardId,
                                    cardModel.type,
                                )
                            },
                            onFeedbackCta = { isPositive ->
                                onFeedbackCta(
                                    isPositive,
                                    cardModel.cardId,
                                    cardModel.type,
                                )
                            },
                            onShareClick = { shareUrl ->
                                onShareClick(
                                    shareUrl,
                                    cardModel.cardId,
                                    cardModel.type,
                                )
                            }
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            KrailTheme.colors.surface.copy(alpha = 0.8f),
                            KrailTheme.colors.surface.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                ),
        ) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { },
                actions = {
                    val appInfo = LocalAppInfo.current
                    if (appInfo?.isDebug == true) {
                        Text(
                            "Reset", modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = resetAllSeenCards,
                            )
                        )
                    }
                }
            )

            Text(
                text = "What's On, Sydney!",
                style = KrailTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .background(color = Color.Transparent),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            KrailTheme.colors.surface.copy(alpha = 0.95f),
                            KrailTheme.colors.surface.copy(alpha = 0.8f),
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                ).height(100.dp),
        ) {
        }
    }
}
