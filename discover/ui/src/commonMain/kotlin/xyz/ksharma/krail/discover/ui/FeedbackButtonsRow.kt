package xyz.ksharma.krail.discover.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverState.DiscoverUiModel.FeedbackState
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

@Composable
fun FeedbackButtonsRow(
    modifier: Modifier = Modifier,
    feedbackState: FeedbackState? = null,
    onPositiveThumb: () -> Unit,
    onNegativeThumb: () -> Unit,
    onPositiveCta: () -> Unit,
    onNegativeCta: () -> Unit,
) {
    // Single source of truth for selected state
    var localSelected by remember { mutableStateOf<FeedbackSelectedState?>(null) }

    // Priority: feedbackState from DB > localSelected (for animations)
    // Priority: feedbackState from DB > localSelected (for animations)
    val currentState = remember(feedbackState, localSelected) {
        feedbackState?.let {
            if (it.isPositive) FeedbackSelectedState.Positive
            else FeedbackSelectedState.Negative
        } ?: localSelected
    }

    val scope = rememberCoroutineScope()

    val thumbsAlpha = remember { Animatable(1f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(0.5f) }

    fun animateToCtaState(selectedState: FeedbackSelectedState) {
        // Only animate if no external state (fresh selection)
        scope.launch {
            thumbsAlpha.animateTo(0f, tween(250))
            localSelected = selectedState
            launch { buttonAlpha.animateTo(1f, tween(350)) }
            launch { buttonScale.animateTo(1f, tween(350)) }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        when (currentState) {
            null -> {
                // Show thumbs (no feedback given yet)
                FeedbackCircleBox(
                    modifier = Modifier.graphicsLayer { alpha = thumbsAlpha.value }
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                onPositiveThumb()
                                animateToCtaState(FeedbackSelectedState.Positive)
                            }),
                ) {
                    Text(text = "👍")
                }

                FeedbackCircleBox(
                    modifier = Modifier.graphicsLayer { alpha = thumbsAlpha.value }
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                onNegativeThumb()
                                animateToCtaState(FeedbackSelectedState.Negative)
                            }),
                ) {
                    Text(text = "👎")
                }
            }

            FeedbackSelectedState.Positive -> {
                // Show positive CTA buttons
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (feedbackState != null) 1f else buttonAlpha.value
                            scaleX = if (feedbackState != null) 1f else buttonScale.value
                            scaleY = if (feedbackState != null) 1f else buttonScale.value
                        },
                    onClick = onPositiveCta,
                ) {
                    Text(text = "Write Review")
                }
            }

            FeedbackSelectedState.Negative -> {
                // Show negative CTA buttons
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (feedbackState != null) 1f else buttonAlpha.value
                            scaleX = if (feedbackState != null) 1f else buttonScale.value
                            scaleY = if (feedbackState != null) 1f else buttonScale.value
                        },
                    onClick = onNegativeCta,
                ) {
                    Text(text = "Share Feedback")
                }
            }
        }
    }
}

@Composable
private fun FeedbackCircleBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape = CircleShape)
            .background(color = themeBackgroundColor()),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.title) {
            content()
        }
    }
}

enum class FeedbackSelectedState { Positive, Negative }

@Preview(showBackground = true)
@Composable
private fun FeedbackButtonsRowPreview() {
    PreviewContent {
        FeedbackButtonsRow(
            onPositiveThumb = {},
            onNegativeThumb = {},
            modifier = Modifier.systemBarsPadding(),
            onPositiveCta = {},
            onNegativeCta = {},
        )
    }
}
