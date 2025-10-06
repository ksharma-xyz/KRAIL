package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.appinfo.LocalAppInfo
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.discover.ui.DiscoverCard
import xyz.ksharma.krail.discover.ui.DiscoverCardTablet
import xyz.ksharma.krail.discover.ui.previewDiscoverCardList
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.taj.components.DiscoverCardVerticalPager
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.isLargeFontScale

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
) {
    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    if (windowWidthSizeClass == WindowWidthSizeClass.COMPACT) {
        DiscoverScreenCompact(
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
    } else {
        DiscoverScreenTablet(
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
}

@Composable
fun DiscoverScreenCompact(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
) {
    if (state.discoverCardsList.isEmpty()) {
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .systemBarsPadding(),
    ) {
        if (isLargeFontScale()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxSize(),
            ) {
                item {
                    DiscoverTitleBar(onBackClick, resetAllSeenCards)
                }

                stickyHeader {
                    DiscoverFooterChipsRow(state, onChipSelected)
                }

                items(
                    count = state.discoverCardsList.size,
                    key = { index -> state.discoverCardsList[index].cardId },
                ) { index ->
                    val cardModel = state.discoverCardsList[index]

                    LaunchedEffect(cardModel.cardId) {
                        onCardSeen(cardModel.cardId)
                    }

                    DiscoverCard(
                        discoverModel = cardModel,
                        onAppSocialLinkClicked = onAppSocialLinkClicked,
                        onPartnerSocialLinkClicked = onPartnerSocialLinkClicked,
                        onCtaClicked = onCtaClicked,
                        onShareClick = {
                            onShareClick(cardModel.cardId, cardModel.type)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        } else {
            DiscoverCardVerticalPager(
                pages = state.discoverCardsList,
                modifier = modifier.fillMaxSize(),
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
                        onShareClick = {
                            onShareClick(
                                cardModel.cardId,
                                cardModel.type,
                            )
                        },
                    )
                },
            )

            // Header with title bar
            DiscoverTitleBar(onBackClick, resetAllSeenCards)

            // Footer with chips
            DiscoverFooterChipsRow(state, onChipSelected)
        }
    }
}

@Composable
fun DiscoverScreenTablet(
    modifier: Modifier = Modifier,
    state: DiscoverState,
    onBackClick: () -> Unit,
    onAppSocialLinkClicked: (KrailSocialType) -> Unit,
    onPartnerSocialLinkClicked: (Button.Social.PartnerSocial.PartnerSocialLink, String, DiscoverCardType) -> Unit,
    onCtaClicked: (url: String, cardId: String, cardType: DiscoverCardType) -> Unit,
    onShareClick: (cardId: String, cardType: DiscoverCardType) -> Unit,
    onCardSeen: (cardId: String) -> Unit,
    resetAllSeenCards: () -> Unit,
    onChipSelected: (DiscoverCardType) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .systemBarsPadding(),
    ) {
        Column {
            if (isLargeFontScale()) {
                // Large font: centered lazy column
                LazyColumn(
                    contentPadding = PaddingValues(top = 120.dp, bottom = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = state.discoverCardsList.size,
                        key = { index -> state.discoverCardsList[index].cardId },
                    ) { index ->
                        val cardModel = state.discoverCardsList[index]

                        LaunchedEffect(cardModel.cardId) {
                            onCardSeen(cardModel.cardId)
                        }

                        DiscoverCardTablet(
                            discoverModel = cardModel,
                            onAppSocialLinkClicked = onAppSocialLinkClicked,
                            onPartnerSocialLinkClicked = { partnerSocialLink, cardId, type ->
                                onPartnerSocialLinkClicked(partnerSocialLink, cardId, type)
                            },
                            onCtaClicked = { url, cardId, type ->
                                onCtaClicked(url, cardId, type)
                            },
                            onShareClick = {
                                onShareClick(cardModel.cardId, cardModel.type)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else {
                // Normal font: grid layout
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    contentPadding = PaddingValues(
                        top = 120.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 200.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = state.discoverCardsList.size,
                        key = { index -> state.discoverCardsList[index].cardId },
                    ) { index ->
                        val cardModel = state.discoverCardsList[index]

                        LaunchedEffect(cardModel.cardId) {
                            onCardSeen(cardModel.cardId)
                        }

                        DiscoverCardTablet(
                            discoverModel = cardModel,
                            onAppSocialLinkClicked = onAppSocialLinkClicked,
                            onPartnerSocialLinkClicked = { partnerSocialLink, cardId, type ->
                                onPartnerSocialLinkClicked(partnerSocialLink, cardId, type)
                            },
                            onCtaClicked = { url, cardId, type ->
                                onCtaClicked(url, cardId, type)
                            },
                            onShareClick = {
                                onShareClick(cardModel.cardId, cardModel.type)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        // Header with title bar
        DiscoverTitleBar(onBackClick, resetAllSeenCards)

        // Footer with chips
        DiscoverFooterChipsRow(state, onChipSelected)
    }
}

@Composable
private fun BoxScope.DiscoverFooterChipsRow(
    state: DiscoverState,
    onChipSelected: (DiscoverCardType) -> Unit,
    modifier: Modifier = Modifier,
) {
    DiscoverChipRow(
        chipTypes = state.sortedDiscoverCardTypes,
        selectedType = state.selectedType,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        KrailTheme.colors.surface.copy(alpha = 0.10f),
                        KrailTheme.colors.surface.copy(alpha = 0.95f),
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                ),
            )
            .padding(vertical = 16.dp)
            .navigationBarsPadding(),
        onChipSelected = onChipSelected,
    )
}

@Composable
private fun BoxScope.DiscoverTitleBar(
    onBackClick: () -> Unit,
    resetAllSeenCards: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        KrailTheme.colors.surface.copy(alpha = 0.8f),
                        KrailTheme.colors.surface.copy(alpha = 0.95f),
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                ),
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
                        "Reset",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = resetAllSeenCards,
                        ),
                    )
                }
            },
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
}

// region Previews

@Preview(
    group = "Discover Card Tablet",
    showBackground = true,
    widthDp = 720,
    heightDp = 1024,
)
@Composable
private fun DiscoverScreenTabletLightPreview() {
    PreviewTheme {
        DiscoverScreen(
            state = DiscoverState(
                discoverCardsList = previewDiscoverCardList.take(4).toImmutableList(),
                sortedDiscoverCardTypes = persistentListOf(
                    DiscoverCardType.Travel,
                    DiscoverCardType.Events,
                    DiscoverCardType.Food,
                    DiscoverCardType.Sports,
                ),
                selectedType = DiscoverCardType.Travel,
            ),
            onBackClick = {},
            onAppSocialLinkClicked = {},
            onPartnerSocialLinkClicked = { _, _, _ -> },
            onCtaClicked = { _, _, _ -> },
            onShareClick = { _, _ -> },
            onCardSeen = {},
            resetAllSeenCards = {},
            onChipSelected = {},
        )
    }
}

@Preview(
    group = "Discover Card Tablet",
    showBackground = true,
    widthDp = 720,
    heightDp = 1024,
)
@Composable
private fun DiscoverScreenTabletDarkPreview() {
    PreviewTheme {
        DiscoverScreen(
            state = DiscoverState(
                discoverCardsList = previewDiscoverCardList.take(4).toImmutableList(),
                sortedDiscoverCardTypes = persistentListOf(
                    DiscoverCardType.Travel,
                    DiscoverCardType.Events,
                    DiscoverCardType.Food,
                    DiscoverCardType.Sports,
                ),
                selectedType = DiscoverCardType.Events,
            ),
            onBackClick = {},
            onAppSocialLinkClicked = {},
            onPartnerSocialLinkClicked = { _, _, _ -> },
            onCtaClicked = { _, _, _ -> },
            onShareClick = { _, _ -> },
            onCardSeen = {},
            resetAllSeenCards = {},
            onChipSelected = {},
        )
    }
}

@Preview(
    group = "Discover Card Phone",
    showBackground = true,
    widthDp = 480,
    heightDp = 854,
)
@Composable
private fun DiscoverScreenCompactLightPreview() {
    PreviewTheme {
        DiscoverScreen(
            state = DiscoverState(
                discoverCardsList = previewDiscoverCardList.take(3).toImmutableList(),
                sortedDiscoverCardTypes = persistentListOf(
                    DiscoverCardType.Travel,
                    DiscoverCardType.Events,
                    DiscoverCardType.Food,
                ),
                selectedType = DiscoverCardType.Food,
            ),
            onBackClick = {},
            onAppSocialLinkClicked = {},
            onPartnerSocialLinkClicked = { _, _, _ -> },
            onCtaClicked = { _, _, _ -> },
            onShareClick = { _, _ -> },
            onCardSeen = {},
            resetAllSeenCards = {},
            onChipSelected = {},
        )
    }
}

@Preview(
    group = "Discover Card Phone",
    showBackground = true,
    widthDp = 480,
    heightDp = 854,
)
@Composable
private fun DiscoverScreenCompactDarkPreview() {
    PreviewTheme {
        DiscoverScreen(
            state = DiscoverState(
                discoverCardsList = previewDiscoverCardList.take(3).toImmutableList(),
                sortedDiscoverCardTypes = persistentListOf(
                    DiscoverCardType.Travel,
                    DiscoverCardType.Events,
                    DiscoverCardType.Food,
                ),
                selectedType = DiscoverCardType.Sports,
            ),
            onBackClick = {},
            onAppSocialLinkClicked = {},
            onPartnerSocialLinkClicked = { _, _, _ -> },
            onCtaClicked = { _, _, _ -> },
            onShareClick = { _, _ -> },
            onCardSeen = {},
            resetAllSeenCards = {},
            onChipSelected = {},
        )
    }
}

// endregion Previews
