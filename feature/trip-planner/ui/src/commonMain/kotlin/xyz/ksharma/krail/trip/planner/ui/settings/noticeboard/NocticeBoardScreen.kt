package xyz.ksharma.krail.trip.planner.ui.settings.noticeboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import kotlin.math.absoluteValue

@Composable
fun VerticalCardStack(
    pages: List<String>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val density = LocalDensity.current

    val screenHeight = with(density) { LocalDensity.current.run { 600.dp.toPx() } } // adjust as needed
    val selectedHeight = 0.75f * screenHeight
    val unselectedHeight = 0.6f * screenHeight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        VerticalPager(
            state = pagerState,
            pageSpacing = 20.dp,
            contentPadding = PaddingValues(vertical = 64.dp),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = pagerState.calculateCurrentOffsetForPage(page).absoluteValue
            val height by animateDpAsState(
                targetValue = lerpDp(
                    selectedHeight.dp,
                    unselectedHeight.dp,
                    pageOffset.coerceIn(0f, 1f)
                ),
                label = "cardHeight"
            )
            val scale = lerp(1f, 0.9f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1f, 0.2f, pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .height(height)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .background(
                        color = when (page % 4) {
                            0 -> Color(0xFFE57373)
                            1 -> Color(0xFF64B5F6)
                            2 -> Color(0xFF81C784)
                            else -> Color(0xFFFFB74D)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .zIndex(1f - pageOffset)
            ) {
                Text(
                    text = pages[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    color = Color.White
                )
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
    VerticalCardStack(pages = sampleCards)
}