package xyz.ksharma.krail.discover.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

private enum class FeedbackAnimState {
    Idle, Rotating, ScalingOut, ShowButton
}

@Composable
fun FeedbackButtonsRow(
    modifier: Modifier = Modifier,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
) {
    var selected by remember { mutableStateOf<FeedbackSelectedState?>(null) }
    var animState by remember { mutableStateOf(FeedbackAnimState.Idle) }
    val scope = rememberCoroutineScope()

    val leftRotation = remember { Animatable(0f) }
    val leftScale = remember { Animatable(1f) }
    val rightScale = remember { Animatable(1f) }
    val buttonScale = remember { Animatable(0.8f) }

    fun startAnimation(feedback: FeedbackSelectedState) {
        selected = feedback
        animState = FeedbackAnimState.Rotating
        scope.launch {
            if (feedback == FeedbackSelectedState.Positive) {
                launch { leftRotation.animateTo(360f, tween(400)) }
                launch { rightScale.animateTo(0f, tween(300)) }
                delay(400)
                animState = FeedbackAnimState.ScalingOut
                leftScale.animateTo(0f, tween(250))
            } else {
                // 👎: both scale out, no rotation
                launch { leftScale.animateTo(0f, tween(300)) }
                launch { rightScale.animateTo(0f, tween(300)) }
                //delay(300)
                animState = FeedbackAnimState.ScalingOut
            }
            animState = FeedbackAnimState.ShowButton
            buttonScale.snapTo(0.8f)
            buttonScale.animateTo(1f, tween(350))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        when (animState) {
            FeedbackAnimState.Idle -> {
                FeedbackCircleBox(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = leftRotation.value
                            scaleX = leftScale.value
                            scaleY = leftScale.value
                        }
                        .klickable {
                            if (animState == FeedbackAnimState.Idle) {
                                onPositive()
                                startAnimation(FeedbackSelectedState.Positive)
                            }
                        }
                ) { Text("👍") }

                FeedbackCircleBox(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = 0f
                            scaleX = rightScale.value
                            scaleY = rightScale.value
                        }
                        .klickable {
                            if (animState == FeedbackAnimState.Idle) {
                                onNegative()
                                startAnimation(FeedbackSelectedState.Negative)
                            }
                        }
                ) { Text("👎") }
            }

            FeedbackAnimState.Rotating, FeedbackAnimState.ScalingOut -> {
                FeedbackCircleBox(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = leftRotation.value
                            scaleX = leftScale.value
                            scaleY = leftScale.value
                        }
                ) { Text("👍") }

                FeedbackCircleBox(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = 0f
                            scaleX = rightScale.value
                            scaleY = rightScale.value
                        }
                ) { Text("👎") }
            }

            FeedbackAnimState.ShowButton -> {
                val text = when (selected) {
                    FeedbackSelectedState.Positive -> "Write a review"
                    FeedbackSelectedState.Negative -> "Send feedback"
                    else -> ""
                }
                Button(
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    onClick = {},
                    modifier = Modifier.scale(buttonScale.value)
                ) {
                    Text(text)
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
        modifier = modifier.size(40.dp).clip(shape = CircleShape),
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
            onPositive = {},
            onNegative = {},
            modifier = Modifier.systemBarsPadding(),
        )
    }
}