package xyz.ksharma.krail.discover.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FeedbackButtonsRow(
    modifier: Modifier = Modifier,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
) {
    var state by remember { mutableStateOf(FeedbackState.Idle) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        AnimatedContent(targetState = state, label = "feedback") { feedbackState ->
            when (feedbackState) {
                FeedbackState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        FeedbackCircleBox(
                            modifier = Modifier.klickable {
                                state = FeedbackState.Positive
                                onPositive()
                            }
                        ) { Text("👍") }

                        FeedbackCircleBox(
                            modifier = Modifier.klickable {
                                state = FeedbackState.Negative
                                onNegative()
                            }
                        ) { Text("👎") }
                    }
                }

                FeedbackState.Positive -> AnimatedFeedbackButton("Write a review")

                FeedbackState.Negative -> AnimatedFeedbackButton("Share feedback")
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

@Composable
private fun AnimatedFeedbackButton(text: String) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (startAnim) 1f else 0.8f, animationSpec = tween(400))
    val alpha by animateFloatAsState(if (startAnim) 1f else 0f, animationSpec = tween(400))

    LaunchedEffect(Unit) { startAnim = true }

    Button(
        dimensions = ButtonDefaults.mediumButtonSize(),
        onClick = {},
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
    ) {
        Text(text)
    }
}

enum class FeedbackState { Idle, Positive, Negative }

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
