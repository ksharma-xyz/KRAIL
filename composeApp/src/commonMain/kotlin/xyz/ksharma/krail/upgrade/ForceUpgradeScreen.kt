package xyz.ksharma.krail.upgrade

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.CookieShapeBox
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor

@Composable
fun ForceUpgradeScreen(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Scene animations
    val backgroundGearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val mainGearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f, // Counter-clockwise rotation
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), // Slightly faster
            repeatMode = RepeatMode.Restart
        )
    )

    // region Cookie Shape Box rotation animations
    // super slow subtle cookie rotation (1 minute per full turn)
    val cookieRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // gentle breathing scale (slight, reversible)
    val cookieScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
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
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Cookie shape with subtle animated rotation + scale
                CookieShapeBox(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = cookieRotation
                            scaleX = cookieScale
                            scaleY = cookieScale
                        }
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
                        .offset(x = (-55).dp, y = 0.dp)
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
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "\uD83D\uDEA7 Time to Update \uD83D\uDEA7",
                style = KrailTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We’ve made improvements, please update to keep going\u00A0places!",
                style = KrailTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {},
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .padding(vertical = 20.dp),
        ) {
            Text("Update Now")
        }
    }
}

@Preview
@Composable
fun ForceUpgradeScreenPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ForceUpgradeScreen()
    }
}
