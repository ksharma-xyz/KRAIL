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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import krail.feature.trip_planner.ui.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private val SearchRowTopRadius = 36.dp // no token equivalent (between RadiusXL=24 and RadiusFull=50)
private val SearchRowVerticalPadding = 20.dp // no token equivalent
private val SearchFieldSpacing = 20.dp // no token equivalent — TODO token "SearchFieldSpacing"

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
    val dim = KrailTheme.dimensions
    val themeColor by LocalThemeColor.current
    var isReverseButtonRotated by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = themeColor.hexToComposeColor(),
                shape = RoundedCornerShape(topStart = SearchRowTopRadius, topEnd = SearchRowTopRadius),
            )
            .padding(vertical = SearchRowVerticalPadding, horizontal = dim.pageHorizontalPadding)
            .padding(
                bottom = with(LocalDensity.current) {
                    WindowInsets.navigationBars
                        .getBottom(this)
                        .toDp()
                },
                top = dim.spacingM,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
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
                    ThemeTextFieldPlaceholderText(
                        text = targetText,
                        isActive = fromStopItem != null,
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
                    ThemeTextFieldPlaceholderText(
                        text = targetText,
                        isActive = toStopItem != null,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(start = dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isReverseButtonRotated) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
            )

            RoundIconButton(
                content = {
                    Image(
                        painter = painterResource(Res.drawable.ic_reverse),
                        contentDescription = "Reverse",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(dim.iconDefault),
                    )
                },
                onClick = {
                    isReverseButtonRotated = !isReverseButtonRotated
                    onReverseButtonClick()
                },
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                },
            )

            RoundIconButton(
                content = {
                    Image(
                        painter = painterResource(Res.drawable.ic_search),
                        contentDescription = "Search",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(dim.iconDefault),
                    )
                },
                onClick = onSearchButtonClick,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun SearchStopColumnPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
        )
    }
}

@Preview
@Composable
private fun SearchStopColumnMetroPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
        )
    }
}

// endregion
