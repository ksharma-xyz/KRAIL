package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import krail.feature.trip_planner.ui.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Composable
fun SearchStopRow(
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    fromStopItem: StopItem? = null,
    toStopItem: StopItem? = null,
    onReverseButtonClick: () -> Unit = {},
    onSearchButtonClick: () -> Unit = {},
) {
    val themeColor by LocalThemeColor.current
    var isReverseButtonRotated by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = themeColor.hexToComposeColor(),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            )
            .padding(vertical = 20.dp, horizontal = 16.dp)
            .padding(
                bottom = with(LocalDensity.current) {
                    WindowInsets.navigationBars
                        .getBottom(this)
                        .toDp()
                },
                top = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextFieldButton(onClick = fromButtonClick) {
                AnimatedContent(
                    targetState = fromStopItem?.stopName ?: "Starting from",
                    transitionSpec = {
                        (
                            fadeIn(
                                animationSpec = tween(200),
                            ) + slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(500, easing = EaseOutBounce),
                            )
                            ) togetherWith (
                            fadeOut(
                                animationSpec = tween(200),
                            ) + slideOutVertically(
                                targetOffsetY = { -it / 2 },
                                animationSpec = tween(500),
                            )
                            )
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "startingFromText",
                ) { targetText ->
                    Text(
                        text = targetText,
                        maxLines = 1,
                    )
                }
            }

            TextFieldButton(onClick = toButtonClick) {
                AnimatedContent(
                    targetState = toStopItem?.stopName ?: "Destination",
                    transitionSpec = {
                        (
                            fadeIn(
                                animationSpec = tween(200),
                            ) + slideInVertically(
                                initialOffsetY = { -it / 2 },
                                animationSpec = tween(500, easing = EaseOutBounce),
                            )
                            ) togetherWith (
                            fadeOut(
                                animationSpec = tween(200),
                            ) + slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = tween(500),
                            )
                            )
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "destinationText",
                ) { targetText ->
                    Text(
                        text = targetText,
                        maxLines = 1,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp), // TODO - token "SearchFieldSpacing"
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isReverseButtonRotated) 180f else 0f,
                animationSpec = tween(durationMillis = 300)
            )

            RoundIconButton(
                content = {
                    Image(
                        painter = painterResource(Res.drawable.ic_reverse),
                        contentDescription = "Reverse",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = {
                    isReverseButtonRotated = !isReverseButtonRotated
                    onReverseButtonClick()
                },
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                }
            )

            RoundIconButton(
                content = {
                    Image(
                        painter = painterResource(Res.drawable.ic_search),
                        contentDescription = "Search",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onSearchButtonClick,
            )
        }
    }
}

// region Previews

@Composable
private fun SearchStopColumnPreview() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            SearchStopRow(
                fromButtonClick = {},
                toButtonClick = {},
            )
        }
    }
}

// endregion
