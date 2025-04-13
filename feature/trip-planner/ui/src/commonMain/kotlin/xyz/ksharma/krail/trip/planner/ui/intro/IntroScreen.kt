package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import kotlin.math.absoluteValue
import kotlin.math.min

val colors = listOf(
    Color.Red,
    Color.Blue,
    Color.Green,
    Color.Yellow,
    Color.Magenta,
)

@Composable
fun IntroScreen(
    state: IntroState,
    modifier: Modifier = Modifier,
    onEvent: (IntroUiEvent) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Text(
            text = "Intro Screen",
            style = KrailTheme.typography.title,
            modifier = Modifier.padding(16.dp),
        )

        val pagerState = rememberPagerState(pageCount = { 5 })

        HorizontalPager(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 24.dp
        ) { pageNumber ->

            val pageOffset = pagerState.calculateCurrentOffsetForPage(pageNumber).absoluteValue

            // Animate height
            val maxHeight = 600.dp
            val minHeight = 450.dp
            val animatedHeight: Dp by animateDpAsState(
                targetValue = lerp(maxHeight, minHeight, min(1f, pageOffset)),
                label = "cardHeight"
            )

            // Animate scale
            val scale = lerp(1f, 0.9f, min(1f, pageOffset))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .zIndex(1f - pageOffset) // ensure selected page is on top
                    .height(animatedHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors[pageNumber % colors.size])
            )
        }
    }
}

// Offset calculation helper
private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

// Linear interpolation between two Dp values
private fun lerp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
