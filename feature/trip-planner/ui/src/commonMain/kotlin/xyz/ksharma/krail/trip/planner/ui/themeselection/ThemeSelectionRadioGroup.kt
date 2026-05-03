package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.krail.taj.animations.ThemeTransitionTiming
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.LocalThemeController
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import kotlin.math.roundToInt

@Composable
fun ThemeSelectionRadioGroup(
    selectedTheme: ThemeMode,
    onThemeSelect: (ThemeMode) -> Unit,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    var targetIndex by rememberSaveable {
        mutableStateOf(ThemeMode.entries.indexOfFirst { it == selectedTheme })
    }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Update targetIndex when selectedTheme changes from outside
    LaunchedEffect(selectedTheme) {
        targetIndex = ThemeMode.entries.indexOfFirst { it == selectedTheme }
    }

    // Store measured text widths
    val textWidths = remember { mutableStateMapOf<Int, Float>() }
    val density = LocalDensity.current

    val dim = KrailTheme.dimensions
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(GROUP_HEIGHT)
            .background(
                color = KrailTheme.colors.themeSelectionBackground,
                shape = RoundedCornerShape(GROUP_CORNER_RADIUS),
            )
            .padding(dim.spacingXS),
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val optionWidth = containerWidth / ThemeMode.entries.size

        val fallbackWidth = optionWidth * FALLBACK_WIDTH_RATIO

        // Current handle width based on selected text + padding
        val currentHandleWidth = textWidths[targetIndex] ?: fallbackWidth
        val paddedHandleWidth = currentHandleWidth + with(density) { HANDLE_LABEL_PADDING.toPx() }

        // Animate handle width
        val animatedWidth by animateFloatAsState(
            targetValue = paddedHandleWidth,
            animationSpec = spring(),
            label = "handleWidth",
        )

        // Animate handle offset (centered under label)
        val animatedOffset by animateFloatAsState(
            targetValue = if (isDragging) {
                dragOffset
            } else {
                targetIndex * optionWidth + (optionWidth - animatedWidth) / 2f
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "selectorOffset",
        )

        // Tap detection
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val tappedIndex = (offset.x / optionWidth).toInt()
                            .coerceIn(0, ThemeMode.entries.lastIndex)
                        targetIndex = tappedIndex
                        onThemeSelect(ThemeMode.entries[tappedIndex])
                    }
                },
        )

        // Handle
        Box(
            modifier = Modifier
                .height(dim.buttonRoundSize)
                .width(with(density) { animatedWidth.toDp() })
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffset =
                                targetIndex * optionWidth + (optionWidth - animatedWidth) / 2f
                        },
                        onDragEnd = {
                            isDragging = false
                            val snappedIndex = (dragOffset / optionWidth).roundToInt()
                                .coerceIn(0, ThemeMode.entries.lastIndex)
                            targetIndex = snappedIndex
                            onThemeSelect(ThemeMode.entries[snappedIndex])
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val horizontalPaddingPx = with(density) { dim.spacingXS.toPx() }
                        val minOffset = horizontalPaddingPx
                        val maxOffset = containerWidth - horizontalPaddingPx - animatedWidth

                        dragOffset = (dragOffset + dragAmount.x)
                            .coerceIn(minOffset, maxOffset)
                    }
                },
        ) {
            ThemeSelectorHandle(
                isDragging = isDragging,
                glowColor = glowColor,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Labels with measurement
        Row(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = dim.spacingXS), // push edge labels inward
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeMode.entries.forEachIndexed { index, option ->
                ThemeOptionLabel(
                    text = option.displayName,
                    isSelected = targetIndex == index,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dim.spacingM) // spacing between text and handle edges
                        .onGloballyPositioned { coords ->
                            val width = coords.size.width.toFloat()
                            textWidths[index] = width
                        },
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun ThemeSelectorHandle(
    isDragging: Boolean,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    // Use glowColor for pressed shadow colors with different alpha values
    val pressedShadowColors = listOf(
        glowColor.copy(alpha = 0.4f),
        glowColor.copy(alpha = 0.3f),
        glowColor.copy(alpha = 0.2f),
    )

    val shadowRadius by animateDpAsState(
        targetValue = if (isDragging) SHADOW_RADIUS_DRAGGING else SHADOW_RADIUS_DEFAULT,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shadowRadius",
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )

    val shadowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.8f, // Always visible, more opaque when dragging
        animationSpec = tween(ThemeTransitionTiming.TEXT_DURATION_MS), // Use centralized timing constant
        label = "shadowAlpha",
    )

    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .scale(scale)
            .dropShadow(
                shape = RoundedCornerShape(dim.radiusXXL),
                shadow = Shadow(
                    color = pressedShadowColors[0],
                    radius = shadowRadius,
                    spread = 0.dp,
                    alpha = shadowAlpha,
                ),
            )
            .dropShadow(
                shape = RoundedCornerShape(dim.radiusXXL),
                shadow = Shadow(
                    color = pressedShadowColors[1],
                    radius = (shadowRadius.value * 0.7f).dp,
                    spread = 1.dp,
                    alpha = shadowAlpha,
                ),
            )
            .dropShadow(
                shape = RoundedCornerShape(dim.radiusXXL),
                shadow = Shadow(
                    color = pressedShadowColors[2],
                    radius = (shadowRadius.value * 0.4f).dp,
                    spread = 2.dp,
                    alpha = shadowAlpha,
                ),
            )
            // Handle background uses surface color
            .background(
                color = KrailTheme.colors.surface,
                shape = RoundedCornerShape(dim.radiusXXL),
            ),
    )
}

@Composable
private fun ThemeOptionLabel(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> KrailTheme.colors.onSurface // Use onSurface for contrast against surface-colored handle
            else -> KrailTheme.colors.softLabel
        },
        animationSpec = tween(ThemeTransitionTiming.TEXT_DURATION_MS),
        label = "textColor",
    )

    // Animate text size
    val animatedFontSize by animateFloatAsState(
        targetValue = if (isSelected) {
            KrailTheme.typography.titleLarge.fontSize.value
        } else {
            KrailTheme.typography.bodyMedium.fontSize.value
        },
        animationSpec = tween(ThemeTransitionTiming.TEXT_DURATION_MS),
        label = "fontSize",
    )

    // Animate letter spacing
    val animatedLetterSpacing by animateFloatAsState(
        targetValue = if (isSelected) {
            KrailTheme.typography.titleLarge.letterSpacing.value
        } else {
            KrailTheme.typography.bodyMedium.letterSpacing.value
        },
        animationSpec = tween(ThemeTransitionTiming.TEXT_DURATION_MS),
        label = "letterSpacing",
    )

    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier.padding(horizontal = dim.spacingM, vertical = dim.spacingM),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = KrailTheme.typography.bodyMedium.copy(
                fontSize = animatedFontSize.sp,
                letterSpacing = animatedLetterSpacing.sp,
                fontWeight = if (isSelected) {
                    KrailTheme.typography.titleLarge.fontWeight
                } else {
                    KrailTheme.typography.bodyMedium.fontWeight
                },
            ),
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

