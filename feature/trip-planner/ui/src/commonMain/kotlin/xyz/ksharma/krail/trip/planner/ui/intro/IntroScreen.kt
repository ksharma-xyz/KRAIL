package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import kotlin.math.absoluteValue

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
            modifier = modifier.padding(vertical = 24.dp),
            state = pagerState
        ) { pageNumber ->
            Box(
                Modifier
                    .graphicsLayer {
                        val pageOffset = pagerState.calculateCurrentOffsetForPage(pageNumber)
                        // translate the contents by the size of the page, to prevent the pages
                        // from sliding in from left or right and stays in the center
                        translationX = pageOffset * size.width
                        // apply an alpha to fade the current page in and the old page out
                        alpha = 1 - pageOffset.absoluteValue
                    }) {

                Column(
                    modifier = Modifier.height(300.dp).fillMaxWidth().padding(horizontal = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            color = colors[pageNumber % colors.size]
                        )
                ) {

                }

            }
        }
    }
}


fun Modifier.pagerFadeTransition(page: Int, pagerState: PagerState) =
    graphicsLayer {
        val pageOffset = pagerState.calculateCurrentOffsetForPage(page)
        translationX = pageOffset * size.width
        alpha = 1 - pageOffset.absoluteValue
    }


private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}
