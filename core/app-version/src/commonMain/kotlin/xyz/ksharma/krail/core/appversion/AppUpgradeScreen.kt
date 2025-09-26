package xyz.ksharma.krail.core.appversion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.CookieShapeBox
import xyz.ksharma.krail.taj.components.CookieShapeBoxDefaults.cookieShapeShadow
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.magicBorderColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor

@Composable
fun AppUpgradeScreen(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Scene animations
    val backgroundGearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val mainGearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f, // Counter-clockwise rotation
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), // Slightly faster
            repeatMode = RepeatMode.Restart,
        ),
    )

    // region Cookie Shape Box rotation animations
    // super slow subtle cookie rotation (1 minute per full turn)
    val cookieRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    // gentle breathing scale (slight, reversible)
    val cookieScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    // endregion

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "KRAIL",
                style = KrailTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                ),
                color = themeColor(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )

            // Construction Scene: Worker fixing gears with tools
            // Adding a Box wrapper here and not using content param in CookieShapeBox because otherwise
            // contents will also rotate.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Cookie shape with subtle animated rotation + scale
                CookieShapeBox(
                    backgroundColor = KrailTheme.colors.surface,
                    cookieShadow = cookieShapeShadow(),
                    outlineBrush = Brush.linearGradient(
                        colors = magicBorderColors().map {
                            it.copy(alpha = 0.8f) // Slightly faded for subtlety
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .graphicsLayer {
                            rotationZ = cookieRotation
                            scaleX = cookieScale
                            scaleY = cookieScale
                        },
                )

                // Main gear being worked on (center of circle) - counter-clockwise
                Text(
                    text = "⚙️",
                    style = KrailTheme.typography.displayLarge.copy(
                        fontSize = 45.sp,
                    ),
                    modifier = Modifier
                        .offset(x = 0.dp, y = 0.dp) // Exactly centered
                        .rotate(mainGearRotation),
                    color = KrailTheme.colors.onSurface,
                )

                // Background rotating gear (top right) - clockwise for meshing motion
                Text(
                    text = "⚙️",
                    style = KrailTheme.typography.displayLarge.copy(
                        fontSize = 35.sp,
                    ),
                    modifier = Modifier
                        .offset(x = 34.dp, y = (-32).dp)
                        .rotate(backgroundGearRotation),
                    color = KrailTheme.colors.onSurface,
                )

                // Worker person (left center edge of circle)
                Text(
                    text = "👷‍♂️",
                    style = KrailTheme.typography.displayLarge.copy(
                        fontSize = 45.sp,
                    ),
                    modifier = Modifier
                        .offset(x = (-55).dp, y = 0.dp),
                )

                // Tool box positioned at bottom right of person with minimal padding
                Text(
                    text = "🧰",
                    style = KrailTheme.typography.displayLarge.copy(
                        fontSize = 28.sp,
                    ),
                    modifier = Modifier
                        .offset(x = (-30).dp, y = 32.dp),
                    color = KrailTheme.colors.onSurface,
                )
            }

            Text(
                text = "\uD83D\uDEA7 Time to Update \uD83D\uDEA7",
                style = KrailTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We’ve made improvements, please update to keep going\u00A0places!",
                style = KrailTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        AnimatedUpdateButton(
            onClick = {},
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .padding(vertical = 20.dp),
        )
    }
}

@Preview
@Composable
fun AppUpgradeScreenPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AppUpgradeScreen()
    }
}

@Composable
private fun AnimatedUpdateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startDelayMillis: Int = 300,
    fadeDurationMillis: Int = 520,
    scaleSpringDamping: Float = 0.68f,
) {
    var hasAnimated by rememberSaveable { mutableStateOf(false) }

    // Start closer to final size (0.7) to feel less "popping"
    val initialScale = if (hasAnimated) 1f else 0.7f
    val scale = remember { Animatable(initialScale) }
    val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }

    LaunchedEffect(hasAnimated) {
        if (!hasAnimated) {
            delay(startDelayMillis.toLong())

            // Run alpha + scale concurrently
            val scaleJob = launch {
                // Gentle ease to slight overshoot
                scale.animateTo(
                    targetValue = 1.06f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                )
                // Natural settle using spring
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = scaleSpringDamping,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            val alphaJob = launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = fadeDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            scaleJob.join()
            alphaJob.join()
            hasAnimated = true
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
        enabled = alpha.value > 0.5f,
    ) {
        Text("Update Now")
    }
}
