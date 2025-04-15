package xyz.ksharma.krail.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun SplashScreen(
    logoColor: Color?,
    backgroundColor: Color?,
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor ?: KrailTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedKrailLogo(logoColor = logoColor ?: KrailTheme.colors.onSurface)

        val splashComplete by rememberUpdatedState(onSplashComplete)
        LaunchedEffect(key1 = Unit) {
            delay(1200)
            splashComplete()
        }
    }
}

@Composable
private fun AnimatedKrailLogo(
    logoColor: Color,
    modifier: Modifier = Modifier,
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        animationStarted = true
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            AnimatedLetter(
                letter = "K",
                animationStarted = animationStarted,
                fontSize = 80.sp,
                delayMillis = 0,
                logoColor = logoColor,
                modifier = Modifier.alignByBaseline(),
            )
            AnimatedLetter(
                letter = "R",
                animationStarted = animationStarted,
                logoColor = logoColor,
                delayMillis = 50,
                modifier = Modifier.alignByBaseline(),
            )
            AnimatedLetter(
                letter = "A",
                animationStarted = animationStarted,
                logoColor = logoColor,
                delayMillis = 100,
                modifier = Modifier.alignByBaseline(),
            )
            AnimatedLetter(
                letter = "I",
                animationStarted = animationStarted,
                logoColor = logoColor,
                delayMillis = 150,
                modifier = Modifier.alignByBaseline(),
            )
            AnimatedLetter(
                letter = "L",
                animationStarted = animationStarted,
                logoColor = logoColor,
                delayMillis = 200,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Text(
            text = "Ride the rail, without fail.",
            style = KrailTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
            color = logoColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp,),
        )
    }
}

@Composable
private fun AnimatedLetter(
    letter: String,
    animationStarted: Boolean,
    logoColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit(65F, TextUnitType.Sp),
    delayMillis: Int = 100,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "animeAnimation")

    // Scale animation with anticipation and squash/stretch
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0.0f at 0 using LinearEasing // Hold initial scale
                0.7f at 200 using FastOutSlowInEasing // Anticipation (quick shrink)
                1.2f at 500 using FastOutSlowInEasing // Squash/stretch (overshoot)
                1.0f at 1000 using FastOutSlowInEasing // Settle back to normal scale
                1.0f at 1100 using LinearEasing // Keep at normal scale
            },
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = delayMillis),
        ),
        label = "animeAnimation",
    )

    val letterScale by remember(scale) {
        mutableFloatStateOf(if (animationStarted) scale else 1f)
    }
    CompositionLocalProvider(
        LocalTextColor provides logoColor,
    ) {
        Text(
            text = letter,
            color = logoColor,
            style = KrailTheme.typography.displayLarge.copy(
                fontSize = fontSize,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Black,
            ),
            modifier = modifier
                .graphicsLayer {
                    scaleX = letterScale
                    scaleY = letterScale
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Preview
@Composable
private fun PreviewLogo() {
    KrailTheme {
        Column(modifier = Modifier.background(color = KrailTheme.colors.surface)) {
            AnimatedKrailLogo(logoColor = Color(0xFFF6891F))
        }
    }
}

@Preview
@Composable
private fun PreviewSplashScreen() {
    KrailTheme {
        SplashScreen(
            onSplashComplete = {},
            logoColor = KrailTheme.colors.onSurface,
            backgroundColor = Color(0xFF009B77),
        )
    }
}
