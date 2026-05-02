package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import krail.feature.trip_planner.ui.generated.resources.ic_search
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private val SearchRowTopRadius = 36.dp
private val SearchRowVerticalPadding = 20.dp
private val SearchFieldSpacing = 20.dp

@Composable
fun SearchStopRow(
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    fromStopItem: StopItem? = null,
    toStopItem: StopItem? = null,
    isExpanded: Boolean = false,
    isFromHighlighted: Boolean = false,
    onExpandRequest: () -> Unit = {},
    onReverseButtonClick: () -> Unit = {},
    onSearchButtonClick: () -> Unit = {},
) {
    val themeColorHex by LocalThemeColor.current
    val navBarPadding = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // When launched from a label pill tap (isFromHighlighted=true), From field is initially
    // hidden to let the "To is pre-filled" message land first, then animates in.
    var showFromField by rememberSaveable(isExpanded, isFromHighlighted) {
        mutableStateOf(isExpanded && !isFromHighlighted)
    }

    LaunchedEffect(isExpanded, isFromHighlighted) {
        if (isExpanded && isFromHighlighted && !showFromField) {
            delay(350)
            showFromField = true
        } else if (!isExpanded) {
            showFromField = false
        } else if (isExpanded && !isFromHighlighted) {
            showFromField = true
        }
    }

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            if (targetState) {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(150))
            } else {
                fadeIn(animationSpec = tween(100)) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(250))
            }
        },
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
        label = "SearchStopRowContent",
    ) { expanded ->
        if (expanded) {
            ExpandedSearchRow(
                fromStopItem = fromStopItem,
                toStopItem = toStopItem,
                themeColorHex = themeColorHex,
                navBarPadding = navBarPadding.value,
                showFromField = showFromField,
                isFromHighlighted = isFromHighlighted,
                fromButtonClick = fromButtonClick,
                toButtonClick = toButtonClick,
                onReverseButtonClick = onReverseButtonClick,
                onSearchButtonClick = onSearchButtonClick,
            )
        } else {
            CollapsedPill(
                navBarPadding = navBarPadding.value,
                onClick = onExpandRequest,
            )
        }
    }
}

@Composable
private fun CollapsedPill(
    navBarPadding: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = with(LocalDensity.current) { navBarPadding.dp } + dim.spacingXL,
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            dimensions = ButtonDefaults.mediumButtonSize(),
            onClick = onClick,
        ) {
            Text(
                text = "Plan a trip",
            )
        }
    }
}

@Composable
private fun ExpandedSearchRow(
    fromStopItem: StopItem?,
    toStopItem: StopItem?,
    themeColorHex: String,
    navBarPadding: Float,
    showFromField: Boolean,
    isFromHighlighted: Boolean,
    fromButtonClick: () -> Unit,
    toButtonClick: () -> Unit,
    onReverseButtonClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var isReverseButtonRotated by rememberSaveable { mutableStateOf(false) }

    // Pulsing border alpha for the highlighted From field
    val infiniteTransition = rememberInfiniteTransition(label = "fromHighlight")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fromBorderAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = themeColorHex.hexToComposeColor(),
                shape = RoundedCornerShape(
                    topStart = SearchRowTopRadius,
                    topEnd = SearchRowTopRadius,
                ),
            )
            .padding(vertical = SearchRowVerticalPadding, horizontal = dim.pageHorizontalPadding)
            .padding(
                bottom = with(LocalDensity.current) { navBarPadding.dp },
                top = dim.spacingM,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
        ) {
            // From field — animates in when opened via a label pill tap
            AnimatedVisibility(
                visible = showFromField,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(350)),
            ) {
                val fromFieldShape = RoundedCornerShape(50)
                TextFieldButton(
                    onClick = fromButtonClick,
                    modifier = if (isFromHighlighted) {
                        Modifier.border(
                            width = dim.strokeRegular,
                            color = KrailTheme.colors.surface.copy(alpha = borderAlpha),
                            shape = fromFieldShape,
                        )
                    } else {
                        Modifier
                    },
                ) {
                    AnimatedContent(
                        targetState = fromStopItem?.stopName ?: "Starting from",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) +
                                slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                                ) togetherWith fadeOut(animationSpec = tween(200)) +
                                slideOutVertically(
                                    targetOffsetY = { -it / 2 },
                                    animationSpec = tween(500),
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
            }

            // A small spacer replaces the From field gap when From is hidden,
            // so the To field doesn't jump when From animates in.
            if (!showFromField) {
                Spacer(modifier = Modifier.height(0.dp))
            }

            // To field — always visible when expanded
            TextFieldButton(onClick = toButtonClick) {
                AnimatedContent(
                    targetState = toStopItem?.stopName ?: "Destination",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) +
                            slideInVertically(
                                initialOffsetY = { -it / 2 },
                                animationSpec = tween(500, easing = FastOutSlowInEasing),
                            ) togetherWith fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = tween(500),
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

        // Action buttons (reverse + search)
        Column(
            modifier = Modifier.padding(start = dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(SearchFieldSpacing),
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isReverseButtonRotated) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "reverseRotation",
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
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
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

@Preview(name = "1. Collapsed pill — Train theme")
@Composable
private fun PreviewSearchStopRow_Collapsed_Train() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = false,
        )
    }
}

@Preview(name = "2. Expanded — both fields empty")
@Composable
private fun PreviewSearchStopRow_Expanded_Empty() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = true,
            isFromHighlighted = false,
        )
    }
}

@Preview(name = "3. Expanded — To pre-filled, From highlighted (label pill flow)")
@Composable
private fun PreviewSearchStopRow_Expanded_LabelPill() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = true,
            isFromHighlighted = true,
            toStopItem = StopItem(stopId = "2000001", stopName = "Central Station"),
        )
    }
}

@Preview(name = "4. Expanded — both stops set")
@Composable
private fun PreviewSearchStopRow_Expanded_BothSet() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = true,
            fromStopItem = StopItem(stopId = "2000002", stopName = "Town Hall Station"),
            toStopItem = StopItem(stopId = "2000001", stopName = "Central Station"),
        )
    }
}

@Preview(name = "5. Collapsed pill — Ferry theme")
@Composable
private fun PreviewSearchStopRow_Collapsed_Ferry() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = false,
        )
    }
}

// endregion