private const val FALLBACK_WIDTH_RATIO = 0.6f
private val GROUP_HEIGHT = 56.dp
private val GROUP_CORNER_RADIUS = 32.dp
private val HANDLE_LABEL_PADDING = 24.dp
private val SHADOW_RADIUS_DEFAULT = 14.dp
private val SHADOW_RADIUS_DRAGGING = 20.dp

// region Previews

@Preview
@Composable
private fun ThemeSelectionRadioGroupPreview() {
    // Create a dummy theme controller for preview
    var currentMode by remember { mutableStateOf(ThemeMode.LIGHT) }
    val dummyThemeController = remember {
        ThemeController(
            currentMode = currentMode,
            setThemeMode = { },
        )
    }

    KrailTheme(dummyThemeController) {
        CompositionLocalProvider(
            LocalThemeController provides dummyThemeController,
        ) {
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .background(KrailTheme.colors.surface)
                    .padding(KrailTheme.dimensions.spacingXXXL),
                contentAlignment = Alignment.Center,
            ) {
                ThemeSelectionRadioGroup(
                    selectedTheme = currentMode,
                    onThemeSelect = { newMode ->
                        currentMode = newMode
                        dummyThemeController.setThemeMode(newMode)
                    },
                    glowColor = KrailTheme.colors.onSurface,
                )
            }
        }
    }
}

// endregion
