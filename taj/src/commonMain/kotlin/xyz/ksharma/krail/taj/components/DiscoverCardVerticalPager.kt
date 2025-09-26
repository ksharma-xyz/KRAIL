package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import xyz.ksharma.krail.taj.themeColor
import kotlin.compareTo
import kotlin.math.absoluteValue

@Composable
fun rememberCardHeight(): Dp {
    val density = LocalDensity.current
    if (density.fontScale <= 1.3f) {
        return 500.dp
    } else if (density.fontScale <= 1.7f) {
        return 525.dp
    } else {
        return 550.dp
    }
}

// todo - to be used only with a mobile , not for tablet.
@Composable
fun <T> DiscoverCardVerticalPager(
    pages: List<T>,
    modifier: Modifier = Modifier,
    keySelector: (T) -> String,
    content: @Composable (T, isCardSelected: Boolean) -> Unit,
) {
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    val discoverCardHeight = rememberCardHeight()

    // Reset to first page when list changes and current page is out of bounds
    LaunchedEffect(pages.map { keySelector(it) }) {
        if (pagerState.currentPage >= pages.size && pages.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val maxCardWidth = maxWidth - 24.dp
        val screenHeight = maxHeight
        val topPadding = ((screenHeight - discoverCardHeight) / 2).coerceAtLeast(0.dp)

        VerticalPager(
            state = pagerState,
            pageSpacing = 20.dp,
            pageSize = PageSize.Fixed(pageSize = discoverCardHeight),
            key = { page ->
                // Add bounds check to prevent crashes
                if (page < pages.size) keySelector(pages[page]) else "empty_$page"
            },
            contentPadding = PaddingValues(vertical = topPadding.coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Add bounds check before accessing pages
            if (page < pages.size) {
                val isCardSelected = pagerState.currentPage == page
                val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue

                val scale = lerp(1f, 0.90f, pageOffset.coerceIn(0f, 1f))

                // if light mode then 0.1f for dark mode 0.25f
                val alpha = if (isSystemInDarkTheme()) {
                    lerp(1f, 0.25f, pageOffset.coerceIn(0f, 1f))
                } else {
                    lerp(1f, 0.1f, pageOffset.coerceIn(0f, 1f))
                }

                // region Shadow for card
                val maxShadowAlpha = if (isSystemInDarkTheme()) 0.15f else 0.1f
                val targetShadowAlpha = if (isCardSelected) maxShadowAlpha else 0f
                val animatedShadowAlpha by animateFloatAsState(
                    targetValue = targetShadowAlpha,
                    animationSpec = tween(durationMillis = 50)
                )
                // endregion Shadow for card

                val cardModifier = Modifier
                    .height(discoverCardHeight)
                    .width(maxCardWidth)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .then(
                        if (isCardSelected) {
                            Modifier.dropShadow(
                                shape = RoundedCornerShape(16.dp),
                                shadow = Shadow(
                                    radius = 24.dp,
                                    color = themeColor(),
                                    spread = 2.dp,
                                    alpha = animatedShadowAlpha,
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .zIndex(1f - pageOffset)
                    .align(Alignment.Center)

                Box(
                    modifier = cardModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    content(pages[page], isCardSelected)
                }
            }
        }
    }
}

fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}
