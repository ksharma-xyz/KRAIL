// Kotlin
package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import xyz.ksharma.krail.taj.components.ButtonDefaults
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
    onComplete: () -> Unit = {},
    onEvent: (IntroUiEvent) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { state.pages.size })

    // Determine the start page and target page based on drag direction.
    val startPage = pagerState.currentPage
    val nextPage = if (startPage < state.pages.lastIndex) startPage + 1 else startPage
    val dragFraction = pagerState.currentPageOffsetFraction
    val targetPage = when {
        dragFraction >= 0f && startPage < state.pages.lastIndex -> startPage + 1
        dragFraction < 0f && startPage > 0 -> startPage - 1
        else -> startPage
    }
    val offsetFraction = kotlin.math.abs(dragFraction)

    // Compute animated alpha:
    // For offset below 50%, alpha falls from 1 to 0.
    // For offset above 50%, alpha rises from 0 to 1.
    val animatedAlpha by animateFloatAsState(
        targetValue = if (offsetFraction < 0.5f) 1f - (offsetFraction * 2f)
        else (offsetFraction - 0.5f) * 2f
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            // Overlapped titles that fade based on the animated alphas.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                if (offsetFraction < 0.5f) {
                    Text(
                        text = state.pages[startPage].title,
                        style = KrailTheme.typography.title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(animatedAlpha)
                    )
                } else if (offsetFraction > 0.5f) {
                    Text(
                        text = state.pages[targetPage].title,
                        style = KrailTheme.typography.title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(animatedAlpha)
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                val screenHeight = maxHeight
                val selectedHeight = screenHeight * 0.75f
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
                                val borderThicknessPx = 8.dp.toPx()
                                val cornerRadiusPx = 24.dp.toPx()
                                val fraction = min(1f, pageOffset)
                                val grey = Color(0xFF888888)
                                val gradientColors = pageData.colorsList
                                    .map { it.hexToComposeColor() }
                                    .map { originalColor -> lerp(originalColor, grey, fraction) }
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
                    ) {
                        IntroPageContent(pageData, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        // Compute continuous button color by interpolating between current and next page colors.
        val currentButtonColor = state.pages[startPage].primaryStyle.hexToComposeColor()
        val nextButtonColor = state.pages[nextPage].primaryStyle.hexToComposeColor()
        val animatedButtonColor = lerp(currentButtonColor, nextButtonColor, offsetFraction)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    customContainerColor = animatedButtonColor,
                    customContentColor = Color.White,
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(text = state.pages[startPage].ctaText)
            }
        }
    }
}

@Composable
private fun IntroPageContent(pageData: IntroState.IntroPage, modifier: Modifier) {
    when (pageData.type) {
        IntroState.IntroPageType.SAVE_TRIPS -> {
            IntroContentSaveTrips(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),
            )
        }

        IntroState.IntroPageType.REAL_TIME_DATA -> {
            IntroContentRealTime(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),
            )
        }

        IntroState.IntroPageType.ALERTS -> {
            IntroContentAlerts(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),

                )
        }

        IntroState.IntroPageType.PLAN_TRIP -> {
            IntroContentPlanTrip(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),

                )
        }

        IntroState.IntroPageType.SELECT_MODE -> {
            IntroContentSelectTransportMode(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),

                )
        }

        IntroState.IntroPageType.INVITE_FRIENDS -> {
            IntroContentInviteFriends(
                tagline = pageData.tagline,
                style = pageData.primaryStyle,
                modifier = modifier.padding(20.dp),
            )
        }
    }
}

private fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction