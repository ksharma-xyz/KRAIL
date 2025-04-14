// Kotlin
package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import kotlin.math.absoluteValue
import kotlin.math.min

@Composable
fun IntroScreen(
    state: IntroState,
    modifier: Modifier = Modifier,
    onEvent: (IntroUiEvent) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            Text(
                text = "Intro Screen",
                style = KrailTheme.typography.title,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
            )

            val pagerState = rememberPagerState(pageCount = { state.pages.size })

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                val screenHeight = maxHeight
                val selectedHeight = screenHeight * 0.70f
                val unselectedHeight = screenHeight * 0.6f

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 64.dp),
                    pageSpacing = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { pageNumber ->

                    val pageOffset =
                        pagerState.calculateCurrentOffsetForPage(pageNumber).absoluteValue
                    val animatedHeight by animateDpAsState(
                        targetValue = lerp(selectedHeight, unselectedHeight, min(1f, pageOffset)),
                        label = "cardHeight"
                    )

                    val scale = lerp(1f, 0.9f, min(1f, pageOffset))

                    // Retrieve page data from state
                    val pageData = state.pages[pageNumber]

                    Column(
                        modifier = Modifier
                            .zIndex(1f - pageOffset)
                            .height(animatedHeight)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .fillMaxWidth()
                            .drawWithContent {
                                // Border thickness and corner radius conversions
                                val borderThicknessPx = 8.dp.toPx()
                                val cornerRadiusPx = 24.dp.toPx()
                                // Fraction determines how much to blend from the real color to grey.
                                // When pageOffset is 0, the page is selected and uses the actual gradient.
                                // When pageOffset is near 1, the colors are nearly grey.
                                val fraction = min(1f, pageOffset)
                                val grey = Color(0xFF888888)
                                val gradientColors = pageData.colorsList.map { it.hexToComposeColor() }
                                    .map { originalColor ->
                                        lerp(originalColor, grey, fraction)
                                    }
                                val gradientBrush = Brush.linearGradient(
                                    colors = gradientColors,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                                drawContent()
                                drawRoundRect(
                                    brush = gradientBrush,
                                    size = size,
                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                    style = Stroke(width = borderThicknessPx)
                                )
                            }
                            .verticalScroll(rememberScrollState())
                    ) {
                        IntroContentSaveTrips(
                            tagline = pageData.tagline,
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(text = "Let's #KRAIL")
            }
        }
    }
}

private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}