package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import kotlin.math.absoluteValue

@Composable
fun DiscoverCardVerticalPager(
    pages: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        val maxCardWidth = maxWidth - 48.dp // 24.dp padding on each side
        val selectedWidth = 1f * maxCardWidth.value
        val unselectedWidth = 0.9f * maxCardWidth.value
        val cardHeight = 480.dp

        VerticalPager(
            state = pagerState,
            pageSpacing = 10.dp,
            pageSize = PageSize.Fixed(cardHeight),
            key = { (it % pages.size).let { idx -> pages[idx] } },
            contentPadding = PaddingValues(vertical = 64.dp),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val actualPage = page % pages.size
            val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue
            val width by animateDpAsState(
                targetValue = lerpDp(
                    selectedWidth.dp,
                    unselectedWidth.dp,
                    pageOffset.coerceIn(0f, 1f)
                ),
                label = "cardWidth"
            )
            val scale = lerp(1f, 0.95f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1f, 0.2f, pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .height(cardHeight)
                    .width(width)
                    .padding(horizontal = 0.dp)
                    .background(
                        color = when (actualPage % 4) {
                            0 -> Color(0xFFE57373)
                            1 -> Color(0xFF64B5F6)
                            2 -> Color(0xFF81C784)
                            else -> Color(0xFFFFB74D)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .zIndex(1f - pageOffset)
            ) {
                content()
            }
        }
    }
}

fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction
}

fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

@Preview
@Composable
private fun VerticalCardStackPreview() {
    val sampleCards = listOf("Notice 1", "Notice 2", "Notice 3", "Notice 4")
    DiscoverCardVerticalPager(
        pages = sampleCards,
        content = {
        })
}