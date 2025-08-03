package xyz.ksharma.krail.taj.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.theme.KrailTheme
import kotlin.math.absoluteValue

val discoverCardHeight = 550.dp

// todo - to be used only with a mobile , not for tablet.
@Composable
fun <T> DiscoverCardVerticalPager(
    pages: List<T>,
    modifier: Modifier = Modifier,
    keySelector: (T) -> String,
    content: @Composable (T, isCardSelected: Boolean) -> Unit,
) {
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val maxCardWidth = maxWidth - 48.dp // 24.dp padding on each side

        // Calculate padding to center the selected item
        val screenHeight = maxHeight
        val topPadding = ((screenHeight - discoverCardHeight) / 2).coerceAtLeast(0.dp)

        if (pages.isEmpty()) {
            return@BoxWithConstraints
        }

        VerticalPager(
            state = pagerState,
            pageSpacing = 20.dp,
            pageSize = PageSize.Fixed(
                pageSize = discoverCardHeight,
            ),
            key = { page ->
                val actualPage = page % pages.size
                keySelector(pages[actualPage])
            },
            contentPadding = PaddingValues(vertical = topPadding.coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val actualPage = page % pages.size
            val isCardSelected = pagerState.currentPage == page
            val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue

            val scale = lerp(1f, 0.95f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1f, 0.2f, pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .zIndex(2f - pageOffset)
                    .height(discoverCardHeight)
                    .width(maxCardWidth)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                content(pages[actualPage], isCardSelected)
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

@Preview
@Composable
private fun VerticalCardStackPreview() {
    KrailTheme {
/*
        DiscoverCardVerticalPager(
            items = discoverCardList,
            content = { cardModel ->
                DiscoverCard(discoverCardModel = cardModel)
            }
        )
*/
    }
}