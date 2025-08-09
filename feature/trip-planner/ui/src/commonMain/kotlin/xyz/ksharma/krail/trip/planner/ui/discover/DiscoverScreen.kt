package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.appinfo.LocalAppInfo
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.ui.DiscoverCard
import xyz.ksharma.krail.discover.ui.DiscoverCardTablet
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.taj.components.DiscoverCardVerticalPager
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.AlternateLayout

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (shareUrl: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
) {
    AlternateLayout(
        modifier = Modifier.fillMaxSize(),
        compactContent = {
            DiscoverScreenCompactContent(
                modifier = modifier,
                state = state,
                onBackClick = onBackClick,
                onAppSocialLinkClicked = onAppSocialLinkClicked,
                onPartnerSocialLinkClicked = onPartnerSocialLinkClicked,
                onCtaClicked = onCtaClicked,
                onShareClick = onShareClick,
                onCardSeen = onCardSeen,
                resetAllSeenCards = resetAllSeenCards,
                onChipSelected = onChipSelected,
            )
        },
        tabletContent = {
            DiscoverScreenTabletContent(
                modifier = modifier,
                state = state,
                onBackClick = onBackClick,
                onAppSocialLinkClicked = onAppSocialLinkClicked,
                onPartnerSocialLinkClicked = onPartnerSocialLinkClicked,
                onCtaClicked = onCtaClicked,
                onShareClick = onShareClick,
                onCardSeen = onCardSeen,
                resetAllSeenCards = resetAllSeenCards,
                onChipSelected = onChipSelected,
            )
        }
    )
}

@Composable
private fun DiscoverScreenCompactContent(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (shareUrl: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
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
                    keySelector = { it.cardId },
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
            val density = LocalDensity.current
            val fontScale = density.fontScale

            Text(
                text = "What's On, Sydney!", // todo -dynamically from config.
                style = KrailTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (fontScale < 1.5f) 16.dp else 8.dp)
                    .background(color = Color.Transparent),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            KrailTheme.colors.surface.copy(alpha = 0.10f),
                            KrailTheme.colors.surface.copy(alpha = 0.95f),
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                )
                .navigationBarsPadding(),
        ) {
            DiscoverChipRow(
                chipTypes = state.sortedDiscoverCardTypes,
                selectedType = state.selectedType,
                modifier = Modifier.padding(vertical = 20.dp),
                onChipSelected = onChipSelected,
            )
        }
    }
}

@Composable
private fun DiscoverScreenTabletContent(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (shareUrl: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface),
    ) {
        Column {
            if (state.discoverCardsList.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    contentPadding = PaddingValues(top = 120.dp, start = 24.dp, end = 24.dp, bottom = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.discoverCardsList,
                        key = { it.cardId }
                    ) { cardModel ->
                        DiscoverCardTablet(
                            discoverModel = cardModel,
                            onAppSocialLinkClicked = onAppSocialLinkClicked,
                            onPartnerSocialLinkClicked = onPartnerSocialLinkClicked,
                            onCtaClicked = onCtaClicked,
                            onShareClick = { shareUrl ->
                                onShareClick(shareUrl, cardModel.cardId, cardModel.type)
                            },
                            modifier = Modifier.animateItem(),
                        )

                        LaunchedEffect(cardModel.cardId) {
                            onCardSeen(cardModel.cardId)
                        }
                    }
                }
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
            val density = LocalDensity.current
            val fontScale = density.fontScale

            Text(
                text = "What's On, Sydney!",
                style = KrailTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (fontScale < 1.5f) 16.dp else 8.dp)
                    .background(color = Color.Transparent),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            KrailTheme.colors.surface.copy(alpha = 0.10f),
                            KrailTheme.colors.surface.copy(alpha = 0.95f),
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                )
                .navigationBarsPadding(),
        ) {
            DiscoverChipRow(
                chipTypes = state.sortedDiscoverCardTypes,
                selectedType = state.selectedType,
                modifier = Modifier.padding(vertical = 20.dp),
                onChipSelected = onChipSelected,
            )
        }
    }
}
