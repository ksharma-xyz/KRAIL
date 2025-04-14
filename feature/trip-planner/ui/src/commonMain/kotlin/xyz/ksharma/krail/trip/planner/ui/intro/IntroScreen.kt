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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
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

            val pagerState = rememberPagerState(pageCount = { 5 })
            val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)

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

                    val greyOverlayAlpha = min(0.6f, pageOffset * 1.2f) // Gradual tinting
                    val greyOverlay = Color(0xFF888888).copy(alpha = greyOverlayAlpha)

                    Column(
                        modifier = Modifier
                            .zIndex(1f - pageOffset)
                            .height(animatedHeight)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .fillMaxWidth()
                            .drawWithCache {
                                // Border thickness (4.dp) conversion to pixels
                                val borderThicknessPx = 5.dp.toPx()
                                // Define the corner radius for the rounded border
                                val cornerRadiusPx = 24.dp.toPx()

                                // Define a diagonal gradient from top left to bottom right
                                val gradientBrush = Brush.linearGradient(
                                    colors = listOf(
                                        KrailThemeStyle.Metro.hexColorCode.hexToComposeColor(),
                                        KrailThemeStyle.Bus.hexColorCode.hexToComposeColor(),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                                // Cache the border drawing: draw the content and then draw the gradient border around it
                                onDrawWithContent {
                                    drawContent()
                                    // Draw the border as a stroke over the full composable bounds
                                    drawRoundRect(
                                        brush = gradientBrush,
                                        size = size,
                                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                        style = Stroke(width = borderThicknessPx)
                                    )
                                }
                            }
                            .verticalScroll(rememberScrollState())
                    ) {
                        IntroContentSaveTrips(modifier = Modifier.padding(20.dp).fillMaxSize())
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
