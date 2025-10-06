package xyz.ksharma.krail.trip.planner.ui.components.loading

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun LoadingEmojiAnim(
    emoji: String,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "333")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * 3,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000 // Total animation duration
                0f at 0 using FastOutLinearInEasing // Start at 0 rotation
                360f * 3 at 1000 using LinearEasing // Fast rotation for 0.5 seconds (4 rotations)
                360f * 3 at 2000 using FastOutSlowInEasing // Maintain rotation but slow down (cumulative 4 rotations)
            },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "33",
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000 // Total animation duration
                1f at 0 using FastOutLinearInEasing // Start at original size
                2f at 200 using LinearEasing // Quickly grow to max scale (0.2 seconds)
                1f at 2000 using FastOutSlowInEasing // Shrink back to original size slowly (1.8 seconds)
            },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "11",
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = emoji,
            style = KrailTheme.typography.headlineLarge.copy(fontSize = 64.sp),
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingEmojiAnim(emoji = "\uD83D\uDE80")
        }
    }
}
