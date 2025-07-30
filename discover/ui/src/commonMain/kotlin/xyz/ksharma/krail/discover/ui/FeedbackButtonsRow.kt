package xyz.ksharma.krail.discover.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

@Composable
fun FeedbackButtonsRow(
    modifier: Modifier = Modifier,
    onPositiveThumb: () -> Unit,
    onNegativeThumb: () -> Unit,
    onPositiveCta: () -> Unit,
    onNegativeCta: () -> Unit,
) {
    var selected by remember { mutableStateOf<FeedbackSelectedState?>(null) }
    val scope = rememberCoroutineScope()

    val thumbsAlpha = remember { Animatable(1f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(0.95f) }

    fun startAnimation(feedback: FeedbackSelectedState) {
        scope.launch {
            thumbsAlpha.animateTo(0f, tween(250))
            selected = feedback
            // Animate alpha and scale together
            launch { buttonAlpha.animateTo(1f, tween(350)) }
            launch { buttonScale.animateTo(1f, tween(350)) }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        if (selected == null) {
            FeedbackCircleBox(
                modifier = Modifier
                    .graphicsLayer { alpha = thumbsAlpha.value }
                    .klickable {
                        if (selected == null) {
                            onPositiveThumb()
                            startAnimation(FeedbackSelectedState.Positive)
                        }
                    }
            ) { Text("👍") }

            FeedbackCircleBox(
                modifier = Modifier
                    .graphicsLayer { alpha = thumbsAlpha.value }
                    .klickable {
                        if (selected == null) {
                            onNegativeThumb()
                            startAnimation(FeedbackSelectedState.Negative)
                        }
                    }
            ) { Text("👎") }

        } else {
            val text = when (selected) {
                FeedbackSelectedState.Positive -> "Write a review"
                FeedbackSelectedState.Negative -> "Send feedback"
                else -> ""
            }
            Button(
                dimensions = ButtonDefaults.mediumButtonSize(),
                onClick = {
                    when (selected) {
                        FeedbackSelectedState.Positive -> onPositiveCta()
                        FeedbackSelectedState.Negative -> onNegativeCta()
                        null -> Unit
                    }
                },
                modifier = Modifier
                    .scale(buttonScale.value)
                    .graphicsLayer { alpha = buttonAlpha.value }
            ) {
                Text(text)
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
